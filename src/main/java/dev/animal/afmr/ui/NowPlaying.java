package dev.animal.afmr.ui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.core.Library.NowPlayingStyle;
import dev.animal.afmr.core.NowPlayingInfo;
import dev.animal.afmr.core.NowPlayingLayout;
import dev.animal.afmr.library.MusicFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import java.util.concurrent.CompletableFuture;

public final class NowPlaying {
    private static final Identifier COVER=NowPlayingWidget.COVER;
    private final NowPlayingWidget widget=new NowPlayingWidget();
    private final ClientRuntime runtime;
    private final Minecraft mc=Minecraft.getInstance();
    private Track track;
    private long playbackSequence;
    private NowPlayingStyle style;
    private boolean enabled,nativeShown;
    private String nativeSongName;
    private Component nativeSongText=Component.empty();
    private double shownAt=Double.NaN;
    private long generation;
    private boolean hasCover;
    private boolean coverLoading;
    private Track pendingCover;
    public NowPlaying(ClientRuntime runtime){this.runtime=runtime;}
    public void tick(){
        Track current=runtime.engine().current();
        var settings=runtime.library().settings;
        long sequence=runtime.engine().playbackSequence();
        if(current!=track||sequence!=playbackSequence||style!=settings.nowPlayingStyle||enabled!=settings.nowPlaying){
            clear();track=current;playbackSequence=sequence;style=settings.nowPlayingStyle;enabled=settings.nowPlaying;
            if(track==null||!enabled)return;
            shownAt=Double.NaN;
            Component title=title(track);
            if(style==NowPlayingStyle.MINECRAFT){
                nativeSongName=track.vanilla?Identifier.parse(track.path).toShortLanguageKey()
                    :track.artist.isBlank()?track.title:track.artist+" — "+track.title;
                // Local metadata is literal: slashes and translation-looking titles must survive unchanged.
                nativeSongText=track.vanilla?title:Component.literal(nativeSongName);
            }else{
                var playlist=runtime.library().playlist(runtime.engine().sourcePlaylistId());
                widget.content(new NowPlayingInfo(track,title.getString(),runtime.engine().previewing()?"Preview":playlist==null?"":playlist.name,settings));
                if(settings.widgetArtwork&&!track.vanilla){pendingCover=current;loadCover();}
            }
        }
        if(track!=null&&enabled&&Double.isNaN(shownAt)&&!runtime.engine().paused()
            &&runtime.engine().trackElapsed(runtime.now())>=settings.popupDelaySeconds)shownAt=runtime.now();
        if(nativeSongName!=null&&!nativeShown&&!Double.isNaN(shownAt)&&!runtime.engine().paused()
            &&mc.options.getFinalSoundSourceVolume(SoundSource.MUSIC)>0){
            mc.gui.toastManager().showNowPlayingToast();nativeShown=true;
        }
    }
    static Component title(Track track){
        return track.vanilla?Component.translatable(Identifier.parse(track.path).toShortLanguageKey().replace('/','.')):Component.literal(track.title);
    }
    /** Settings changes refresh both the cached rows and the current popup. */
    public void refresh(){clear();tick();}
    boolean hasCover(){return hasCover;}
    /** One artwork read at a time; rapid skips/settings changes retain only the latest pending request. */
    private void loadCover(){
        if(coverLoading||pendingCover==null)return;
        Track current=pendingCover;pendingCover=null;coverLoading=true;
        long token=generation;
        CompletableFuture.supplyAsync(()->MusicFiles.cover(current)).whenComplete((data,failure)->mc.execute(()->{
            coverLoading=false;
            if(token==generation&&data!=null){
                try{
                    var image=NativeImage.read(data);
                    mc.getTextureManager().register(COVER,new DynamicTexture(()->"AFMR album cover",image));hasCover=true;
                }catch(Exception e){dev.animal.afmr.Afmr.LOG.debug("Album artwork unavailable",e);}
            }
            loadCover();
        }));
    }
    public void clear(){
        generation++;track=null;pendingCover=null;
        if(nativeSongName!=null)mc.gui.toastManager().hideNowPlayingToast();
        nativeSongName=null;nativeSongText=Component.empty();nativeShown=false;
        if(hasCover){mc.getTextureManager().release(COVER);hasCover=false;}
    }
    public String nativeSongName(){
        var settings=runtime.library().settings;
        return settings.nowPlaying&&settings.nowPlayingStyle==NowPlayingStyle.MINECRAFT&&runtime.engine().current()==track&&!Double.isNaN(shownAt)?nativeSongName:null;
    }
    public Component nativeSongText(){return nativeSongName()==null?Component.empty():nativeSongText;}
    public void draw(GuiGraphicsExtractor g){
        var settings=runtime.library().settings;
        if(track==null||!settings.nowPlaying||settings.nowPlayingStyle==NowPlayingStyle.MINECRAFT
            ||runtime.engine().current()!=track||mc.gui.hud.isHidden()||mc.level==null)return;
        double age=runtime.now()-shownAt,duration=settings.popupSeconds;
        if(Double.isNaN(age)||age<0)return;
        if(age>=duration)return;
        float alpha=(float)Math.min(1,Math.min(age/0.6,(duration-age)/0.8));if(alpha<=0.02)return;
        boolean hybrid=settings.nowPlayingStyle==NowPlayingStyle.HYBRID;
        boolean bottom=settings.widgetCorner.resolved(settings.nowPlayingStyle).bottom();
        int availableHeight=Math.max(1,g.guiHeight()-(bottom?74:16));
        var layout=widget.layout(settings,g.guiWidth()-16,availableHeight);
        double widgetWidth=layout.width()*layout.scale(),widgetHeight=layout.height()*layout.scale();
        float x=NowPlayingLayout.anchorX(settings.nowPlayingStyle,settings.widgetCorner,widgetWidth,g.guiWidth());
        float y=NowPlayingLayout.anchorY(settings.nowPlayingStyle,settings.widgetCorner,widgetWidth,widgetHeight,g.guiWidth(),g.guiHeight());
        if(hybrid)x+=(float)(widgetWidth*(1-alpha)*(settings.widgetCorner.resolved(settings.nowPlayingStyle).right()?1:-1));
        widget.draw(g,settings,layout,x,y,alpha,hasCover,age);
    }
}

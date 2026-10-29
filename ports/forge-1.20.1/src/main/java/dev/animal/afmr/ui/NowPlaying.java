package dev.animal.afmr.ui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Track;

import dev.animal.afmr.core.NowPlayingInfo;
import dev.animal.afmr.core.NowPlayingLayout;
import dev.animal.afmr.library.MusicFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import java.util.concurrent.CompletableFuture;
import java.io.ByteArrayInputStream;

public final class NowPlaying {
    private static final ResourceLocation COVER=NowPlayingWidget.COVER;
    private final NowPlayingWidget widget=new NowPlayingWidget();
    private final ClientRuntime runtime;
    private final Minecraft mc=Minecraft.getInstance();
    private Track track;
    private long playbackSequence;
    private boolean enabled;
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
        if(current!=track||sequence!=playbackSequence||enabled!=settings.nowPlaying){
            clear();track=current;playbackSequence=sequence;enabled=settings.nowPlaying;
            if(track==null||!enabled)return;
            shownAt=Double.NaN;

            var playlist=runtime.library().playlist(runtime.engine().sourcePlaylistId());
            widget.content(new NowPlayingInfo(track,title(track).getString(),runtime.engine().previewing()?"Preview":playlist==null?"":playlist.name,settings));
            if(settings.widgetArtwork&&!track.vanilla){pendingCover=current;loadCover();}
        }
        if(track!=null&&enabled&&Double.isNaN(shownAt)&&!runtime.engine().paused()
            &&runtime.engine().trackPlayed(runtime.now())>=settings.popupDelaySeconds)shownAt=runtime.now();
    }
    static Component title(Track track){
        return Component.literal(track.title);
    }
    /** Settings changes refresh both the cached rows and the current popup. */
    public void refresh(){clear();tick();}
    /** Reopen this song's existing popup without restarting audio or reloading its artwork. */
    public void showCurrent(){
        tick();
        if(track==null||!enabled)return;
        shownAt=runtime.now();
    }
    boolean hasCover(){return hasCover;}
    /** One artwork read at a time; rapid skips/settings changes retain only the latest pending request. */
    private void loadCover(){
        if(coverLoading||pendingCover==null)return;
        Track current=pendingCover;pendingCover=null;coverLoading=true;
        long token=generation;
        CompletableFuture.supplyAsync(()->MusicFiles.cover(current)).whenComplete((data,failure)->mc.execute(()->{
            coverLoading=false;
            if(token==generation&&data!=null){
                NativeImage image=null;DynamicTexture texture=null;boolean registered=false;
                try{
                    // 1.20.1's byte[] overload copies the PNG onto LWJGL's small scratch stack.
                    // The stream overload owns/frees a native heap buffer instead.
                    image=NativeImage.read(new ByteArrayInputStream(data));
                    texture=new DynamicTexture(image);
                    mc.getTextureManager().register(COVER,texture);registered=true;hasCover=true;
                }catch(Exception e){dev.animal.afmr.Afmr.LOG.debug("Album artwork unavailable",e);}
                finally{if(!registered){if(texture!=null)texture.close();else if(image!=null)image.close();}}
            }
            loadCover();
        }));
    }
    public void clear(){
        generation++;track=null;pendingCover=null;
        if(hasCover){mc.getTextureManager().release(COVER);hasCover=false;}
    }
    public void draw(GuiGraphics g){
        var settings=runtime.library().settings;
        if(track==null||!settings.nowPlaying
            ||runtime.engine().current()!=track||mc.options.hideGui||mc.level==null)return;
        double age=runtime.now()-shownAt,duration=settings.popupSeconds;
        if(Double.isNaN(age)||age<0)return;
        if(age>=duration)return;
        float alpha=(float)Math.min(1,Math.min(age/0.6,(duration-age)/0.8));if(alpha<=0.02)return;
        boolean bottom=settings.widgetCorner.resolved().bottom();
        int availableHeight=Math.max(1,g.guiHeight()-(bottom?74:16));
        var layout=widget.layout(settings,g.guiWidth()-16,availableHeight);
        double widgetWidth=layout.width()*layout.scale(),widgetHeight=layout.height()*layout.scale();
        float x=NowPlayingLayout.anchorX(settings.widgetCorner,widgetWidth,g.guiWidth());
        float y=NowPlayingLayout.anchorY(settings.widgetCorner,widgetWidth,widgetHeight,g.guiWidth(),g.guiHeight());
        widget.draw(g,settings,layout,x,y,alpha,hasCover,age);
    }
}

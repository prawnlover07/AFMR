package dev.animal.afmr.ui;

import net.minecraft.network.chat.Component;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.core.NowPlayingInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

final class NowPlayingPreviewScreen extends MusicScreen {
    private String guidance;
    private final NowPlayingWidget widget=new NowPlayingWidget();
    private final Track track;
    private final long started=System.nanoTime();

    NowPlayingPreviewScreen(Screen parent,ClientRuntime runtime){
        super("Widget preview",parent,runtime);
        var current=runtime.engine().current();
        if(current!=null)track=current;
        else{
            track=new Track("preview","","Sweden",false);
            track.artist="C418";track.album="Minecraft - Volume Alpha";track.durationSeconds=215;
        }
        var playlist=runtime.library().playlist(runtime.engine().sourcePlaylistId());
        widget.content(new NowPlayingInfo(track,NowPlaying.title(track).getString(),current==null?"Day outside"
            :runtime.engine().previewing()?"Preview":playlist==null?"":playlist.name,runtime.library().settings));
    }

    @Override protected void init(){
        super.init();guidance=fit(Component.translatable("afmr.copy.nowplayingpreview.guidance").getString(),contentWidth);button("Back",left+contentWidth-100,height-29,100,this::onClose);
    }

    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        var s=runtime.library().settings;
        var layout=widget.layout(s,width-16,height-110);
        widget.draw(g,s,layout,8,62,1,runtime.engine().current()==track&&runtime.overlay().hasCover(),(System.nanoTime()-started)/1e9);
        g.drawString(font,guidance,left,height-46,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF8FA7A0));
    }
}

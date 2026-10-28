package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

final class TrackPlaylistScreen extends MusicScreen {
    private final String trackId,title;
    TrackPlaylistScreen(Screen parent,ClientRuntime runtime,String trackId,String title){super("Add song to playlist",parent,runtime);this.trackId=trackId;this.title=title;}
    @Override protected void init(){
        super.init();
        var list=addRenderableWidget(new DenseList<Playlist>(left,69,contentWidth,Math.max(28,bodyBottom()-125),(g,p,x,y,w,hover)->{
            if(hover)g.fill(x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));g.text(menuFont,fit(p.name,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
        },(p,event,twice)->{if(runtime.addTrackToPlaylist(trackId,p.id))onClose();},p->p.name));
        list.items(runtime.library().playlists);button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){g.text(menuFont,fit(title,contentWidth),left,49,MenuAppearance.color(MenuColor.ACCENT,0xFFBCBDF8));}
}

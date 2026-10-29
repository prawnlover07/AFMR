package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

final class TrackPlaylistScreen extends MusicScreen {
    private final String trackId,title;
    TrackPlaylistScreen(Screen parent,ClientRuntime runtime,String trackId,String title){super("Add song to playlist",parent,runtime);this.trackId=trackId;this.title=title;}
    @Override protected void init(){
        super.init();
        var list=addRenderableWidget(new DenseList<Playlist>(left,69,contentWidth,Math.max(28,bodyBottom()-125),(g,p,x,y,w,hover)->{
            if(hover)net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));menuFont.drawShadow(g,fit(p.name,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
        },(p,clickX,clickY,button,twice)->{if(runtime.addTrackToPlaylist(trackId,p.id))onClose();},p->p.name));
        list.items(runtime.library().playlists);button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){menuFont.drawShadow(g,fit(title,contentWidth),left,49,MenuAppearance.color(MenuColor.ACCENT,0xFFBCBDF8));}
}

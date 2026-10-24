package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.ListeningHistory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

final class HistoryPlaylistScreen extends MusicScreen {
    private final ListeningHistory.Entry entry;
    HistoryPlaylistScreen(Screen parent,ClientRuntime runtime,ListeningHistory.Entry entry){super("Add song to playlist",parent,runtime);this.entry=entry;}
    @Override protected void init(){
        super.init();
        var list=addRenderableWidget(new DenseList<Playlist>(left,69,contentWidth,Math.max(28,height-125),(g,p,x,y,w,hover)->{
            if(hover)g.fill(x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF35594E));g.drawString(font,fit(p.name,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFDCE6E0));
        },(p,clickX,clickY,button,twice)->{if(runtime.addHistoryTrack(entry,p.id))onClose();},p->p.name));
        list.items(runtime.library().playlists);button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){g.drawString(font,fit(entry.title(),contentWidth),left,49,MenuAppearance.color(MenuColor.ACCENT,0xFFB9D7B7));}
}

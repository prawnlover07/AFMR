package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

final class NewPlaylistScreen extends MusicScreen {
    private final LibraryScreen editor;
    private final dev.animal.afmr.core.Library.Playlist source;
    private EditBox name;
    private String error="";
    NewPlaylistScreen(LibraryScreen parent,ClientRuntime runtime,dev.animal.afmr.core.Library.Playlist source){super(source==null?"New playlist":"Clone playlist",parent,runtime);editor=parent;this.source=source;}
    @Override protected void init(){
        super.init();name=edit(source==null?"":source.name.substring(0,Math.min(75,source.name.length()))+" copy","Playlist name",left,70,contentWidth,null);name.setMaxLength(80);setInitialFocus(name);
        button(source==null?"Create":"Clone",left,height-29,Math.min(190,contentWidth-112),()->{
            try{var p=source==null?runtime.library().createPlaylist(name.getValue()):runtime.library().clonePlaylist(source.id,name.getValue());editor.selectPlaylist(p.id);runtime.save();minecraft.setScreen(editor);}
            catch(IllegalArgumentException e){error=e.getMessage();}
        });
        button("Cancel",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        g.drawString(font,"Playlist name",left,53,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        g.drawString(font,fit(error,contentWidth),left,height-48,0xFFFFA0A0);
    }
}

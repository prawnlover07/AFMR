package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;

final class NewPlaylistScreen extends MusicScreen {
    private final LibraryScreen editor;
    private final dev.animal.afmr.core.Library.Playlist source;
    private MenuField name;
    private String error="";
    NewPlaylistScreen(LibraryScreen parent,ClientRuntime runtime,dev.animal.afmr.core.Library.Playlist source){super(source==null?"New playlist":"Duplicate playlist",parent,runtime);editor=parent;this.source=source;}
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();name=edit(source==null?"":source.name.substring(0,Math.min(75,source.name.length()))+" copy","Playlist name",left,70,contentWidth,null);name.setMaxLength(80);setInitialFocus(name);
        button(source==null?"Create":"Duplicate",left,bodyBottom()-29,Math.min(190,contentWidth-112),()->{
            try{var p=source==null?runtime.library().createPlaylist(name.getValue()):runtime.library().clonePlaylist(source.id,name.getValue());runtime.playlistEdits().created(runtime.library(),p);editor.selectPlaylist(p.id);runtime.save();minecraft.setScreen(editor);}
            catch(IllegalArgumentException e){error=e.getMessage();}
        });
        button("Cancel",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        menuFont.drawShadow(g,"Playlist name",left,53,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        menuFont.drawShadow(g,fit(error,contentWidth),left,bodyBottom()-48,0xFFFFA0A0);
    }
}

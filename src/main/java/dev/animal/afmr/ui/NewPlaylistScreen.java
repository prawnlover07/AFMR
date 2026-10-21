package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;

final class NewPlaylistScreen extends MusicScreen {
    private final PlaylistManagerScreen manager;
    private EditBox name;
    private String error="";
    NewPlaylistScreen(PlaylistManagerScreen parent,ClientRuntime runtime){super("New playlist",parent,runtime);manager=parent;}
    @Override protected void init(){
        super.init();name=edit("","Playlist name",left,70,contentWidth,null);name.setMaxLength(80);setInitialFocus(name);
        button("Create & set conditions",left,height-29,Math.min(190,contentWidth-112),()->{
            try{var p=runtime.library().createPlaylist(name.getValue());manager.select(p.id);runtime.save();minecraft.gui.setScreen(new CustomConditionsScreen(manager,runtime,p));}
            catch(IllegalArgumentException e){error=e.getMessage();}
        });
        button("Cancel",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,"Playlist name",left,53,0xFFD9E8E0);
        g.text(font,fit("Choose only the conditions you need. All enabled filters must match.",contentWidth),left,106,0xFF8FA7A0);
        g.text(font,fit("New playlists start above Day/Night. Change their rank in Playlists & priority.",contentWidth),left,126,0xFF8FA7A0);
        g.text(font,fit("Add music through Edit songs or Import playlist after creating it.",contentWidth),left,146,0xFF8FA7A0);
        g.text(font,fit(error,contentWidth),left,height-48,0xFFFFA0A0);
    }
}

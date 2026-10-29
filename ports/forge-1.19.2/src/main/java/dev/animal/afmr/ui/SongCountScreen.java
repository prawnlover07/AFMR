package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.MenuColor;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class SongCountScreen extends MusicScreen {
    private final Playlist playlist;
    private MenuField count;
    private String error="";
    SongCountScreen(Screen parent,ClientRuntime runtime,Playlist playlist){
        super("Songs before silence",parent,runtime);this.playlist=playlist;
    }
    @Override protected void init(){
        super.init();
        if(playlist!=null)toggle("Override global count",left,54,contentWidth,()->playlist.customSongCount,()->{
            if(apply()){playlist.customSongCount=!playlist.customSongCount;runtime.save();rebuildWidgets();}
        });
        count=edit(Integer.toString(playlist==null?runtime.library().settings.songsBeforeSilence:playlist.songsBeforeSilence),
            "Songs before silence (1-1000)",left,88,contentWidth,null);
        var help=Component.translatable("afmr.copy.songcount.count");
        if(!help.getString().isBlank())count.description(help);
        button("Save & back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
    private boolean apply(){
        try{
            int value=Integer.parseInt(count.getValue());if(value<1||value>1000)throw new NumberFormatException();
            if(playlist==null)runtime.library().settings.songsBeforeSilence=value;else playlist.songsBeforeSilence=value;
            runtime.save();error="";return true;
        }catch(NumberFormatException e){error="Songs before silence: whole number from 1 to 1000.";return false;}
    }
    @Override protected boolean saveChanges(){return apply();}
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(PoseStack g,int x,int y,float dt){
        String state=playlist==null?"Global default":playlist.customSongCount?playlist.name:"Using global count: "+runtime.library().settings.songsBeforeSilence;
        menuFont.drawShadow(g,fit(state,contentWidth),left,119,MenuAppearance.color(MenuColor.ACCENT,0xFFBCBDF8));
        if(!error.isEmpty())menuFont.drawShadow(g,fit(error,contentWidth),left,bodyBottom()-44,0xFFFFA0A0);
    }
}

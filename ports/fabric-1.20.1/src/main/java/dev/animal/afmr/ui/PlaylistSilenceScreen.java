package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class PlaylistSilenceScreen extends MusicScreen {
    private final Playlist playlist;
    private EditBox minimum,maximum;
    private String error="";
    PlaylistSilenceScreen(Screen parent,ClientRuntime runtime,Playlist playlist){
        super("Playlist silence",parent,runtime);this.playlist=playlist;
    }
    @Override protected void init(){
        super.init();
        var customSilenceSwitchHelpControl=button("Custom silence: "+(playlist.customSilence?"on":"off"),left,78,contentWidth,()->{
            if(apply()){playlist.customSilence=!playlist.customSilence;runtime.save();rebuildWidgets();}
        });
        var customSilenceSwitchHelpText=Component.translatable("afmr.copy.playlistsilence.custom_silence_switch");
        if(!customSilenceSwitchHelpText.getString().isBlank())customSilenceSwitchHelpControl.setTooltip(Tooltip.create(customSilenceSwitchHelpText));
        button("Songs before silence: "+(playlist.customSongCount?playlist.songsBeforeSilence:"global"),left,110,contentWidth,()->{
            if(apply())minecraft.setScreen(new SongCountScreen(this,runtime,playlist));
        });
        int fieldX=left+contentWidth-100;
        minimum=edit(Double.toString(playlist.silenceMinSeconds),"Min silence (sec)",fieldX,138,100,null);
        maximum=edit(Double.toString(playlist.silenceMaxSeconds),"Max silence (sec)",fieldX,166,100,null);
        var minimumSilenceHelpText=Component.translatable("afmr.copy.playlistsilence.minimum_silence");
        if(!minimumSilenceHelpText.getString().isBlank())minimum.setTooltip(Tooltip.create(minimumSilenceHelpText));
        var maximumSilenceHelpText=Component.translatable("afmr.copy.playlistsilence.maximum_silence");
        if(!maximumSilenceHelpText.getString().isBlank())maximum.setTooltip(Tooltip.create(maximumSilenceHelpText));
        button("Save & back",left,height-29,contentWidth,this::onClose);
    }
    private boolean apply(){
        try{
            double min=Double.parseDouble(minimum.getValue()),max=Double.parseDouble(maximum.getValue());
            if(!Double.isFinite(min)||!Double.isFinite(max)||min<0||max<min||max>86400)throw new NumberFormatException();
            playlist.silenceMinSeconds=min;playlist.silenceMaxSeconds=max;runtime.save();error="";return true;
        }catch(NumberFormatException e){error="Silence: 0-86400; max >= min.";return false;}
    }
    @Override protected boolean saveChanges(){return apply();}
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        g.drawString(font,fit(playlist.name,contentWidth),left,56,MenuAppearance.color(MenuColor.ACCENT,0xFF8DE0BE));
        g.drawString(font,fit("Min silence (sec)",contentWidth-108),left,144,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        g.drawString(font,fit("Max silence (sec)",contentWidth-108),left,172,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        if(!error.isBlank())g.drawString(font,fit(error,contentWidth),left,height-43,0xFFFFA0A0);
    }
}

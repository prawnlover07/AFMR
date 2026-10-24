package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class PlaylistVolumeScreen extends MusicScreen {
    private final Playlist playlist;
    PlaylistVolumeScreen(Screen parent,ClientRuntime runtime,Playlist playlist){
        super("Playlist volume",parent,runtime);this.playlist=playlist;
    }
    @Override protected void init(){
        super.init();
        var slider=addRenderableWidget(new AbstractSliderButton(left,80,contentWidth,20,
            Component.literal("Volume: "+Math.round(playlist.volume*100)+"%"),playlist.volume){
            @Override protected void updateMessage(){setMessage(Component.literal("Volume: "+Math.round(value*100)+"%"));}
            @Override protected void applyValue(){playlist.volume=Math.round(value*100)/100.0;}
        });
        var liveVolumeSliderHelpText=Component.translatable("afmr.copy.playlistvolume.live_volume_slider");
        if(!liveVolumeSliderHelpText.getString().isBlank())slider.setTooltip(Tooltip.create(liveVolumeSliderHelpText));
        button("Reset to 100%",left,110,contentWidth,()->{playlist.volume=1;runtime.save();rebuildWidgets();});
        button("Back",left,height-29,contentWidth,this::onClose);
    }
    @Override public void removed(){runtime.save();super.removed();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit(playlist.name,contentWidth),left,56,MenuAppearance.color(MenuColor.ACCENT,0xFF8DE0BE));
    }
}

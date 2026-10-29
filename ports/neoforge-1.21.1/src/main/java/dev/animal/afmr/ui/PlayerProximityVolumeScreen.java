package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlayerProximityVolume;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class PlayerProximityVolumeScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private EditBox nearDistance,farDistance,nearVolume,farVolume;
    private int half;
    private String error="";
    PlayerProximityVolumeScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Player proximity volume",parent,runtime);this.conditions=conditions;
    }
    @Override protected void init(){
        super.init();half=(contentWidth-12)/2;var volume=conditions.proximityVolume;
        toggle("Proximity volume",left,48,contentWidth,()->volume.enabled,()->{
            if(apply()){volume.enabled=!volume.enabled;runtime.save();rebuildWidgets();}
        }).setTooltip(Tooltip.create(Component.translatable("afmr.copy.proximityvolume.enabled")));
        nearDistance=edit(Double.toString(volume.nearDistance),"Near distance (blocks)",left,94,half,null);
        nearVolume=edit(Double.toString(volume.nearVolume*100),"Near volume (%)",left+half+12,94,half,null);
        farDistance=edit(Double.toString(volume.farDistance),"Far distance (blocks)",left,142,half,null);
        farVolume=edit(Double.toString(volume.farVolume*100),"Far volume (%)",left+half+12,142,half,null);
        nearDistance.setTooltip(Tooltip.create(Component.translatable("afmr.copy.proximityvolume.near_distance")));
        farDistance.setTooltip(Tooltip.create(Component.translatable("afmr.copy.proximityvolume.far_distance")));
        nearVolume.setTooltip(Tooltip.create(Component.translatable("afmr.copy.proximityvolume.near_volume")));
        farVolume.setTooltip(Tooltip.create(Component.translatable("afmr.copy.proximityvolume.far_volume")));
        button("Reverse volumes",left,172,contentWidth,()->{
            if(apply()){double swap=volume.nearVolume;volume.nearVolume=volume.farVolume;volume.farVolume=swap;runtime.save();rebuildWidgets();}
        }).setTooltip(Tooltip.create(Component.translatable("afmr.copy.proximityvolume.reverse")));
        button("Save & back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
    private boolean apply(){
        try{
            var draft=new PlayerProximityVolume();draft.nearDistance=Double.parseDouble(nearDistance.getValue());draft.farDistance=Double.parseDouble(farDistance.getValue());
            draft.nearVolume=Double.parseDouble(nearVolume.getValue())/100;draft.farVolume=Double.parseDouble(farVolume.getValue())/100;
            if(!draft.valid()){error="Distance: 0-1024, far > near; volume: 0-100%.";return false;}
            var volume=conditions.proximityVolume;volume.nearDistance=draft.nearDistance;volume.farDistance=draft.farDistance;
            volume.nearVolume=draft.nearVolume;volume.farVolume=draft.farVolume;runtime.save();error="";return true;
        }catch(NumberFormatException e){error="Invalid number.";return false;}
    }
    @Override protected boolean saveChanges(){return apply();}
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        int color=MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED);
        g.drawString(menuFont,fit("Near distance (blocks)",half),left,79,color);
        g.drawString(menuFont,fit("Near volume (%)",half),left+half+12,79,color);
        g.drawString(menuFont,fit("Far distance (blocks)",half),left,127,color);
        g.drawString(menuFont,fit("Far volume (%)",half),left+half+12,127,color);
        if(!error.isBlank())g.drawString(menuFont,fit(error,contentWidth),left,bodyBottom()-43,0xFFFFA0A0);
    }
}

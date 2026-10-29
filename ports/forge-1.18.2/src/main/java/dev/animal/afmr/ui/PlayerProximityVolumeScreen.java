package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlayerProximityVolume;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class PlayerProximityVolumeScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private MenuField nearDistance,farDistance,nearVolume,farVolume;
    private int half;
    private String error="";
    PlayerProximityVolumeScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Player proximity volume",parent,runtime);this.conditions=conditions;
    }
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();half=(contentWidth-12)/2;var volume=conditions.proximityVolume;
        toggle("Proximity volume",left,48,contentWidth,()->volume.enabled,()->{
            if(apply()){volume.enabled=!volume.enabled;runtime.save();init(minecraft,width,height);}
        }).description(new net.minecraft.network.chat.TranslatableComponent("afmr.copy.proximityvolume.enabled"));
        nearDistance=edit(Double.toString(volume.nearDistance),"Near distance (blocks)",left,94,half,null);
        nearVolume=edit(Double.toString(volume.nearVolume*100),"Near volume (%)",left+half+12,94,half,null);
        farDistance=edit(Double.toString(volume.farDistance),"Far distance (blocks)",left,142,half,null);
        farVolume=edit(Double.toString(volume.farVolume*100),"Far volume (%)",left+half+12,142,half,null);
        nearDistance.description(new net.minecraft.network.chat.TranslatableComponent("afmr.copy.proximityvolume.near_distance"));
        farDistance.description(new net.minecraft.network.chat.TranslatableComponent("afmr.copy.proximityvolume.far_distance"));
        nearVolume.description(new net.minecraft.network.chat.TranslatableComponent("afmr.copy.proximityvolume.near_volume"));
        farVolume.description(new net.minecraft.network.chat.TranslatableComponent("afmr.copy.proximityvolume.far_volume"));
        button("Reverse volumes",left,172,contentWidth,()->{
            if(apply()){double swap=volume.nearVolume;volume.nearVolume=volume.farVolume;volume.farVolume=swap;runtime.save();init(minecraft,width,height);}
        }).description(new net.minecraft.network.chat.TranslatableComponent("afmr.copy.proximityvolume.reverse"));
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
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        int color=MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED);
        menuFont.drawShadow(g,fit("Near distance (blocks)",half),left,79,color);
        menuFont.drawShadow(g,fit("Near volume (%)",half),left+half+12,79,color);
        menuFont.drawShadow(g,fit("Far distance (blocks)",half),left,127,color);
        menuFont.drawShadow(g,fit("Far volume (%)",half),left+half+12,127,color);
        if(!error.isBlank())menuFont.drawShadow(g,fit(error,contentWidth),left,bodyBottom()-43,0xFFFFA0A0);
    }
}

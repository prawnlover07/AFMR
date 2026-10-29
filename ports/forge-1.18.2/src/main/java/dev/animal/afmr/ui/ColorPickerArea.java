package dev.animal.afmr.ui;

import dev.animal.afmr.core.ColorPickerState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

/** Cached gradient strips; pointer dragging and arrow keys edit the same HSV state. */
final class ColorPickerArea extends AbstractWidget implements Described {
    private Component description;
    public Component description(){return description;}
    public void description(Component text){description=text;}

    private static final int STRIPS=64;
    private static final int[] HUES=new int[7];
    static{for(int i=0;i<HUES.length;i++)HUES[i]=0xFF000000|ColorPickerState.rgb(i/6d,1,1);}
    private final ColorPickerState state;
    private final boolean hue;
    private final Runnable changed;
    private final int[] tops=new int[STRIPS];
    private double cachedHue=-1;
    ColorPickerArea(int x,int y,int w,int h,boolean hue,ColorPickerState state,Runnable changed){
        super(x,y,w,h,new net.minecraft.network.chat.TextComponent(hue?"Hue":"Saturation and brightness"));this.hue=hue;this.state=state;this.changed=changed;
        var pickerHelpText=new net.minecraft.network.chat.TranslatableComponent(hue?"afmr.copy.colorpickerarea.hue":"afmr.copy.colorpickerarea.saturation_and_brightness");
        if(!pickerHelpText.getString().isBlank())description(pickerHelpText);
    }
    private void choose(double x,double y){
        if(hue)state.hue((y-this.y)/Math.max(1,height-1));
        else state.saturationValue((x-this.x)/Math.max(1,width-1),1-(y-this.y)/Math.max(1,height-1));
        changed.run();
    }
    @Override public void onClick(double x,double y){choose(x,y);}
    @Override protected void onDrag(double x,double y,double dx,double dy){choose(x,y);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(!isFocused()||!active)return false;
        if(key!=InputConstants.KEY_LEFT&&key!=InputConstants.KEY_RIGHT&&key!=InputConstants.KEY_UP&&key!=InputConstants.KEY_DOWN)return false;
        if(hue)state.hue(state.hue()+((key==InputConstants.KEY_RIGHT||key==InputConstants.KEY_DOWN)?0.01:-0.01));
        else state.saturationValue(state.saturation()+(key==InputConstants.KEY_LEFT?-0.01:key==InputConstants.KEY_RIGHT?0.01:0),state.value()+(key==InputConstants.KEY_UP?0.01:key==InputConstants.KEY_DOWN?-0.01:0));
        changed.run();return true;
    }
    @Override public void renderButton(PoseStack g,int mx,int my,float delta){
        if(isFocused()){
            net.minecraft.client.gui.GuiComponent.fill(g,this.x-2,this.y-2,(this.x+width)+2,this.y-1,0xFFFFFFFF);net.minecraft.client.gui.GuiComponent.fill(g,this.x-2,(this.y+height)+1,(this.x+width)+2,(this.y+height)+2,0xFFFFFFFF);
            net.minecraft.client.gui.GuiComponent.fill(g,this.x-2,this.y-1,this.x-1,(this.y+height)+1,0xFFFFFFFF);net.minecraft.client.gui.GuiComponent.fill(g,(this.x+width)+1,this.y-1,(this.x+width)+2,(this.y+height)+1,0xFFFFFFFF);
        }
        if(hue){
            for(int i=0;i<6;i++)fillGradient(g,this.x,this.y+height*i/6,(this.x+width),this.y+height*(i+1)/6,HUES[i],HUES[i+1]);
            int y=this.y+(int)Math.round(state.hue()*(height-1));
            net.minecraft.client.gui.GuiComponent.fill(g,this.x-1,y-1,(this.x+width)+1,y+2,0xFF000000);net.minecraft.client.gui.GuiComponent.fill(g,this.x-1,y,(this.x+width)+1,y+1,0xFFFFFFFF);
        }else{
            if(cachedHue!=state.hue()){
                cachedHue=state.hue();for(int i=0;i<STRIPS;i++)tops[i]=0xFF000000|ColorPickerState.rgb(cachedHue,i/(double)(STRIPS-1),1);
            }
            for(int i=0;i<STRIPS;i++)fillGradient(g,this.x+width*i/STRIPS,this.y,this.x+width*(i+1)/STRIPS,(this.y+height),tops[i],0xFF000000);
            int x=this.x+(int)Math.round(state.saturation()*(width-1)),y=this.y+(int)Math.round((1-state.value())*(height-1));
            net.minecraft.client.gui.GuiComponent.fill(g,x-3,y-3,x+4,y-2,0xFF000000);net.minecraft.client.gui.GuiComponent.fill(g,x-3,y+3,x+4,y+4,0xFF000000);net.minecraft.client.gui.GuiComponent.fill(g,x-3,y-2,x-2,y+3,0xFF000000);net.minecraft.client.gui.GuiComponent.fill(g,x+3,y-2,x+4,y+3,0xFF000000);
            net.minecraft.client.gui.GuiComponent.fill(g,x-2,y-2,x+3,y-1,0xFFFFFFFF);net.minecraft.client.gui.GuiComponent.fill(g,x-2,y+2,x+3,y+3,0xFFFFFFFF);net.minecraft.client.gui.GuiComponent.fill(g,x-2,y-1,x-1,y+2,0xFFFFFFFF);net.minecraft.client.gui.GuiComponent.fill(g,x+2,y-1,x+3,y+2,0xFFFFFFFF);
        }
    }
    @Override public void updateNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,getMessage());}
}

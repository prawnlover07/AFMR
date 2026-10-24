package dev.animal.afmr.ui;

import dev.animal.afmr.core.ColorPickerState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

/** Cached gradient strips; pointer dragging and arrow keys edit the same HSV state. */
final class ColorPickerArea extends AbstractWidget {
    private static final int STRIPS=64;
    private static final int[] HUES=new int[7];
    static{for(int i=0;i<HUES.length;i++)HUES[i]=0xFF000000|ColorPickerState.rgb(i/6d,1,1);}
    private final ColorPickerState state;
    private final boolean hue;
    private final Runnable changed;
    private final int[] tops=new int[STRIPS];
    private double cachedHue=-1;
    ColorPickerArea(int x,int y,int w,int h,boolean hue,ColorPickerState state,Runnable changed){
        super(x,y,w,h,Component.literal(hue?"Hue":"Saturation and brightness"));this.hue=hue;this.state=state;this.changed=changed;
        var pickerHelpText=Component.translatable(hue?"afmr.copy.colorpickerarea.hue":"afmr.copy.colorpickerarea.saturation_and_brightness");
        if(!pickerHelpText.getString().isBlank())setTooltip(Tooltip.create(pickerHelpText));
    }
    private void choose(double x,double y){
        if(hue)state.hue((y-getY())/Math.max(1,height-1));
        else state.saturationValue((x-getX())/Math.max(1,width-1),1-(y-getY())/Math.max(1,height-1));
        changed.run();
    }
    @Override public void onClick(MouseButtonEvent event,boolean twice){choose(event.x(),event.y());}
    @Override protected void onDrag(MouseButtonEvent event,double dx,double dy){choose(event.x(),event.y());}
    @Override public boolean keyPressed(KeyEvent event){
        if(!isFocused()||!active)return false;int key=event.key();
        if(key!=InputConstants.KEY_LEFT&&key!=InputConstants.KEY_RIGHT&&key!=InputConstants.KEY_UP&&key!=InputConstants.KEY_DOWN)return false;
        if(hue)state.hue(state.hue()+((key==InputConstants.KEY_RIGHT||key==InputConstants.KEY_DOWN)?0.01:-0.01));
        else state.saturationValue(state.saturation()+(key==InputConstants.KEY_LEFT?-0.01:key==InputConstants.KEY_RIGHT?0.01:0),state.value()+(key==InputConstants.KEY_UP?0.01:key==InputConstants.KEY_DOWN?-0.01:0));
        changed.run();return true;
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(isFocused()){
            g.fill(getX()-2,getY()-2,getRight()+2,getY()-1,0xFFFFFFFF);g.fill(getX()-2,getBottom()+1,getRight()+2,getBottom()+2,0xFFFFFFFF);
            g.fill(getX()-2,getY()-1,getX()-1,getBottom()+1,0xFFFFFFFF);g.fill(getRight()+1,getY()-1,getRight()+2,getBottom()+1,0xFFFFFFFF);
        }
        if(hue){
            for(int i=0;i<6;i++)g.fillGradient(getX(),getY()+height*i/6,getRight(),getY()+height*(i+1)/6,HUES[i],HUES[i+1]);
            int y=getY()+(int)Math.round(state.hue()*(height-1));
            g.fill(getX()-1,y-1,getRight()+1,y+2,0xFF000000);g.fill(getX()-1,y,getRight()+1,y+1,0xFFFFFFFF);
        }else{
            if(cachedHue!=state.hue()){
                cachedHue=state.hue();for(int i=0;i<STRIPS;i++)tops[i]=0xFF000000|ColorPickerState.rgb(cachedHue,i/(double)(STRIPS-1),1);
            }
            for(int i=0;i<STRIPS;i++)g.fillGradient(getX()+width*i/STRIPS,getY(),getX()+width*(i+1)/STRIPS,getBottom(),tops[i],0xFF000000);
            int x=getX()+(int)Math.round(state.saturation()*(width-1)),y=getY()+(int)Math.round((1-state.value())*(height-1));
            g.fill(x-3,y-3,x+4,y-2,0xFF000000);g.fill(x-3,y+3,x+4,y+4,0xFF000000);g.fill(x-3,y-2,x-2,y+3,0xFF000000);g.fill(x+3,y-2,x+4,y+3,0xFF000000);
            g.fill(x-2,y-2,x+3,y-1,0xFFFFFFFF);g.fill(x-2,y+2,x+3,y+3,0xFFFFFFFF);g.fill(x-2,y-1,x-1,y+2,0xFFFFFFFF);g.fill(x+2,y-1,x+3,y+2,0xFFFFFFFF);
        }
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,getMessage());}
}

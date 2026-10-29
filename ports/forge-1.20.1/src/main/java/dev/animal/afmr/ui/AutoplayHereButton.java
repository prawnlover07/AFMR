package dev.animal.afmr.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A destructive action requires one continuous, cancellable hold. */
final class AutoplayHereButton extends FlatButton {
    private static final Component LABEL=Component.literal("Set playback area here");
    private static final Component WARNING=Component.literal("Are you sure? Replaces conditions");
    private static final long HOLD_NANOS=1_000_000_000L;
    private long pressedAt;
    private boolean holding,latched,keyboard,pointerInside;
    private int heldKey;
    AutoplayHereButton(int x,int y,int w,int h,float scale,Runnable action){
        super("Set playback area here",x,y,w,h,scale,action);
        var autoplayHereHelpText=Component.translatable("afmr.copy.autoplayhere.autoplay_here");
        if(!autoplayHereHelpText.getString().isBlank())setTooltip(Tooltip.create(autoplayHereHelpText));
    }
    private void begin(boolean fromKeyboard,int key){
        if(latched)return;
        latched=holding=true;keyboard=fromKeyboard;heldKey=key;pointerInside=true;pressedAt=System.nanoTime();
        setMessage(WARNING);setLabelTextColor(0xFFFF7777);
    }
    void cancelHold(){holding=latched=false;setMessage(LABEL);setLabelTextColor(null);}
    void tick(){
        if(!holding)return;
        if(!active||!visible||keyboard&&!isFocused()||!keyboard&&!pointerInside){holding=false;setMessage(LABEL);setLabelTextColor(null);return;}
        if(System.nanoTime()-pressedAt>=HOLD_NANOS){
            holding=false;setMessage(LABEL);setLabelTextColor(null);action.run();
        }
    }
    @Override public void onClick(double x,double y){begin(false,0);}
    @Override public void onRelease(double x,double y){cancelHold();}
    @Override protected void onDrag(double x,double y,double dx,double dy){
        pointerInside=isMouseOver(x,y);
        if(!pointerInside){holding=false;setMessage(LABEL);setLabelTextColor(null);}
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(active&&isFocused()&&(key==InputConstants.KEY_RETURN||key==InputConstants.KEY_SPACE)){begin(true,key);return true;}
        return false;
    }
    @Override public boolean keyReleased(int key,int scan,int modifiers){if(keyboard&&key==heldKey){cancelHold();return true;}return false;}
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
        pointerInside=isHovered();
        super.renderWidget(g,mx,my,dt);
        if(holding){
            int filled=(int)(width*Math.max(0,Math.min((double)(System.nanoTime()-pressedAt)/HOLD_NANOS,1)));
            g.fill(getX(),(getY()+getHeight())-3,getX()+filled,(getY()+getHeight()),0xFFFF7777);
        }
    }
}

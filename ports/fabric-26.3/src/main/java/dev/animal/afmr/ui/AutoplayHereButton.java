package dev.animal.afmr.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
        setMessage(WARNING);textColor(0xFFFF7777);
    }
    void cancelHold(){holding=latched=false;setMessage(LABEL);textColor(null);}
    void tick(){
        if(!holding)return;
        if(!active||!visible||keyboard&&!isFocused()||!keyboard&&!pointerInside){holding=false;setMessage(LABEL);textColor(null);return;}
        if(System.nanoTime()-pressedAt>=HOLD_NANOS){
            holding=false;setMessage(LABEL);textColor(null);action.run();
        }
    }
    @Override public void onClick(MouseButtonEvent event,boolean twice){begin(false,0);}
    @Override public void onRelease(MouseButtonEvent event){cancelHold();}
    @Override protected void onDrag(MouseButtonEvent event,double dx,double dy){
        pointerInside=isMouseOver(event.x(),event.y());
        if(!pointerInside){holding=false;setMessage(LABEL);textColor(null);}
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(active&&isFocused()&&(event.key()==InputConstants.KEY_RETURN||event.key()==InputConstants.KEY_SPACE)){begin(true,event.key());return true;}
        return false;
    }
    @Override public boolean keyReleased(KeyEvent event){if(keyboard&&event.key()==heldKey){cancelHold();return true;}return false;}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        pointerInside=isHovered();
        super.extractWidgetRenderState(g,mx,my,dt);
        if(holding){
            int filled=(int)(width*Math.clamp((double)(System.nanoTime()-pressedAt)/HOLD_NANOS,0,1));
            g.fill(getX(),getBottom()-3,getX()+filled,getBottom(),0xFFFF7777);
        }
    }
}

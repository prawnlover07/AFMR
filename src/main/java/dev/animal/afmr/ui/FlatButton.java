package dev.animal.afmr.ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
final class FlatButton extends AbstractWidget {
    private final Runnable action;boolean selected;
    FlatButton(String label,int x,int y,int w,int h,Runnable action){super(x,y,w,h,Component.literal(label));this.action=action;}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(getX(),getY(),getX()+width,getY()+height,selected?0xFF35594E:isHoveredOrFocused()?0xFF3B4949:0xFF283236);
        if(selected)g.fill(getX(),getY()+height-2,getX()+width,getY()+height,0xFF8DE0BE);
        var font=Minecraft.getInstance().font;g.text(font,font.plainSubstrByWidth(getMessage().getString(),width-8),getX()+4,getY()+(height-8)/2,active?0xFFE1ECE6:0xFF6D7976,false);
    }
    @Override public void onClick(MouseButtonEvent e,boolean twice){action.run();}
    @Override public boolean keyPressed(KeyEvent e){if(isFocused()&&active&&(e.key()==InputConstants.KEY_RETURN||e.key()==InputConstants.KEY_SPACE)){action.run();return true;}return false;}
    @Override protected void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,getMessage());}
}

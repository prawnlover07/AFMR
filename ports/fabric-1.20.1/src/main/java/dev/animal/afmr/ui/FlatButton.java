package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
class FlatButton extends AbstractWidget {
    protected final Runnable action;
    boolean selected;
    private Integer labelTextColor;
    FlatButton(String label,int x,int y,int w,int h,float scale,Runnable action){super(x,y,w,h,Component.literal(label));this.action=action;}
    void setLabelTextColor(Integer color){labelTextColor=color;}
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
        g.fill(getX(),getY(),getX()+width,getY()+height,selected?MenuAppearance.background(MenuColor.SELECTION,0x35594E):isHoveredOrFocused()?MenuAppearance.background(MenuColor.HOVER,0x3B4949):MenuAppearance.background(MenuColor.CONTROLS,0x283236));
        if(selected)g.fill(getX(),getY()+height-2,getX()+width,getY()+height,MenuAppearance.color(MenuColor.ACCENT,0xFF8DE0BE));
        var font=Minecraft.getInstance().font;
        g.drawString(font,font.plainSubstrByWidth(getMessage().getString(),width-8),getX()+4,getY()+(height-8)/2,active?(labelTextColor==null?MenuAppearance.color(MenuColor.TEXT,0xFFE1ECE6):labelTextColor):MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF6D7976),false);
    }
    @Override public void onClick(double x,double y){action.run();}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(isFocused()&&active&&(key==InputConstants.KEY_RETURN||key==InputConstants.KEY_SPACE)){action.run();return true;}return false;}
    @Override protected void updateWidgetNarration(NarrationElementOutput out){defaultButtonNarrationText(out);}
}

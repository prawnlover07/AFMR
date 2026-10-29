package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Physical hit bounds with the same typography/geometry as the workspace's scaled tabs. */
final class WorkspaceButton extends FlatButton {
    private final float fontScale;
    private final int logicalWidth;
    WorkspaceButton(String label,int x,int y,int w,int h,float scale,Runnable action){
        super(label,Math.round(x*scale),Math.round(y*scale),Math.round(w*scale),Math.round(h*scale),1,action);
        fontScale=scale;logicalWidth=w;
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
        g.fill(getX(),getY(),(getX()+getWidth()),(getY()+getHeight()),selected?MenuAppearance.background(MenuColor.SELECTION,0x304963):isHoveredOrFocused()?MenuAppearance.background(MenuColor.HOVER,0x3B424D):MenuAppearance.background(MenuColor.CONTROLS,0x2C3038));
        if(selected)g.fill(getX(),(getY()+getHeight())-Math.max(1,Math.round(2*fontScale)),(getX()+getWidth()),(getY()+getHeight()),MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        g.pose().pushPose();g.pose().translate(getX(),getY(),0);g.pose().scale(fontScale,fontScale,1);
        var font=MenuAppearance.font();g.drawString(font,font.plainSubstrByWidth(getMessage().getString(),logicalWidth-8),4,Math.round((height/fontScale-8)/2),MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3),false);g.pose().popPose();
    }
}

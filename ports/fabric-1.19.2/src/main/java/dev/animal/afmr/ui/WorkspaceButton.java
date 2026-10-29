package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;

/** Physical hit bounds with the same typography/geometry as the workspace's scaled tabs. */
final class WorkspaceButton extends FlatButton {
    private final float fontScale;
    private final int logicalWidth;
    WorkspaceButton(String label,int x,int y,int w,int h,float scale,Runnable action){
        super(label,Math.round(x*scale),Math.round(y*scale),Math.round(w*scale),Math.round(h*scale),1,action);
        fontScale=scale;logicalWidth=w;
    }
    @Override public void renderButton(PoseStack g,int mx,int my,float dt){
        net.minecraft.client.gui.GuiComponent.fill(g,this.x,this.y,(this.x+getWidth()),(this.y+getHeight()),selected?MenuAppearance.background(MenuColor.SELECTION,0x304963):isHoveredOrFocused()?MenuAppearance.background(MenuColor.HOVER,0x3B424D):MenuAppearance.background(MenuColor.CONTROLS,0x2C3038));
        if(selected)net.minecraft.client.gui.GuiComponent.fill(g,this.x,(this.y+getHeight())-Math.max(1,Math.round(2*fontScale)),(this.x+getWidth()),(this.y+getHeight()),MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        g.pushPose();g.translate(this.x,this.y,0);g.scale(fontScale,fontScale,1);
        var font=MenuAppearance.font();font.draw(g,font.plainSubstrByWidth(getMessage().getString(),logicalWidth-8),4,Math.round((height/fontScale-8)/2),MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3));g.popPose();
    }
}

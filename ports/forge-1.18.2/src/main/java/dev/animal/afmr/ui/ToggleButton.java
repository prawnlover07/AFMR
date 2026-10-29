package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.BooleanSupplier;

/** A setting row: the box and its label share one toggle target. */
final class ToggleButton extends FlatButton {
    private final BooleanSupplier checked;
    ToggleButton(String label,int x,int y,int w,int h,float scale,BooleanSupplier checked,Runnable action){
        super(label,x,y,w,h,scale,action);this.checked=checked;
    }
    @Override public void renderButton(PoseStack g,int mx,int my,float dt){
        if(isHoveredOrFocused())net.minecraft.client.gui.GuiComponent.fill(g,this.x,this.y,(this.x+getWidth()),(this.y+getHeight()),MenuAppearance.background(MenuColor.HOVER,0x3B424D));
        int x=this.x+2,y=this.y+(height-12)/2;
        net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+12,y+12,MenuAppearance.color(MenuColor.BORDERS,0xFF627078));
        net.minecraft.client.gui.GuiComponent.fill(g,x+1,y+1,x+11,y+11,MenuAppearance.background(MenuColor.BACKGROUND,0x17191D));
        if(checked.getAsBoolean()){
            int color=MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF);
            net.minecraft.client.gui.GuiComponent.fill(g,x+3,y+5,x+5,y+8,color);net.minecraft.client.gui.GuiComponent.fill(g,x+5,y+3,x+9,y+8,color);
        }
        var font=MenuAppearance.font();
        font.draw(g,font.plainSubstrByWidth(getMessage().getString(),width-22),this.x+20,this.y+(height-8)/2,active?MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3):MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF6F7185));
    }
}

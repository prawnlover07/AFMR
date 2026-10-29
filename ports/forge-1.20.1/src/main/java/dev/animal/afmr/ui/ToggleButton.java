package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.function.BooleanSupplier;

/** A setting row: the box and its label share one toggle target. */
final class ToggleButton extends FlatButton {
    private final BooleanSupplier checked;
    ToggleButton(String label,int x,int y,int w,int h,float scale,BooleanSupplier checked,Runnable action){
        super(label,x,y,w,h,scale,action);this.checked=checked;
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
        if(isHoveredOrFocused())g.fill(getX(),getY(),(getX()+getWidth()),(getY()+getHeight()),MenuAppearance.background(MenuColor.HOVER,0x3B424D));
        int x=getX()+2,y=getY()+(height-12)/2;
        g.fill(x,y,x+12,y+12,MenuAppearance.color(MenuColor.BORDERS,0xFF627078));
        g.fill(x+1,y+1,x+11,y+11,MenuAppearance.background(MenuColor.BACKGROUND,0x17191D));
        if(checked.getAsBoolean()){
            int color=MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF);
            g.fill(x+3,y+5,x+5,y+8,color);g.fill(x+5,y+3,x+9,y+8,color);
        }
        var font=MenuAppearance.font();
        g.drawString(font,font.plainSubstrByWidth(getMessage().getString(),width-22),getX()+20,getY()+(height-8)/2,
            active?MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3):MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF6F7185),false);
    }
}

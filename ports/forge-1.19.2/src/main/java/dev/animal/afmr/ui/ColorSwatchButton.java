package dev.animal.afmr.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import java.util.function.IntSupplier;

/** A focusable, clickable RGB swatch; color is read live from its owning setting. */
final class ColorSwatchButton extends FlatButton {
    private final IntSupplier color;
    ColorSwatchButton(String name,int x,int y,int w,int h,IntSupplier color,Runnable action){
        super(name,x,y,w,h,1,action);this.color=color;
        var openColorPickerHelpText=Component.translatable("afmr.copy.colorswatch.open_color_picker",name);
        if(!openColorPickerHelpText.getString().isBlank())description(openColorPickerHelpText);
    }
    @Override public void renderButton(PoseStack g,int mx,int my,float delta){
        int rgb=color.getAsInt()&0xFFFFFF;
        int border=isHoveredOrFocused()?0xFFFFFFFF:0xFF808080;
        net.minecraft.client.gui.GuiComponent.fill(g,this.x,this.y,(this.x+width),(this.y+height),border);
        net.minecraft.client.gui.GuiComponent.fill(g,this.x+1,this.y+1,(this.x+width)-1,(this.y+height)-1,0xFF000000);
        net.minecraft.client.gui.GuiComponent.fill(g,this.x+2,this.y+2,(this.x+width)-2,(this.y+height)-2,0xFF000000|rgb);
    }
}

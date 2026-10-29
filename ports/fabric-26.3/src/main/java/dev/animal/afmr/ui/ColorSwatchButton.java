package dev.animal.afmr.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.function.IntSupplier;

/** A focusable, clickable RGB swatch; color is read live from its owning setting. */
final class ColorSwatchButton extends FlatButton {
    private final IntSupplier color;
    ColorSwatchButton(String name,int x,int y,int w,int h,IntSupplier color,Runnable action){
        super(name,x,y,w,h,1,action);this.color=color;
        var openColorPickerHelpText=Component.translatable("afmr.copy.colorswatch.open_color_picker",name);
        if(!openColorPickerHelpText.getString().isBlank())setTooltip(Tooltip.create(openColorPickerHelpText));
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        int rgb=color.getAsInt()&0xFFFFFF;
        int border=isHoveredOrFocused()?0xFFFFFFFF:0xFF808080;
        g.fill(getX(),getY(),getRight(),getBottom(),border);
        g.fill(getX()+1,getY()+1,getRight()-1,getBottom()-1,0xFF000000);
        g.fill(getX()+2,getY()+2,getRight()-2,getBottom()-2,0xFF000000|rgb);
    }
}

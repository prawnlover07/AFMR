package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import java.util.function.DoubleConsumer;

/** Menu opacity control using the selected menu font and palette. */
final class OpacitySlider extends AbstractSliderButton implements Described {
    private net.minecraft.network.chat.Component description;
    public net.minecraft.network.chat.Component description(){return description;}
    public void description(net.minecraft.network.chat.Component text){description=text;}

    private final String label;
    private final DoubleConsumer changed;
    OpacitySlider(String label,int x,int y,int width,double value,DoubleConsumer changed){
        super(x,y,width,20,Component.empty(),value);this.label=label;this.changed=changed;updateMessage();
    }
    @Override protected void updateMessage(){setMessage(Component.literal(label+": "+Math.round(value*100)+"%"));}
    @Override protected void applyValue(){changed.accept(value);}
    @Override public void renderButton(PoseStack g,int mx,int my,float dt){
        int x=this.x,y=this.y;
        net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+width,y+height,MenuAppearance.background(MenuColor.CONTROLS,0x2C3038));
        int handle=x+(int)(value*(width-8));
        net.minecraft.client.gui.GuiComponent.fill(g,handle,y,handle+8,y+height,isHoveredOrFocused()?MenuAppearance.background(MenuColor.HOVER,0x3B424D):MenuAppearance.background(MenuColor.SELECTION,0x304963));
        var font=MenuAppearance.font();
        font.draw(g,font.plainSubstrByWidth(getMessage().getString(),width-12),x+6,y+(height-8)/2,active?MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3):MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF6F7185));
    }
}

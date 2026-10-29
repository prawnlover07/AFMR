package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import com.mojang.blaze3d.vertex.PoseStack;

/** Small source markers remain independent of the editing selection. */
final class PlaybackIndicator {
    private PlaybackIndicator(){}
    static void draw(PoseStack g,int x,int y,boolean paused){
        int color=MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF);
        if(paused){net.minecraft.client.gui.GuiComponent.fill(g,x+1,y+1,x+3,y+9,color);net.minecraft.client.gui.GuiComponent.fill(g,x+6,y+1,x+8,y+9,color);}
        else{net.minecraft.client.gui.GuiComponent.fill(g,x+3,y,x+5,y+8,color);net.minecraft.client.gui.GuiComponent.fill(g,x+5,y,x+9,y+3,color);net.minecraft.client.gui.GuiComponent.fill(g,x,y+6,x+5,y+10,color);}
    }
}

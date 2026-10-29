package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphics;

/** Small source markers remain independent of the editing selection. */
final class PlaybackIndicator {
    private PlaybackIndicator(){}
    static void draw(GuiGraphics g,int x,int y,boolean paused){
        int color=MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF);
        if(paused){g.fill(x+1,y+1,x+3,y+9,color);g.fill(x+6,y+1,x+8,y+9,color);}
        else{g.fill(x+3,y,x+5,y+8,color);g.fill(x+5,y,x+9,y+3,color);g.fill(x,y+6,x+5,y+10,color);}
    }
}

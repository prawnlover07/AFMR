package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Font-independent transport symbols with explicit tooltip and narration labels. */
final class TransportButton extends FlatButton {
    enum Icon { PLAY, PAUSE, NEXT }
    private Icon icon;
    private final Component explanation;
    TransportButton(int x,int y,float scale,String help,Runnable action){
        super("",x,y,28,22,scale,action);explanation=Component.translatable(help);
    }
    void state(Icon next,String label){
        if(icon==next&&getMessage().getString().equals(label))return;
        icon=next;var text=Component.literal(label);setMessage(text);
        var tooltip=text.copy();if(!label.equals("Play now")&&!explanation.getString().isBlank())tooltip.append("\n").append(explanation);
        setTooltip(Tooltip.create(tooltip));
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(getX(),getY(),getRight(),getBottom(),MenuAppearance.background(isHoveredOrFocused()?MenuColor.HOVER:MenuColor.CONTROLS,isHoveredOrFocused()?0x3B424D:0x2C3038));
        int x=getX()+width/2,y=getY()+height/2;
        int color=MenuAppearance.color(active?MenuColor.TEXT:MenuColor.MUTED_TEXT,active?0xFFEBEEF3:0xFF6F7185);
        if(icon==Icon.PAUSE){g.fill(x-4,y-5,x-1,y+5,color);g.fill(x+2,y-5,x+5,y+5,color);}
        else{
            int start=icon==Icon.NEXT?x-5:x-3;
            for(int i=0;i<8;i++)g.fill(start+i,y-5+i/2,start+i+1,y+5-i/2,color);
            if(icon==Icon.NEXT)g.fill(x+4,y-5,x+6,y+5,color);
        }
    }
}

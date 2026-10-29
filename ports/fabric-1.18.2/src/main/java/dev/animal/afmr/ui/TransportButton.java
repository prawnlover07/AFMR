package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;

/** Font-independent transport symbols with explicit tooltip and narration labels. */
final class TransportButton extends FlatButton {
    enum Icon { PLAY, PAUSE, NEXT }
    private Icon icon;
    private final Component explanation;
    TransportButton(int x,int y,float scale,String help,Runnable action){
        super("",x,y,28,22,scale,action);explanation=new net.minecraft.network.chat.TranslatableComponent(help);
    }
    void state(Icon next,String label){
        if(icon==next&&getMessage().getString().equals(label))return;
        icon=next;var text=new net.minecraft.network.chat.TextComponent(label);setMessage(text);
        var tooltip=text.copy();if(!label.equals("Play now")&&!explanation.getString().isBlank())tooltip.append("\n").append(explanation);
        description(tooltip);
    }
    @Override public void renderButton(PoseStack g,int mx,int my,float dt){
        net.minecraft.client.gui.GuiComponent.fill(g,this.x,this.y,this.x+width,this.y+height,MenuAppearance.background(isHoveredOrFocused()?MenuColor.HOVER:MenuColor.CONTROLS,isHoveredOrFocused()?0x3B424D:0x2C3038));
        int x=this.x+width/2,y=this.y+height/2;
        int color=MenuAppearance.color(active?MenuColor.TEXT:MenuColor.MUTED_TEXT,active?0xFFEBEEF3:0xFF6F7185);
        if(icon==Icon.PAUSE){net.minecraft.client.gui.GuiComponent.fill(g,x-4,y-5,x-1,y+5,color);net.minecraft.client.gui.GuiComponent.fill(g,x+2,y-5,x+5,y+5,color);}
        else{
            int start=icon==Icon.NEXT?x-5:x-3;
            for(int i=0;i<8;i++)net.minecraft.client.gui.GuiComponent.fill(g,start+i,y-5+i/2,start+i+1,y+5-i/2,color);
            if(icon==Icon.NEXT)net.minecraft.client.gui.GuiComponent.fill(g,x+4,y-5,x+6,y+5,color);
        }
    }
}

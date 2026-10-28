package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Current-song title acts as a link, with hover/focus feedback and normal button input. */
final class NowPlayingLink extends FlatButton {
    NowPlayingLink(int x,int y,int width,float scale,Runnable action){super("",x,y,width,14,scale,action);}
    void label(String text){if(!getMessage().getString().equals(text))setMessage(net.minecraft.network.chat.Component.literal(text));}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        var font=MenuAppearance.font();String text=font.plainSubstrByWidth(getMessage().getString(),width);
        int color=MenuAppearance.color(active&&isHoveredOrFocused()?MenuColor.ACCENT:MenuColor.TEXT,0xFFEBEEF3);
        g.text(font,text,getX(),getY()+3,color,false);
        if(active&&isHoveredOrFocused())g.fill(getX(),getY()+12,getX()+font.width(text),getY()+13,color);
    }
}

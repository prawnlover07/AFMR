package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import com.mojang.blaze3d.vertex.PoseStack;

/** Current-song title acts as a link, with hover/focus feedback and normal button input. */
final class NowPlayingLink extends FlatButton {
    NowPlayingLink(int x,int y,int width,float scale,Runnable action){super("",x,y,width,14,scale,action);}
    void label(String text){if(!getMessage().getString().equals(text))setMessage(net.minecraft.network.chat.Component.literal(text));}
    @Override public void renderButton(PoseStack g,int mx,int my,float dt){
        var font=MenuAppearance.font();String text=font.plainSubstrByWidth(getMessage().getString(),width);
        int color=MenuAppearance.color(active&&isHoveredOrFocused()?MenuColor.ACCENT:MenuColor.TEXT,0xFFEBEEF3);
        font.draw(g,text,this.x,this.y+3,color);
        if(active&&isHoveredOrFocused())net.minecraft.client.gui.GuiComponent.fill(g,this.x,this.y+12,this.x+font.width(text),this.y+13,color);
    }
}

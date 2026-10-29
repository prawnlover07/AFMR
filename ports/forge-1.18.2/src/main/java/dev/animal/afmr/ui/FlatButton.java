package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
class FlatButton extends AbstractWidget implements Described {
    private Component description;
    public Component description(){return description;}
    public void description(Component text){description=text;}

    protected final Runnable action;
    boolean selected,opaque;
    private Integer labelTextColor;
    FlatButton(String label,int x,int y,int w,int h,float scale,Runnable action){super(x,y,w,h,new net.minecraft.network.chat.TextComponent(label));this.action=action;}
    void setLabelTextColor(Integer color){labelTextColor=color;}
    @Override public void renderButton(PoseStack g,int mx,int my,float dt){
        int background=selected?MenuAppearance.background(MenuColor.SELECTION,0x304963):isHoveredOrFocused()?MenuAppearance.background(MenuColor.HOVER,0x3B424D):MenuAppearance.background(MenuColor.CONTROLS,0x2C3038);
        net.minecraft.client.gui.GuiComponent.fill(g,this.x,this.y,this.x+width,this.y+height,opaque?background|0xFF000000:background);
        if(selected)net.minecraft.client.gui.GuiComponent.fill(g,this.x,this.y+height-2,this.x+width,this.y+height,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        var font=MenuAppearance.font();
        font.draw(g,font.plainSubstrByWidth(getMessage().getString(),width-8),this.x+4,this.y+(height-8)/2,active?(labelTextColor==null?MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3):labelTextColor):MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF6F7185));
    }
    @Override public void onClick(double x,double y){action.run();}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(isFocused()&&active&&(key==InputConstants.KEY_RETURN||key==InputConstants.KEY_SPACE)){playDownSound(Minecraft.getInstance().getSoundManager());action.run();return true;}return false;}
    @Override public void updateNarration(NarrationElementOutput out){defaultButtonNarrationText(out);}
}

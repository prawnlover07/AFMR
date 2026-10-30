package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
class FlatButton extends AbstractWidget {
    protected final Runnable action;boolean selected,opaque;
    private final WidgetTooltipHolder help=new WidgetTooltipHolder();
    private ScreenRectangle tooltipBounds;
    private final float scale;
    FlatButton(String label,int x,int y,int w,int h,float scale,Runnable action){
        super(x,y,w,h,Component.literal(label));this.action=action;
        this.scale=scale;
        tooltipBounds=new ScreenRectangle(Math.round(x*scale),Math.round(y*scale),Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));
        help.setDelay(java.time.Duration.ofMillis(350));
        if(MenuAppearance.font().width(label)>w-8)help.set(Tooltip.create(getMessage()));
    }
    @Override public void setTooltip(Tooltip tooltip){help.set(tooltip);}
    @Override public void setTooltipDelay(java.time.Duration delay){help.setDelay(delay);}
    @Override protected void extractTooltipForNextRenderPass(GuiGraphicsExtractor g,int mx,int my){
        int x=Math.round(getX()*scale),y=Math.round(getY()*scale),w=Math.max(1,Math.round(width*scale)),h=Math.max(1,Math.round(height*scale));
        if(tooltipBounds.left()!=x||tooltipBounds.top()!=y||tooltipBounds.width()!=w||tooltipBounds.height()!=h)
            tooltipBounds=new ScreenRectangle(x,y,w,h);
        // Deferred tooltips render after the workspace's virtual-canvas transform is popped.
        help.refreshTooltipForNextRenderPass(g,Math.round(mx*scale),Math.round(my*scale),isHovered(),isFocused(),tooltipBounds);
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        int background=selected?MenuAppearance.background(MenuColor.SELECTION,0x304963):isHoveredOrFocused()?MenuAppearance.background(MenuColor.HOVER,0x3B424D):MenuAppearance.background(MenuColor.CONTROLS,0x2C3038);
        g.fill(getX(),getY(),getX()+width,getY()+height,opaque?background|0xFF000000:background);
        if(selected)g.fill(getX(),getY()+height-2,getX()+width,getY()+height,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        var font=MenuAppearance.font();g.text(font,font.plainSubstrByWidth(getMessage().getString(),width-8),getX()+4,getY()+(height-8)/2,active?(labelTextColor==null?MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3):labelTextColor):MenuAppearance.color(MenuColor.MUTED_TEXT,MenuColor.DISABLED_TEXT),false);
    }
    @Override public void onClick(MouseButtonEvent e,boolean twice){action.run();}
    @Override public boolean keyPressed(KeyEvent e){if(isFocused()&&active&&(e.key()==InputConstants.KEY_RETURN||e.key()==InputConstants.KEY_SPACE)){playDownSound(Minecraft.getInstance().getSoundManager());action.run();return true;}return false;}
    @Override protected void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,getMessage());help.updateNarration(out);}
}

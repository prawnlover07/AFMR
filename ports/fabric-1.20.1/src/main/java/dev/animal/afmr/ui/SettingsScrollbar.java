package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.*;

import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Scrolls complete setting rows, keeping controls outside the viewport out of input/focus. */
final class SettingsScrollbar extends AbstractWidget {
    private final int maximum,rows;
    private final IntSupplier offset;
    private final IntConsumer change;
    private double grab;
    SettingsScrollbar(int x,int y,int height,int maximum,int rows,IntSupplier offset,IntConsumer change){
        super(x,y,8,height,Component.literal("Widget settings scroll"));
        this.maximum=maximum;this.rows=rows;this.offset=offset;this.change=change;
        visible=maximum>0;var widgetScrollbarHelpText=Component.translatable("afmr.copy.settingsscrollbar.widget_scrollbar");
        if(!widgetScrollbarHelpText.getString().isBlank())setTooltip(Tooltip.create(widgetScrollbarHelpText));
    }
    private int thumbHeight(){return Math.max(12,height*rows/(rows+maximum));}
    private int thumbY(){return getY()+(height-thumbHeight())*offset.getAsInt()/Math.max(1,maximum);}
    private void choose(double y){change.accept((int)Math.round((y-getY()-grab)*maximum/Math.max(1,height-thumbHeight())));}
    @Override public void onClick(double x,double y){
        int top=thumbY();grab=y>=top&&y<top+thumbHeight()?y-top:thumbHeight()/2d;choose(y);
    }
    @Override protected void onDrag(double x,double y,double dx,double dy){choose(y);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(!isFocused()||!active)return false;
        int target=switch(key){
            case InputConstants.KEY_UP->offset.getAsInt()-1;case InputConstants.KEY_DOWN->offset.getAsInt()+1;
            case InputConstants.KEY_PAGEUP->offset.getAsInt()-rows;case InputConstants.KEY_PAGEDOWN->offset.getAsInt()+rows;
            case InputConstants.KEY_HOME->0;case InputConstants.KEY_END->maximum;default->-1;
        };
        if(target==-1&&key!=InputConstants.KEY_UP&&key!=InputConstants.KEY_PAGEUP)return false;
        change.accept(target);return true;
    }
    @Override protected void renderWidget(GuiGraphics g,int x,int y,float delta){
        g.fill(getX(),getY(),(getX()+width),(getY()+height),MenuAppearance.background(MenuColor.CONTROLS,0x2C3038));
        int top=thumbY();g.fill(getX()+1,top,(getX()+width)-1,top+thumbHeight(),isFocused()?MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF):MenuAppearance.color(MenuColor.SCROLLBARS,0xFF698293));
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput out){
        out.add(NarratedElementType.TITLE,Component.literal("Widget settings, row "+(offset.getAsInt()+1)+" of "+(maximum+1)));
    }
}

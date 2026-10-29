package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

/** Viewport-rendered rows with independent scrolling; no button per song. */
final class DenseList<T> extends AbstractWidget {
    interface RowRenderer<T>{void draw(GuiGraphicsExtractor g,T item,int x,int y,int width,boolean hover);}
    interface Click<T>{void click(T item,MouseButtonEvent event,boolean twice);}
    static final int ROW=14;
    private List<T> items=List.of();
    private final RowRenderer<T> renderer;
    private final Click<T> click;
    private final Function<T,String> label;
    private int offset;
    private boolean dragging;
    DenseList(int x,int y,int w,int h,RowRenderer<T> renderer,Click<T> click,Function<T,String> label){super(x,y,w,h,Component.literal("Music list"));this.renderer=renderer;this.click=click;this.label=label;}
    void items(List<T> value){items=value;offset=Math.clamp(offset,0,maxOffset());}
    void top(){offset=0;}
    int offset(){return offset;}
    T itemAt(double x,double y){
        if(x<getX()||x>=getX()+width-8||y<getY()||y>=getBottom())return null;
        int index=offset+(int)(y-getY())/ROW;return index<items.size()?items.get(index):null;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        if(itemAt(event.x(),event.y())==null&&!(maxOffset()>0&&event.x()>=getX()+width-8&&isMouseOver(event.x(),event.y())))return false;
        return super.mouseClicked(event,twice);
    }
    void offset(int value){offset=Math.clamp(value,0,maxOffset());}
    void reveal(T item){int index=items.indexOf(item);if(index>=0){if(index<offset)offset=index;else if(index>=offset+visible())offset=index-visible()+1;}}
    private int visible(){return Math.max(1,height/ROW);}
    private int maxOffset(){return Math.max(0,items.size()-visible());}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        int even=MenuAppearance.background(MenuColor.BACKGROUND,0x1E1E25),odd=MenuAppearance.background(MenuColor.BACKGROUND,0x23242B);
        int end=Math.min(items.size(),offset+visible()+1);
        int emptyY=getY()+Math.min(items.size()-offset,visible()+1)*ROW;
        if(emptyY<getBottom())g.fill(getX(),emptyY,getX()+width-8,getBottom(),MenuAppearance.background(MenuColor.BACKGROUND,0x1C1B22));
        // Paint exact viewport geometry before the rounded text scissor can trim fractional edges.
        for(int i=offset;i<end;i++){
            int y=getY()+(i-offset)*ROW;g.fill(getX(),y,getX()+width-8,Math.min(y+ROW,getBottom()),(i%2==0)?even:odd);
        }
        g.enableScissor(getX(),getY(),getX()+width-8,getY()+height);
        for(int i=offset;i<end;i++){
            int y=getY()+(i-offset)*ROW;
            renderer.draw(g,items.get(i),getX(),y,width-8,mx>=getX()&&mx<getX()+width-8&&my>=y&&my<y+ROW);
        }
        g.disableScissor();g.fill(getX()+width-8,getY(),getX()+width,getY()+height,MenuAppearance.background(MenuColor.CONTROLS,0x15141B));
        if(maxOffset()>0){int thumb=Math.max(16,height*visible()/items.size()),y=getY()+(height-thumb)*offset/maxOffset();g.fill(getX()+width-6,y,getX()+width-1,y+thumb,MenuAppearance.color(MenuColor.SCROLLBARS,0xFF66778B));}
    }
    @Override public void onClick(MouseButtonEvent e,boolean twice){
        if(e.x()>=getX()+width-8){dragging=true;scrollTo(e.y());return;}
        int index=offset+(int)(e.y()-getY())/ROW;
        if(index>=0&&index<items.size()){setMessage(Component.literal(label.apply(items.get(index))));click.click(items.get(index),e,twice);}
    }
    private void scrollTo(double y){offset=(int)Math.clamp((y-getY())/height*maxOffset(),0,maxOffset());}
    @Override protected void onDrag(MouseButtonEvent e,double dx,double dy){if(dragging)scrollTo(e.y());}
    @Override public void onRelease(MouseButtonEvent e){dragging=false;}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){if(dy==0)return false;int steps=(int)Math.copySign(Math.max(1,Math.ceil(Math.abs(dy)*3)),dy);offset=Math.clamp(offset-steps,0,maxOffset());return true;}
    @Override protected void updateWidgetNarration(NarrationElementOutput output){output.add(NarratedElementType.TITLE,getMessage());}
}

package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

/** Viewport-rendered rows with independent scrolling; no button per song. */
final class DenseList<T> extends AbstractWidget {
    interface RowRenderer<T>{void draw(GuiGraphics g,T item,int x,int y,int width,boolean hover);}
    interface Click<T>{void click(T item,double clickX,double clickY,int button,boolean twice);}
    static final int ROW=14;
    private List<T> items=List.of();
    private final RowRenderer<T> renderer;
    private final Click<T> click;
    private final Function<T,String> label;
    private int offset;
    private boolean dragging;
    private int lastClick=-1;private long lastClickAt;
    DenseList(int x,int y,int w,int h,RowRenderer<T> renderer,Click<T> click,Function<T,String> label){super(x,y,w,h,Component.literal("Music list"));this.renderer=renderer;this.click=click;this.label=label;}
    void items(List<T> value){items=value;offset=Math.max(0,Math.min(offset,maxOffset()));}
    void top(){offset=0;}
    int offset(){return offset;}
    void offset(int value){offset=Math.max(0,Math.min(value,maxOffset()));}
    void reveal(T item){int index=items.indexOf(item);if(index>=0){if(index<offset)offset=index;else if(index>=offset+visible())offset=index-visible()+1;}}
    private int visible(){return Math.max(1,height/ROW);}
    private int maxOffset(){return Math.max(0,items.size()-visible());}
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
        int even=MenuAppearance.background(MenuColor.BACKGROUND,0x1B2023),odd=MenuAppearance.background(MenuColor.BACKGROUND,0x202629);
        int end=Math.min(items.size(),offset+visible()+1);
        int emptyY=getY()+Math.min(items.size()-offset,visible()+1)*ROW;
        if(emptyY<(getY()+getHeight()))g.fill(getX(),emptyY,getX()+width-8,(getY()+getHeight()),MenuAppearance.background(MenuColor.BACKGROUND,0x191D20));
        // Paint exact viewport geometry before the rounded text scissor can trim fractional edges.
        for(int i=offset;i<end;i++){
            int y=getY()+(i-offset)*ROW;g.fill(getX(),y,getX()+width-8,Math.min(y+ROW,getY()+height),(i%2==0)?even:odd);
        }
        var pose=g.pose().last().pose();
        g.enableScissor((int)Math.floor(getX()*pose.m00()+pose.m30()),(int)Math.floor(getY()*pose.m11()+pose.m31()),
            (int)Math.ceil((getX()+width-8)*pose.m00()+pose.m30()),(int)Math.ceil((getY()+height)*pose.m11()+pose.m31()));
        for(int i=offset;i<end;i++){
            int y=getY()+(i-offset)*ROW;
            renderer.draw(g,items.get(i),getX(),y,width-8,mx>=getX()&&mx<getX()+width-8&&my>=y&&my<y+ROW);
        }
        g.disableScissor();g.fill(getX()+width-8,getY(),getX()+width,getY()+height,MenuAppearance.background(MenuColor.CONTROLS,0x111619));
        if(maxOffset()>0){int thumb=Math.max(16,height*visible()/items.size()),y=getY()+(height-thumb)*offset/maxOffset();g.fill(getX()+width-6,y,getX()+width-1,y+thumb,MenuAppearance.color(MenuColor.SCROLLBARS,0xFF637B75));}
    }
    @Override public void onClick(double x,double y){
        if(x>=getX()+width-8){dragging=true;scrollTo(y);return;}
        int index=offset+(int)(y-getY())/ROW;
        if(index>=0&&index<items.size()){setMessage(Component.literal(label.apply(items.get(index))));long now=System.nanoTime();boolean twice=index==lastClick&&now-lastClickAt<250_000_000L;lastClick=index;lastClickAt=now;click.click(items.get(index),x,y,0,twice);}
    }
    private void scrollTo(double y){offset=(int)Math.max(0,Math.min((y-getY())/height*maxOffset(),maxOffset()));}
    @Override protected void onDrag(double x,double y,double dx,double dy){if(dragging)scrollTo(y);}
    @Override public void onRelease(double x,double y){dragging=false;}
    @Override public boolean mouseScrolled(double x,double y,double dy){if(dy==0)return false;int steps=(int)Math.copySign(Math.max(1,Math.ceil(Math.abs(dy)*3)),dy);offset=Math.max(0,Math.min(offset-steps,maxOffset()));return true;}
    @Override protected void updateWidgetNarration(NarrationElementOutput output){output.add(NarratedElementType.TITLE,getMessage());}
}

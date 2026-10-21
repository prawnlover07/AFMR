package dev.animal.afmr.ui;
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
    void reveal(T item){int index=items.indexOf(item);if(index>=0){if(index<offset)offset=index;else if(index>=offset+visible())offset=index-visible()+1;}}
    private int visible(){return Math.max(1,height/ROW);}
    private int maxOffset(){return Math.max(0,items.size()-visible());}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(getX(),getY(),getX()+width,getY()+height,0xFF191D20);
        g.enableScissor(getX(),getY(),getX()+width-8,getY()+height);
        for(int i=offset;i<Math.min(items.size(),offset+visible()+1);i++){
            int y=getY()+(i-offset)*ROW;g.fill(getX(),y,getX()+width-8,y+ROW,(i%2==0)?0xFF1B2023:0xFF202629);
            renderer.draw(g,items.get(i),getX(),y,width-8,mx>=getX()&&mx<getX()+width-8&&my>=y&&my<y+ROW);
        }
        g.disableScissor();g.fill(getX()+width-7,getY(),getX()+width,getY()+height,0xFF111619);
        if(maxOffset()>0){int thumb=Math.max(16,height*visible()/items.size()),y=getY()+(height-thumb)*offset/maxOffset();g.fill(getX()+width-6,y,getX()+width-1,y+thumb,0xFF637B75);}
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

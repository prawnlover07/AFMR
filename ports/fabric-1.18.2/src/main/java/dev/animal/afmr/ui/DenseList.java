package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

/** Viewport-rendered rows with independent scrolling; no button per song. */
final class DenseList<T> extends AbstractWidget implements Described {
    private net.minecraft.network.chat.Component description;
    public net.minecraft.network.chat.Component description(){return description;}
    public void description(net.minecraft.network.chat.Component text){description=text;}

    interface RowRenderer<T>{void draw(PoseStack g,T item,int x,int y,int width,boolean hover);}
    interface Click<T>{void click(T item,double clickX,double clickY,int button,boolean twice);}
    static final int ROW=14;
    private List<T> items=List.of();
    private final RowRenderer<T> renderer;
    private final Click<T> click;
    private final Function<T,String> label;
    private int offset;
    private boolean dragging;
    private int lastClick=-1;private long lastClickAt;
    DenseList(int x,int y,int w,int h,RowRenderer<T> renderer,Click<T> click,Function<T,String> label){super(x,y,w,h,new net.minecraft.network.chat.TextComponent("Music list"));this.renderer=renderer;this.click=click;this.label=label;}
    void items(List<T> value){items=value;offset=Math.max(0,Math.min(offset,maxOffset()));}
    void top(){offset=0;}
    int offset(){return offset;}
    T itemAt(double x,double y){
        if(x<this.x||x>=this.x+width-8||y<this.y||y>=this.y+height)return null;
        int index=offset+(int)(y-this.y)/ROW;return index<items.size()?items.get(index):null;
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(itemAt(x,y)==null&&!(maxOffset()>0&&x>=this.x+width-8&&isMouseOver(x,y)))return false;
        return super.mouseClicked(x,y,button);
    }
    void offset(int value){offset=Math.max(0,Math.min(value,maxOffset()));}
    void reveal(T item){int index=items.indexOf(item);if(index>=0){if(index<offset)offset=index;else if(index>=offset+visible())offset=index-visible()+1;}}
    private int visible(){return Math.max(1,height/ROW);}
    private int maxOffset(){return Math.max(0,items.size()-visible());}
    @Override public void renderButton(PoseStack g,int mx,int my,float dt){
        int even=MenuAppearance.background(MenuColor.BACKGROUND,0x1E1E25),odd=MenuAppearance.background(MenuColor.BACKGROUND,0x23242B);
        int end=Math.min(items.size(),offset+visible()+1);
        int emptyY=this.y+Math.min(items.size()-offset,visible()+1)*ROW;
        if(emptyY<(this.y+getHeight()))net.minecraft.client.gui.GuiComponent.fill(g,this.x,emptyY,this.x+width-8,(this.y+getHeight()),MenuAppearance.background(MenuColor.BACKGROUND,0x1C1B22));
        // Paint exact viewport geometry before the rounded text scissor can trim fractional edges.
        for(int i=offset;i<end;i++){
            int y=this.y+(i-offset)*ROW;net.minecraft.client.gui.GuiComponent.fill(g,this.x,y,this.x+width-8,Math.min(y+ROW,this.y+height),(i%2==0)?even:odd);
        }
        float renderScale=net.minecraft.client.Minecraft.getInstance().screen instanceof LibraryScreen screen?screen.scale:1;
        var window=net.minecraft.client.Minecraft.getInstance().getWindow();double pixels=window.getGuiScale();
        int clipX=(int)Math.floor(x*renderScale*pixels),clipY=(int)Math.floor(y*renderScale*pixels);
        int clipRight=(int)Math.ceil((x+width-8)*renderScale*pixels),clipBottom=(int)Math.ceil((y+height)*renderScale*pixels);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(clipX,window.getHeight()-clipBottom,clipRight-clipX,clipBottom-clipY);
        for(int i=offset;i<end;i++){
            int y=this.y+(i-offset)*ROW;
            renderer.draw(g,items.get(i),this.x,y,width-8,mx>=this.x&&mx<this.x+width-8&&my>=y&&my<y+ROW);
        }
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();net.minecraft.client.gui.GuiComponent.fill(g,this.x+width-8,this.y,this.x+width,this.y+height,MenuAppearance.background(MenuColor.CONTROLS,0x15141B));
        if(maxOffset()>0){int thumb=Math.max(16,height*visible()/items.size()),y=this.y+(height-thumb)*offset/maxOffset();net.minecraft.client.gui.GuiComponent.fill(g,this.x+width-6,y,this.x+width-1,y+thumb,MenuAppearance.color(MenuColor.SCROLLBARS,0xFF66778B));}
    }
    @Override public void onClick(double x,double y){
        if(x>=this.x+width-8){dragging=true;scrollTo(y);return;}
        int index=offset+(int)(y-this.y)/ROW;
        if(index>=0&&index<items.size()){setMessage(new net.minecraft.network.chat.TextComponent(label.apply(items.get(index))));long now=System.nanoTime();boolean twice=index==lastClick&&now-lastClickAt<250_000_000L;lastClick=index;lastClickAt=now;click.click(items.get(index),x,y,0,twice);}
    }
    private void scrollTo(double y){offset=(int)Math.max(0,Math.min((y-this.y)/height*maxOffset(),maxOffset()));}
    @Override protected void onDrag(double x,double y,double dx,double dy){if(dragging)scrollTo(y);}
    @Override public void onRelease(double x,double y){dragging=false;}
    @Override public boolean mouseScrolled(double x,double y,double dy){if(dy==0)return false;int steps=(int)Math.copySign(Math.max(1,Math.ceil(Math.abs(dy)*3)),dy);offset=Math.max(0,Math.min(offset-steps,maxOffset()));return true;}
    @Override public void updateNarration(NarrationElementOutput output){output.add(NarratedElementType.TITLE,getMessage());}
}

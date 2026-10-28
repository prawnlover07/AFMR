package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

/** One release commits a seek. A changed playback identity cancels an unfinished drag. */
final class SeekBar extends AbstractWidget {
    private final ClientRuntime runtime;
    private boolean dragging;
    private long sequence;
    private double target;
    private int displayed=-1,total=-1,clockWidth;
    private net.minecraft.client.gui.Font clockFont;
    private String clock="";
    SeekBar(ClientRuntime runtime,int x,int y,int width){super(x,y,width,18,Component.literal("Song position"));this.runtime=runtime;}
    private boolean available(){var engine=runtime.engine();var track=engine.current();return track!=null&&Double.isFinite(track.durationSeconds)&&track.durationSeconds>0&&engine.phase()==dev.animal.afmr.core.PlaybackEngine.Phase.PLAYING;}
    private int barWidth(){return Math.max(1,width-Math.max(94,clockWidth+8));}
    private void target(double x){target=Math.max(0,Math.min(1,(x-getX())/barWidth()))*runtime.engine().current().durationSeconds;}
    void cancel(){dragging=false;}
    boolean cancelDrag(){boolean wasDragging=dragging;cancel();return wasDragging;}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        var engine=runtime.engine();var track=engine.current();
        if(dragging&&(sequence!=engine.playbackSequence()||!available()))cancel();
        double duration=track==null?0:track.durationSeconds,position=dragging?target:engine.trackElapsed(runtime.now());
        int seconds=(int)Math.max(0,position),length=Double.isFinite(duration)?(int)Math.max(0,duration):0;
        if(seconds!=displayed||length!=total||clockFont!=MenuAppearance.font()){displayed=seconds;total=length;clock=time(seconds)+" / "+(length==0?"--:--":time(length));clockFont=MenuAppearance.font();clockWidth=clockFont.width(clock);}
        int color=MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF),cy=getY()+8,bw=barWidth();
        g.fill(getX(),cy-1,getX()+bw,cy+2,MenuAppearance.color(MenuColor.BORDERS,0xFF3B424C));
        int head=getX()+(int)(bw*(duration>0?Math.max(0,Math.min(1,position/duration)):0));
        g.fill(getX(),cy-1,head,cy+2,color);
        if(available()&&(isHoveredOrFocused()||dragging))g.fill(head-2,cy-4,head+2,cy+5,color);
        g.text(MenuAppearance.font(),clock,getX()+bw+8,getY()+4,MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3),false);
    }
    static String time(int seconds){return seconds/60+":"+(seconds%60<10?"0":"")+seconds%60;}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        if(event.buttonInfo().button()!=InputConstants.MOUSE_BUTTON_LEFT||!available()||event.x()<getX()||event.x()>getX()+barWidth()||!isMouseOver(event.x(),event.y()))return false;
        sequence=runtime.engine().playbackSequence();dragging=true;target(event.x());return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(!dragging)return false;if(sequence!=runtime.engine().playbackSequence()||!available()){cancel();return true;}target(event.x());return true;}
    @Override public boolean mouseReleased(MouseButtonEvent event){
        if(!dragging)return false;dragging=false;
        if(sequence==runtime.engine().playbackSequence()&&available()){target(event.x());runtime.engine().seek(target,runtime.now());}return true;
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(event.key()==InputConstants.KEY_ESCAPE&&dragging){cancel();return true;}
        if(!available()||!isFocused())return false;
        int key=event.key();double position=runtime.engine().trackElapsed(runtime.now());
        if(key==InputConstants.KEY_LEFT||key==InputConstants.KEY_RIGHT)return runtime.engine().seek(position+(key==InputConstants.KEY_RIGHT?5:-5),runtime.now());
        if(key==InputConstants.KEY_HOME)return runtime.engine().seek(0,runtime.now());return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,Component.literal("Song position "+clock));}
}

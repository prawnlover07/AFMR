package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;

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
    SeekBar(ClientRuntime runtime,int x,int y,int width){super(x,y,width,18,new net.minecraft.network.chat.TextComponent("Song position"));this.runtime=runtime;}
    private boolean available(){var engine=runtime.engine();var track=engine.current();return track!=null&&Double.isFinite(track.durationSeconds)&&track.durationSeconds>0&&engine.phase()==dev.animal.afmr.core.PlaybackEngine.Phase.PLAYING;}
    private int barWidth(){return Math.max(1,width-Math.max(94,clockWidth+8));}
    private void target(double x){target=Math.max(0,Math.min(1,(x-this.x)/barWidth()))*runtime.engine().current().durationSeconds;}
    void cancel(){dragging=false;}
    boolean cancelDrag(){boolean wasDragging=dragging;cancel();return wasDragging;}
    @Override public void renderButton(PoseStack g,int mx,int my,float dt){
        var engine=runtime.engine();var track=engine.current();
        if(dragging&&(sequence!=engine.playbackSequence()||!available()))cancel();
        double duration=track==null?0:track.durationSeconds,position=dragging?target:engine.trackElapsed(runtime.now());
        int seconds=(int)Math.max(0,position),length=Double.isFinite(duration)?(int)Math.max(0,duration):0;
        if(seconds!=displayed||length!=total||clockFont!=MenuAppearance.font()){displayed=seconds;total=length;clock=time(seconds)+" / "+(length==0?"--:--":time(length));clockFont=MenuAppearance.font();clockWidth=clockFont.width(clock);}
        int color=MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF),cy=this.y+8,bw=barWidth();
        net.minecraft.client.gui.GuiComponent.fill(g,this.x,cy-1,this.x+bw,cy+2,MenuAppearance.color(MenuColor.BORDERS,0xFF3B424C));
        int head=this.x+(int)(bw*(duration>0?Math.max(0,Math.min(1,position/duration)):0));
        net.minecraft.client.gui.GuiComponent.fill(g,this.x,cy-1,head,cy+2,color);
        if(available()&&(isHoveredOrFocused()||dragging))net.minecraft.client.gui.GuiComponent.fill(g,head-2,cy-4,head+2,cy+5,color);
        MenuAppearance.font().draw(g,clock,this.x+bw+8,this.y+4,MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3));
    }
    static String time(int seconds){return seconds/60+":"+(seconds%60<10?"0":"")+seconds%60;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button!=InputConstants.MOUSE_BUTTON_LEFT||!available()||x<this.x||x>this.x+barWidth()||!isMouseOver(x,y))return false;
        sequence=runtime.engine().playbackSequence();dragging=true;target(x);return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(!dragging)return false;if(sequence!=runtime.engine().playbackSequence()||!available()){cancel();return true;}target(x);return true;}
    @Override public boolean mouseReleased(double x,double y,int button){
        if(!dragging)return false;dragging=false;
        if(sequence==runtime.engine().playbackSequence()&&available()){target(x);runtime.engine().seek(target,runtime.now());}return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==InputConstants.KEY_ESCAPE&&dragging){cancel();return true;}
        if(!available()||!isFocused())return false;
        double position=runtime.engine().trackElapsed(runtime.now());
        if(key==InputConstants.KEY_LEFT||key==InputConstants.KEY_RIGHT)return runtime.engine().seek(position+(key==InputConstants.KEY_RIGHT?5:-5),runtime.now());
        if(key==InputConstants.KEY_HOME)return runtime.engine().seek(0,runtime.now());return false;
    }
    @Override public void updateNarration(NarrationElementOutput out){out.add(NarratedElementType.TITLE,new net.minecraft.network.chat.TextComponent("Song position "+clock));}
}

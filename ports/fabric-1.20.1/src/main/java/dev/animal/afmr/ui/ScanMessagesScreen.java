package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

final class ScanMessagesScreen extends MusicScreen {
    private String guidance;
    private final List<String> messages;
    private final Map<String,String> labels=new HashMap<>();
    private final Map<String,List<FormattedCharSequence>> tooltips=new HashMap<>();
    private int mouseX,mouseY;
    ScanMessagesScreen(Screen parent,ClientRuntime runtime,List<String> messages){super("Scan messages",parent,runtime);this.messages=messages;}
    @Override protected void init(){
        super.init();guidance=fit(Component.translatable("afmr.copy.scanmessages.guidance").getString(),contentWidth);labels.clear();tooltips.clear();
        for(String message:messages){labels.put(message,fit(message,contentWidth-18));tooltips.put(message,font.split(Component.literal(message),Math.max(100,Math.min(400,contentWidth))));}
        var list=addRenderableWidget(new DenseList<String>(left,55,contentWidth,Math.max(28,height-112),
            (g,message,x,y,w,hover)->{
                g.drawString(font,labels.get(message),x+4,y+3,0xFFFFB0A7,false);
                if(hover)g.renderTooltip(font,tooltips.get(message),mouseX,mouseY);
            },(message,clickX,clickY,button,twice)->{},message->message));list.items(messages);
        button("Back",left,height-29,contentWidth,this::onClose);
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        mouseX=x;mouseY=y;g.drawString(font,guidance,left,height-48,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3));
    }
}

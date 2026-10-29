package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import com.mojang.blaze3d.vertex.PoseStack;
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
        for(String message:messages){labels.put(message,fit(message,contentWidth-18));tooltips.put(message,menuFont.split(Component.literal(message),Math.max(100,Math.min(400,contentWidth))));}
        var list=addRenderableWidget(new DenseList<String>(left,55,contentWidth,Math.max(28,bodyBottom()-112),
            (g,message,x,y,w,hover)->{
                menuFont.draw(g,labels.get(message),x+4,y+3,0xFFFFB0A7);
                if(hover)net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,tooltips.get(message),mouseX,mouseY);
            },(message,clickX,clickY,button,twice)->{},message->message));list.items(messages);
        button("Back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        mouseX=x;mouseY=y;menuFont.drawShadow(g,guidance,left,bodyBottom()-48,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
    }
}

package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaybackInspector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

final class InspectorDetailsScreen extends MusicScreen {
    private final PlaybackInspector.Entry entry;
    InspectorDetailsScreen(Screen parent,ClientRuntime runtime,PlaybackInspector.Entry entry){super("Playlist inspection snapshot",parent,runtime);this.entry=entry;}
    @Override protected void init(){
        super.init();var list=addRenderableWidget(new DenseList<String>(left,47,contentWidth,Math.max(28,bodyBottom()-86),
            (g,line,x,y,w,hover)->{g.drawString(menuFont,fit(line,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));if(hover)g.renderTooltip(menuFont,menuFont.split(net.minecraft.network.chat.Component.literal(line),Math.min(450,contentWidth)),(int)mouseX,(int)mouseY);},
            (line,clickX,clickY,button,twice)->{},s->s));
        var lines=new java.util.ArrayList<>(entry.details());var p=runtime.library().playlist(entry.id());
        if(p!=null)lines.addAll(PlaybackInspector.songCounts(runtime.library(),p,runtime.applicableVanilla(),runtime.engine().failedTracks()));list.items(lines);
        button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    private double mouseX,mouseY;
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){mouseX=x;mouseY=y;}
}

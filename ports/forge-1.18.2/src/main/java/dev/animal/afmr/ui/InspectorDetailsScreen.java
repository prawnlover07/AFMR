package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaybackInspector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

final class InspectorDetailsScreen extends MusicScreen {
    private final PlaybackInspector.Entry entry;
    InspectorDetailsScreen(Screen parent,ClientRuntime runtime,PlaybackInspector.Entry entry){super("Playlist inspection snapshot",parent,runtime);this.entry=entry;}
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();var list=addRenderableWidget(new DenseList<String>(left,47,contentWidth,Math.max(28,bodyBottom()-86),
            (g,line,x,y,w,hover)->{menuFont.drawShadow(g,fit(line,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));if(hover)net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,menuFont.split(new net.minecraft.network.chat.TextComponent(line),Math.min(450,contentWidth)),(int)mouseX,(int)mouseY);},
            (line,clickX,clickY,button,twice)->{},s->s));
        var lines=new java.util.ArrayList<>(entry.details());var p=runtime.library().playlist(entry.id());
        if(p!=null)lines.addAll(PlaybackInspector.songCounts(runtime.library(),p,runtime.applicableVanilla(),runtime.engine().failedTracks()));list.items(lines);
        button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    private double mouseX,mouseY;
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){mouseX=x;mouseY=y;}
}

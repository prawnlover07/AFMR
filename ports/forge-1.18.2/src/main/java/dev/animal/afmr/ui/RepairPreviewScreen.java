package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.LibraryRepair;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

final class RepairPreviewScreen extends MusicScreen {
    private String guidance;
    private final LibraryRepair.Plan plan;
    private final Map<LibraryRepair.Move,String> labels=new HashMap<>();
    private LibraryRepair.Move tooltipMove;
    private List<FormattedCharSequence> tooltip=List.of();
    private String statusText="";
    private List<FormattedCharSequence> statusTooltip=List.of();
    private int mouseX,mouseY;
    RepairPreviewScreen(Screen parent,ClientRuntime runtime,LibraryRepair.Plan plan){super("Review library repair",parent,runtime);this.plan=plan;}
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();guidance=fit(plan.merges().isEmpty()?new net.minecraft.network.chat.TranslatableComponent("afmr.copy.repairpreview.guidance").getString()
            :new net.minecraft.network.chat.TranslatableComponent("afmr.copy.repairpreview.details").getString(),contentWidth);labels.clear();tooltipMove=null;
        var list=addRenderableWidget(new DenseList<LibraryRepair.Move>(left,99,contentWidth,Math.max(28,bodyBottom()-180),
            (g,move,x,y,w,hover)->{
                menuFont.draw(g,labels.get(move),x+4,y+3,move.found()?MenuAppearance.color(MenuColor.ACCENT,0xFFBCCBFF):0xFFFFB0A7);
                if(hover){
                    if(tooltipMove!=move){tooltipMove=move;tooltip=menuFont.split(new net.minecraft.network.chat.TextComponent(move.before()+"\n→ "+move.replacement().path),Math.max(100,Math.min(400,contentWidth)));}
                    net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,tooltip,mouseX,mouseY);
                }
            },
            (move,clickX,clickY,button,twice)->{},move->move.before()+" -> "+move.replacement().path));
        for(var move:plan.moves()){
            labels.put(move,fit((move.found()?"Located: ":"Still missing: ")+move.replacement().title+" | "+move.replacement().path,contentWidth-18));
        }
        list.items(plan.moves());
        int half=(contentWidth-6)/2;
        button("Cancel",left,bodyBottom()-29,half,this::onClose);
        button("Apply repair",left+half+6,bodyBottom()-29,half,()->{if(runtime.applyRepair(plan))minecraft.setScreen(parent);});
    }
    @Override public void onClose(){runtime.cancelRepair(plan);super.onClose();}
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        mouseX=x;mouseY=y;
        menuFont.drawShadow(g,fit(plan.found()+" songs located | "+plan.missing()+" still missing | "+plan.merges().size()+" duplicates merged",contentWidth),left,49,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        menuFont.drawShadow(g,fit("New location: "+plan.request().replacement(),contentWidth),left,65,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        menuFont.drawShadow(g,guidance,left,81,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        menuFont.drawShadow(g,fit(runtime.repairStatus(),contentWidth),left,bodyBottom()-64,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        if(x>=left&&x<left+contentWidth&&y>=bodyBottom()-66&&y<bodyBottom()-50){
            if(!statusText.equals(runtime.repairStatus())){statusText=runtime.repairStatus();statusTooltip=menuFont.split(new net.minecraft.network.chat.TextComponent(statusText),Math.max(100,Math.min(400,contentWidth)));}
            net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,statusTooltip,x,y);
        }
    }
}

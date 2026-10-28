package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.LibraryRepair;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        super.init();guidance=fit(plan.merges().isEmpty()?Component.translatable("afmr.copy.repairpreview.guidance").getString()
            :Component.translatable("afmr.copy.repairpreview.details").getString(),contentWidth);labels.clear();tooltipMove=null;
        var list=addRenderableWidget(new DenseList<LibraryRepair.Move>(left,99,contentWidth,Math.max(28,bodyBottom()-180),
            (g,move,x,y,w,hover)->{
                g.text(menuFont,labels.get(move),x+4,y+3,move.found()?MenuAppearance.color(MenuColor.ACCENT,0xFFBCCBFF):0xFFFFB0A7,false);
                if(hover){
                    if(tooltipMove!=move){tooltipMove=move;tooltip=menuFont.split(Component.literal(move.before()+"\n→ "+move.replacement().path),Math.max(100,Math.min(400,contentWidth)));}
                    g.setTooltipForNextFrame(menuFont,tooltip,mouseX,mouseY);
                }
            },
            (move,event,twice)->{},move->move.before()+" -> "+move.replacement().path));
        for(var move:plan.moves()){
            labels.put(move,fit((move.found()?"Located: ":"Still missing: ")+move.replacement().title+" | "+move.replacement().path,contentWidth-18));
        }
        list.items(plan.moves());
        int half=(contentWidth-6)/2;
        button("Cancel",left,bodyBottom()-29,half,this::onClose);
        button("Apply repair",left+half+6,bodyBottom()-29,half,()->{if(runtime.applyRepair(plan))minecraft.gui.setScreen(parent);});
    }
    @Override public void onClose(){runtime.cancelRepair(plan);super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        mouseX=x;mouseY=y;
        g.text(menuFont,fit(plan.found()+" songs located | "+plan.missing()+" still missing | "+plan.merges().size()+" duplicates merged",contentWidth),left,49,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        g.text(menuFont,fit("New location: "+plan.request().replacement(),contentWidth),left,65,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        g.text(menuFont,guidance,left,81,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        g.text(menuFont,fit(runtime.repairStatus(),contentWidth),left,bodyBottom()-64,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        if(x>=left&&x<left+contentWidth&&y>=bodyBottom()-66&&y<bodyBottom()-50){
            if(!statusText.equals(runtime.repairStatus())){statusText=runtime.repairStatus();statusTooltip=menuFont.split(Component.literal(statusText),Math.max(100,Math.min(400,contentWidth)));}
            g.setTooltipForNextFrame(menuFont,statusTooltip,x,y);
        }
    }
}

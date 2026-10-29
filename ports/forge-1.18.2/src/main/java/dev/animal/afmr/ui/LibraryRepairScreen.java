package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.LibraryHealth;
import dev.animal.afmr.library.LibraryHealth.Issue;
import dev.animal.afmr.library.LibraryRepair;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

/** Cached report rows; rendering never reads the filesystem or walks the music catalog. */
final class LibraryRepairScreen extends MusicScreen {
    private String emptyGuidance;
    private LibraryHealth.Report report;
    private LibraryRepair.Plan shownPlan;
    private DenseList<Issue> list;
    private Issue selected;
    private String query="";
    private MenuField search;
    private FlatButton locate,check,review;
    private int mouseX,mouseY;
    private final Map<Issue,String> labels=new HashMap<>();
    private Issue tooltipIssue;
    private List<FormattedCharSequence> tooltip=List.of();
    private String statusText="";
    private List<FormattedCharSequence> statusTooltip=List.of();
    LibraryRepairScreen(Screen parent,ClientRuntime runtime){super("Library repair / missing sources",parent,runtime);}
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();emptyGuidance=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.libraryrepair.run_check_again_or_rescan_all_to_find_missing_folders_and_songs").getString();report=runtime.sourceHealth();int third=(contentWidth-8)/3;
        check=button("Check again",left,48,third,()->{
            if(runtime.repairSearchActive())runtime.cancelRepairSearch();else runtime.checkLibrary();
        });
        locate=button("Locate selected...",left+third+4,48,third,this::locate);
        review=button("Review repair",left+2*(third+4),48,third,()->{
            var plan=runtime.pendingRepair();if(plan!=null){shownPlan=plan;minecraft.setScreen(new RepairPreviewScreen(this,runtime,plan));}
            else if(report!=null&&!report.scanErrors().isEmpty())minecraft.setScreen(new ScanMessagesScreen(this,runtime,report.scanErrors()));
        });
        search=edit(query,"Filter missing paths / titles",left,74,contentWidth,value->{query=value;filter();});
        list=addRenderableWidget(new DenseList<>(left,101,contentWidth,Math.max(28,bodyBottom()-171),this::drawIssue,
            (issue,clickX,clickY,button,twice)->{selected=issue;controls();if(twice)locate();},issue->issue.path()+": "+issue.message()));
        cache();filter();controls();
        button("Back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
    private void cache(){
        labels.clear();tooltipIssue=null;
        if(report==null)return;
        for(var issue:report.issues()){
            String folder=issue.path();
            if(issue.kind()==LibraryHealth.Kind.FOLDER)try{
                var name=java.nio.file.Path.of(folder).getFileName();if(name!=null)folder=name+" | "+folder;
            }catch(java.nio.file.InvalidPathException ignored){}
            labels.put(issue,fit(issue.kind()==LibraryHealth.Kind.FOLDER?"Folder: "+folder:"Song: "+issue.title()+" | "+issue.path(),contentWidth-18));
        }
    }
    private void filter(){
        if(list==null)return;String q=query.toLowerCase(Locale.ROOT);
        list.items(report==null?List.of():report.issues().stream().filter(i->q.isBlank()||(i.path()+" "+i.title()+" "+i.message()).toLowerCase(Locale.ROOT).contains(q)).toList());
    }
    private void controls(){
        boolean free=!runtime.scanning()&&runtime.pendingRepair()==null;
        boolean searching=runtime.repairSearchActive(),cancelling=runtime.repairSearchCancelling();
        check.setMessage(new net.minecraft.network.chat.TextComponent(cancelling?"Cancelling...":searching?"Cancel search":"Check again"));
        check.active=searching?!cancelling:free;locate.active=free&&selected!=null;review.active=runtime.pendingRepair()!=null||report!=null&&!report.scanErrors().isEmpty();
        review.setMessage(new net.minecraft.network.chat.TextComponent(runtime.pendingRepair()!=null?"Review repair":"Scan messages"));
    }
    private void locate(){
        if(selected==null||runtime.scanning()||runtime.pendingRepair()!=null)return;
        NativePicker.open(runtime,selected.kind()==LibraryHealth.Kind.FOLDER?NativePicker.Kind.REPAIR_FOLDER:NativePicker.Kind.REPAIR_FILE,selected.id());
    }
    private void drawIssue(PoseStack g,Issue issue,int x,int y,int w,boolean hover){
        if(issue.equals(selected))net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
        menuFont.draw(g,labels.get(issue),x+4,y+3,0xFFFFB0A7);
        if(hover){
            if(tooltipIssue!=issue){tooltipIssue=issue;tooltip=menuFont.split(new net.minecraft.network.chat.TextComponent(issue.title()+"\n"+issue.path()+"\n"+issue.message()),Math.max(100,Math.min(400,contentWidth)));}
            net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,tooltip,mouseX,mouseY);
        }
    }
    @Override public void tick(){
        if(report!=runtime.sourceHealth()){selected=null;init(minecraft,width,height);}
        controls();
        var plan=runtime.pendingRepair();
        if(plan!=null&&plan!=shownPlan){shownPlan=plan;minecraft.setScreen(new RepairPreviewScreen(this,runtime,plan));}
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        mouseX=x;mouseY=y;
        if(runtime.scanning())ScanProgressView.draw(g,menuFont,runtime.scanProgress(),left,bodyBottom()-66,contentWidth);
        else{
            String text=report==null?emptyGuidance:report.issues().size()+" missing/unavailable | "+report.scanErrors().size()+" scan messages";
            menuFont.drawShadow(g,fit(text,contentWidth),left,bodyBottom()-65,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
            menuFont.drawShadow(g,fit(runtime.repairStatus(),contentWidth),left,bodyBottom()-48,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
            if(x>=left&&x<left+contentWidth&&y>=bodyBottom()-50&&y<bodyBottom()-36){
                if(!statusText.equals(runtime.repairStatus())){statusText=runtime.repairStatus();statusTooltip=menuFont.split(new net.minecraft.network.chat.TextComponent(statusText),Math.max(100,Math.min(400,contentWidth)));}
                net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,statusTooltip,x,y);
            }
        }
    }
}

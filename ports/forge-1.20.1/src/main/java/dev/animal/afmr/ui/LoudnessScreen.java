package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.LoudnessScan;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import java.util.Locale;

/** The normalization action starts the job immediately; this screen provides progress and its per-track report. */
final class LoudnessScreen extends MusicScreen {
    private final String playlistId;
    private LoudnessScan.Report report;
    private ResultRow selected;
    private String summary="";
    private boolean running;
    private record ResultRow(LoudnessScan.Row row,String source,String gain,String detail) {}
    LoudnessScreen(Screen parent,ClientRuntime runtime,String playlistId){super("Playlist normalization",parent,runtime);this.playlistId=playlistId;}
    private boolean running(){return runtime.loudnessJob()!=null&&runtime.loudnessJob().report()==null;}
    private LoudnessScan.Report report(){return runtime.loudnessJob()==null?runtime.lastLoudnessReport():runtime.loudnessJob().report();}
    @Override protected void init(){
        super.init();running=running();report=report();
        if(report!=null){
            summary=(report.cancelled()?"Cancelled. ":"Complete. ")+report.rows().size()+" / "+report.total()+" processed; "+report.count(LoudnessScan.Source.FAILED)+" issues.";
            var list=addRenderableWidget(new DenseList<ResultRow>(left,108,contentWidth,Math.max(28,bodyBottom()-183),this::drawRow,(row,clickX,clickY,button,twice)->selected=row,r->r.row().track().title()));
            list.items(report.rows().stream().map(LoudnessScreen::resultRow).toList());
        }
        button(running?"Cancel":"Normalize again",left,bodyBottom()-29,Math.min(150,contentWidth/2-6),()->{
            if(running){runtime.loudnessJob().cancel();rebuildWidgets();}
            else{selected=null;runtime.normalizePlaylist(playlistId);rebuildWidgets();}
        }).active=running?!runtime.loudnessJob().cancelled():runtime.library().playlist(playlistId)!=null;
        button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override public void tick(){if(running!=running()||report!=report())rebuildWidgets();}
    private static String db(double value){return String.format(Locale.ROOT,"%+.2f dB",value==0?0:value);}
    private static ResultRow resultRow(LoudnessScan.Row row){
        String source=switch(row.source()){case SCANNED->"Scanned";case CACHED->"Cached";case TAGS->"Existing tag";case FAILED->"Failed";};
        String detail=row.source()==LoudnessScan.Source.FAILED?row.detail():"Requested "+db(row.requestedDb())+"; applied "+db(row.appliedDb())
            +(row.limited()?" (peak protection)":"")+(row.detail().isBlank()?"":". "+row.detail());
        return new ResultRow(row,source+(row.limited()?" / limited":""),row.source()==LoudnessScan.Source.FAILED?"--":db(row.appliedDb()),detail);
    }
    private void drawRow(GuiGraphics g,ResultRow row,int x,int y,int w,boolean hover){
        boolean active=selected!=null&&row.row()==selected.row();
        if(active||hover)g.fill(x,y,x+w,y+14,active?MenuAppearance.color(MenuColor.SELECTION,0xFF304963):MenuAppearance.color(MenuColor.HOVER,0xFF2C303E));
        int name=w/2,adjustment=w-88;
        g.drawString(menuFont,fit(row.row().track().title(),name-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
        g.drawString(menuFont,fit(row.source(),adjustment-name-8),x+name,y+3,row.row().source()==LoudnessScan.Source.FAILED?0xFFFFA0A0:MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA1B4BC));
        g.drawString(menuFont,row.gain(),x+adjustment,y+3,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        var job=runtime.loudnessJob();String playlist=job!=null?job.playlist():report!=null?report.playlist():"";
        g.drawString(menuFont,fit(playlist.isBlank()?runtime.status():playlist,contentWidth),left,48,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        if(running){
            String state=job.cancelled()?"Cancelling...":"Analyzing audio...";
            g.drawString(menuFont,fit(state,contentWidth),left,85,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
            g.drawString(menuFont,job.completed()+" / "+job.total()+" processed",left,102,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA1B4BC));
            int barWidth=(int)(contentWidth*(job.total()==0?0:(double)job.completed()/job.total()));
            g.fill(left,119,left+contentWidth,125,MenuAppearance.color(MenuColor.CONTROLS,0xFF444E4D));g.fill(left,119,left+barWidth,125,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
            for(int lane=0;lane<job.lanes();lane++){
                var progress=job.progress(lane);if(progress!=null)
                    g.drawString(menuFont,fit(progress.title()+" / "+(int)progress.seconds()+" s analyzed",contentWidth),left,137+lane*16,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA1B4BC));
            }
        }else if(report!=null){
            g.drawString(menuFont,fit(summary,contentWidth),left,81,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
            g.drawString(menuFont,"Track",left+4,98,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFADC0C8));g.drawString(menuFont,"Source",left+(contentWidth-8)/2,98,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFADC0C8));
            g.drawString(menuFont,"Applied gain",left+contentWidth-96,98,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFADC0C8));
        }
        String detail="";
        if(selected!=null)detail=selected.detail();
        g.drawString(menuFont,fit(detail,contentWidth),left,bodyBottom()-65,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA1B4BC));
    }
}

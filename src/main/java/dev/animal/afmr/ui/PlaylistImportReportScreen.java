package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.PlaylistImportReport;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

final class PlaylistImportReportScreen extends MusicScreen {
    private final PlaylistImportReport report;
    private final List<String> lines=new ArrayList<>();
    private List<List<FormattedCharSequence>> tooltips=List.of();
    private int page;
    PlaylistImportReportScreen(Screen parent,ClientRuntime runtime,PlaylistImportReport report){
        super("Playlist import report",parent,runtime);this.report=report;
        for(String source:report.sources())lines.add("Playlist: "+source);
        lines.addAll(report.details());
        if(lines.isEmpty())lines.add("No issues reported.");
        if(report.details().size()>=100)lines.add("Details limited to the first 100 messages; skipped count includes all entries.");
    }
    @Override protected void init(){
        super.init();int size=pageSize(112,72);
        tooltips=lines.stream().map(line->font.split(Component.literal(line),Math.max(80,Math.min(300,contentWidth)))).toList();
        pages(page,size,lines.size(),height-53,next->{page=next;rebuildWidgets();});
        button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit("Destination: "+report.destination(),contentWidth),left,48,0xFF8DE0BE);
        g.text(font,fit(report.addedToPlaylist()+" added | "+report.alreadyAssigned()+" already assigned | "+report.newTracks()+" new library tracks",contentWidth),left,64,0xFFD9E8E0);
        g.text(font,fit(report.skipped()+" skipped | "+report.duplicates()+" duplicate entries | "+report.reused()+" reused metadata",contentWidth),left,80,0xFF8FA7A0);
        g.text(font,fit("Hover a detail to read its full message.",contentWidth),left,96,0xFF8FA7A0);
        int size=pageSize(112,72);
        for(int i=page*size;i<Math.min(lines.size(),(page+1)*size);i++){
            int rowY=112+(i-page*size)*22;
            g.text(font,fit(lines.get(i),contentWidth),left,rowY,0xFFB4C9BF);
            if(x>=left&&x<=left+contentWidth&&y>=rowY&&y<rowY+18)
                g.setTooltipForNextFrame(font,tooltips.get(i),x,y);
        }
    }
}

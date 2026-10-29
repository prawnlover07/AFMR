package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.PlaylistImportReport;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

final class PlaylistImportReportScreen extends MusicScreen {
    private String guidance;
    private final PlaylistImportReport report;
    private final List<String> lines=new ArrayList<>();
    private List<List<FormattedCharSequence>> tooltips=List.of();
    private int page;
    PlaylistImportReportScreen(Screen parent,ClientRuntime runtime,PlaylistImportReport report){
        super("Playlist import report",parent,runtime);this.report=report;
        for(String source:report.sources())lines.add("Playlist: "+source);
        lines.addAll(report.details());
        if(lines.isEmpty())lines.add("No issues reported.");
        if(report.details().size()>=100)lines.add(Component.translatable("afmr.copy.playlistimportreport.guidance").getString());
    }
    @Override protected void init(){
        super.init();guidance=fit(Component.translatable("afmr.copy.playlistimportreport.hover_a_detail_to_read_its_full_message").getString(),contentWidth);int size=pageSize(112,72);
        tooltips=lines.stream().map(line->menuFont.split(Component.literal(line),Math.max(80,Math.min(300,contentWidth)))).toList();
        pages(page,size,lines.size(),bodyBottom()-53,next->{page=next;rebuildWidgets();});
        button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        menuFont.drawShadow(g,fit("Destination: "+report.destination(),contentWidth),left,48,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        menuFont.drawShadow(g,fit(report.addedToPlaylist()+" added | "+report.alreadyAssigned()+" already assigned | "+report.newTracks()+" new library tracks",contentWidth),left,64,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        menuFont.drawShadow(g,fit(report.skipped()+" skipped | "+report.duplicates()+" duplicate entries | "+report.reused()+" reused metadata",contentWidth),left,80,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF));
        menuFont.drawShadow(g,guidance,left,96,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF));
        int size=pageSize(112,72);
        for(int i=page*size;i<Math.min(lines.size(),(page+1)*size);i++){
            int rowY=112+(i-page*size)*22;
            menuFont.drawShadow(g,fit(lines.get(i),contentWidth),left,rowY,MenuAppearance.color(MenuColor.BORDERS,0xFFB3BEC2));
            if(x>=left&&x<=left+contentWidth&&y>=rowY&&y<rowY+18)
                net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,tooltips.get(i),x,y);
        }
    }
}

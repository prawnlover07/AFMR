package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.PlaylistImportReport;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

final class PlaylistImportScreen extends MusicScreen {
    private String destination;
    private PlaylistImportReport report;
    private boolean wasScanning;
    PlaylistImportScreen(Screen parent,ClientRuntime runtime,String destination){super("Import foobar2000 playlists",parent,runtime);this.destination=destination;}
    @Override protected void init(){
        super.init();report=runtime.lastPlaylistImport();wasScanning=runtime.scanning();
        var playlists=runtime.library().playlists;
        if(runtime.library().playlist(destination)==null)destination=playlists.getFirst().id;
        var list=addRenderableWidget(new DenseList<dev.animal.afmr.core.Library.Playlist>(left,65,contentWidth,Math.max(28,height-163),(g,p,x,y,w,hover)->{
            if(p.id.equals(destination)||hover)g.fill(x,y,x+w,y+14,p.id.equals(destination)?0xFF35594E:0xFF2C373A);
            g.text(font,fit(p.name+(p.enabled?"":" [auto off]"),w-8),x+4,y+3,0xFFDCE6E0);
        },(p,e,twice)->destination=p.id,p->p.name));
        list.items(playlists);list.reveal(runtime.library().playlist(destination));list.active=!runtime.scanning();
        button("Choose .fplite files...",left,height-91,contentWidth,()->NativePicker.open(runtime,NativePicker.Kind.PLAYLISTS,destination)).active=!runtime.scanning();
        button("View last import report",left,height-29,Math.min(170,contentWidth-112),()->minecraft.gui.setScreen(new PlaylistImportReportScreen(this,runtime,report))).active=report!=null;
        button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override public void tick(){if(report!=runtime.lastPlaylistImport()||wasScanning!=runtime.scanning())rebuildWidgets();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit("Add songs to: "+runtime.library().playlist(destination).name,contentWidth),left,48,0xFF8DE0BE);
        g.text(font,fit("Existing song switches are kept. foobar files are read-only.",contentWidth),left,height-65,0xFF8FA7A0);
        String status=runtime.scanning()?"Reading playlists and linked music...":report==null?"Choose a destination, then select one or more .fplite files.":report.addedToPlaylist()+" added to "+report.destination()+" | "+report.skipped()+" skipped. View report for details.";
        g.text(font,fit(status,contentWidth),left,height-48,0xFF8FA7A0);
    }
}

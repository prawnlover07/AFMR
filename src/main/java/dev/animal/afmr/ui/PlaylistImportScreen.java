package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import dev.animal.afmr.library.PlaylistImportReport;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

final class PlaylistImportScreen extends MusicScreen {
    private String destination;
    private PlaylistImportReport report;
    private boolean wasScanning;
    PlaylistImportScreen(Screen parent,ClientRuntime runtime,String destination){super("Add music",parent,runtime);this.destination=destination;}
    @Override protected void init(){
        super.init();report=runtime.lastPlaylistImport();wasScanning=runtime.scanning();
        var playlists=runtime.library().playlists;
        if(!destination.equals("library")&&runtime.library().playlist(destination)==null)destination=playlists.getFirst().id;
        var library=button("Library only",left,47,contentWidth,()->{destination="library";rebuildWidgets();});library.selected=destination.equals("library");
        var importToLibraryOnlyHelpText=Component.translatable("afmr.copy.playlistimport.import_to_library_only");
        if(!importToLibraryOnlyHelpText.getString().isBlank())library.setTooltip(Tooltip.create(importToLibraryOnlyHelpText));
        var list=addRenderableWidget(new DenseList<dev.animal.afmr.core.Library.Playlist>(left,73,contentWidth,Math.max(28,height-173),(g,p,x,y,w,hover)->{
            if(p.id.equals(destination)||hover)g.fill(x,y,x+w,y+14,p.id.equals(destination)?MenuAppearance.color(MenuColor.SELECTION,0xFF35594E):MenuAppearance.color(MenuColor.HOVER,0xFF2C373A));
            g.text(font,fit(p.name+(p.enabled?"":" [auto off]"),w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFDCE6E0));
        },(p,e,twice)->{destination=p.id;rebuildWidgets();},p->p.name));
        list.items(playlists);list.reveal(runtime.library().playlist(destination));list.active=!runtime.scanning();
        int third=(contentWidth-8)/3;
        button("Files",left,height-91,third,()->NativePicker.open(runtime,NativePicker.Kind.FILES,destination)).active=!runtime.scanning();
        button("Folder",left+third+4,height-91,third,()->NativePicker.open(runtime,NativePicker.Kind.FOLDER,destination)).active=!runtime.scanning();
        button("Foobar playlist",left+2*(third+4),height-91,third,()->NativePicker.open(runtime,NativePicker.Kind.PLAYLISTS,destination)).active=!runtime.scanning()&&!destination.equals("library");
        button("View last import report",left,height-29,Math.min(170,contentWidth-112),()->minecraft.gui.setScreen(new PlaylistImportReportScreen(this,runtime,report))).active=report!=null;
        button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override public void tick(){if(report!=runtime.lastPlaylistImport()||wasScanning!=runtime.scanning())rebuildWidgets();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        String status=runtime.scanning()?"Importing...":report==null?"":report.addedToPlaylist()+" added / "+report.skipped()+" skipped";
        g.text(font,fit(status,contentWidth),left,height-48,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF8FA7A0));
    }
}

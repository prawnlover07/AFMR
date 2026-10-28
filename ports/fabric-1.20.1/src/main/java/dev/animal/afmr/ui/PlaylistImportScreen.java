package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import dev.animal.afmr.library.PlaylistImportReport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

final class PlaylistImportScreen extends MusicScreen {
    private String destination;
    private PlaylistImportReport report;
    private boolean wasScanning;
    PlaylistImportScreen(Screen parent,ClientRuntime runtime,String destination){super("Add music",parent,runtime);this.destination=destination;}
    @Override protected void init(){
        super.init();report=runtime.lastPlaylistImport();wasScanning=runtime.scanning();
        var playlists=runtime.library().playlists;
        if(!destination.equals("library")&&runtime.library().playlist(destination)==null)destination=playlists.get(0).id;
        var library=button("Library only",left,47,contentWidth,()->{destination="library";rebuildWidgets();});library.selected=destination.equals("library");
        var importToLibraryOnlyHelpText=Component.translatable("afmr.copy.playlistimport.import_to_library_only");
        if(!importToLibraryOnlyHelpText.getString().isBlank())library.setTooltip(Tooltip.create(importToLibraryOnlyHelpText));
        var list=addRenderableWidget(new DenseList<dev.animal.afmr.core.Library.Playlist>(left,73,contentWidth,Math.max(28,bodyBottom()-173),(g,p,x,y,w,hover)->{
            if(p.id.equals(destination)||hover)g.fill(x,y,x+w,y+14,p.id.equals(destination)?MenuAppearance.color(MenuColor.SELECTION,0xFF304963):MenuAppearance.color(MenuColor.HOVER,0xFF2C303E));
            g.drawString(menuFont,fit(p.name+(p.enabled?"":" [auto off]"),w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
        },(p,clickX,clickY,button,twice)->{destination=p.id;rebuildWidgets();},p->p.name));
        list.items(playlists);list.reveal(runtime.library().playlist(destination));list.active=!runtime.scanning();
        int third=(contentWidth-8)/3;
        button("Files",left,bodyBottom()-91,third,()->NativePicker.open(runtime,NativePicker.Kind.FILES,destination)).active=!runtime.scanning();
        button("Folder",left+third+4,bodyBottom()-91,third,()->NativePicker.open(runtime,NativePicker.Kind.FOLDER,destination)).active=!runtime.scanning();
        button("Foobar playlist",left+2*(third+4),bodyBottom()-91,third,()->NativePicker.open(runtime,NativePicker.Kind.PLAYLISTS,destination)).active=!runtime.scanning()&&!destination.equals("library");
        button("View last import report",left,bodyBottom()-29,Math.min(170,contentWidth-112),()->minecraft.setScreen(new PlaylistImportReportScreen(this,runtime,report))).active=report!=null;
        button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override public void tick(){if(report!=runtime.lastPlaylistImport()||wasScanning!=runtime.scanning())rebuildWidgets();}
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        String status=runtime.scanning()?"Importing...":report==null?"":report.addedToPlaylist()+" added / "+report.skipped()+" skipped";
        g.drawString(menuFont,fit(status,contentWidth),left,bodyBottom()-48,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF));
    }
}

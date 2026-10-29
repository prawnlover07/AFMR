package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.library.PlaylistPresets;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

/** Selects settings to share; export snapshots are taken before opening Explorer. */
final class PlaylistSharingScreen extends MusicScreen {
    private String guidance;
    private final Set<String> selected=new HashSet<>();
    private final String anchor;
    private FlatButton export;
    private boolean includeSongs;
    private ClientRuntime.PresetImport shown;
    private long revision;
    private String statusText="";
    private List<FormattedCharSequence> statusTooltip=List.of();
    PlaylistSharingScreen(Screen parent,ClientRuntime runtime,String playlist){
        super("Import / export playlist setups",parent,runtime);anchor=playlist;selected.add(playlist);
    }
    @Override protected void init(){
        super.init();guidance=fit(includeSongs?Component.translatable("afmr.copy.playlistsharing.guidance").getString():Component.translatable("afmr.copy.playlistsharing.settings_only").getString(),contentWidth);statusText="";revision=runtime.catalogRevision();int half=(contentWidth-6)/2;
        var importSetupHelpControl=button("Import .afmrp...",left,48,half,()->{
            if(runtime.pendingPreset()!=null)showImport(runtime.pendingPreset());
            else NativePicker.open(runtime,NativePicker.Kind.PRESET_IMPORT,anchor);
        });
        var importSetupHelpText=Component.translatable("afmr.copy.playlistsharing.import_setup");
        if(!importSetupHelpText.getString().isBlank())importSetupHelpControl.description(importSetupHelpText);
        export=button("Export selected...",left+half+6,48,half,()->{
            try{NativePicker.savePreset(runtime,PlaylistPresets.capture(runtime.library(),selected,includeSongs));}
            catch(IllegalArgumentException e){runtime.message(e.getMessage());}
        });
        var exportSetupHelpText=Component.translatable("afmr.copy.playlistsharing.export_setup");
        if(!exportSetupHelpText.getString().isBlank())export.description(exportSetupHelpText);
        toggle("Include song list",left,74,contentWidth,()->includeSongs,()->{includeSongs=!includeSongs;rebuildWidgets();});
        button("All",left,100,56,()->{selected.clear();runtime.library().playlists.forEach(p->selected.add(p.id));updateControls();});
        button("None",left+62,100,56,()->{selected.clear();updateControls();});
        var rows=addRenderableWidget(new DenseList<Playlist>(left,127,contentWidth,Math.max(28,bodyBottom()-203),
            (g,p,x,y,w,hover)->{
                if(selected.contains(p.id))net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
                menuFont.draw(g,fit((selected.contains(p.id)?"[x] ":"[ ] ")+p.name,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6));
            },(p,clickX,clickY,button,twice)->{if(!selected.remove(p.id))selected.add(p.id);updateControls();},p->p.name));
        rows.items(runtime.library().playlists);updateControls();
        button("Full configuration...",left,bodyBottom()-29,(contentWidth-6)/2,()->minecraft.setScreen(new ConfigBackupScreen(this,runtime)));
        button("Back",left+(contentWidth-6)/2+6,bodyBottom()-29,(contentWidth-6)/2,this::onClose);
    }
    private void updateControls(){export.active=!runtime.scanning()&&!selected.isEmpty()&&selected.size()<=PlaylistPresets.MAX_PLAYLISTS;}
    private void showImport(ClientRuntime.PresetImport pending){shown=pending;minecraft.setScreen(new PresetImportScreen(this,runtime,pending));}
    @Override public void tick(){
        if(revision!=runtime.catalogRevision())rebuildWidgets();updateControls();
        var pending=runtime.pendingPreset();if(pending!=null&&pending!=shown)showImport(pending);
    }
    void imported(List<String> ids,String profile,boolean repair){
        Screen next=this;
        if(profile.equals(runtime.library().profiles.activeId)&&parent instanceof LibraryScreen editor)
            next=new LibraryScreen(editor.parent,runtime,ids.get(0));
        minecraft.setScreen(repair?new LibraryRepairScreen(next,runtime):next);
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        menuFont.drawShadow(g,fit(selected.size()+" selected / maximum "+PlaylistPresets.MAX_PLAYLISTS,contentWidth-126),left+126,106,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        if(runtime.scanning())ScanProgressView.draw(g,menuFont,runtime.scanProgress(),left,bodyBottom()-66,contentWidth);
        else{
            menuFont.drawShadow(g,guidance,left,bodyBottom()-65,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
            menuFont.drawShadow(g,fit(runtime.taskStatus(),contentWidth),left,bodyBottom()-48,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
            if(x>=left&&x<left+contentWidth&&y>=bodyBottom()-50&&y<bodyBottom()-36){
                if(!statusText.equals(runtime.taskStatus())){statusText=runtime.taskStatus();statusTooltip=menuFont.split(Component.literal(statusText),Math.max(100,Math.min(400,contentWidth)));}
                net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,statusTooltip,x,y);
            }
        }
    }
}

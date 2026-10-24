package dev.animal.afmr.ui;

import net.minecraft.network.chat.Component;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.PresetSongs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

/** Folder choices belong to the pending import; leaving this page never changes the library. */
final class PresetSongFoldersScreen extends MusicScreen {
    private String repairGuidance,chooseGuidance;
    private final ClientRuntime.PresetImport pending;
    private PresetSongs.Plan shown;
    private PresetSongs.Root selected;
    private FlatButton locate,recheck;
    private final Map<String,String> labels=new HashMap<>();
    private final Map<String,String> examples=new HashMap<>();
    PresetSongFoldersScreen(Screen parent,ClientRuntime runtime,ClientRuntime.PresetImport pending){
        super("Locate playlist music folders",parent,runtime);this.pending=pending;
    }
    @Override protected void init(){
        super.init();repairGuidance=fit(Component.translatable("afmr.copy.presetsongfolders.missing_or_renamed_songs_can_also_be_repaired_after_import").getString(),contentWidth);chooseGuidance=Component.translatable("afmr.copy.presetsongfolders.choose_a_folder_containing_the_saved_relative_paths").getString();int half=(contentWidth-6)/2;
        locate=button("Locate selected folder...",left,48,half,()->{
            if(selected!=null)NativePicker.open(runtime,NativePicker.Kind.PRESET_FOLDER,selected.id());
        });
        recheck=button("Check song files again",left+half+6,48,half,()->runtime.preparePresetSongs(pending));
        if(runtime.presetSongPlan()!=null)shown=runtime.presetSongPlan();
        labels.clear();examples.clear();for(var song:pending.data().songs)if(!song.vanilla)examples.putIfAbsent(song.folder,song.path);
        List<PresetSongs.Root> roots=shown==null?List.of():shown.roots();
        for(var root:roots)labels.put(root.id(),fit(root.name()+" | "+(root.chosen()?root.path():Component.translatable("afmr.copy.presetsongfolders.additional_guidance").getString()),contentWidth-8));
        var rows=addRenderableWidget(new DenseList<PresetSongs.Root>(left,75,contentWidth,Math.max(28,height-167),
            (g,root,x,y,w,hover)->{
                if(selected!=null&&selected.id().equals(root.id()))g.fill(x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF35594E));
                g.drawString(font,labels.get(root.id()),x+4,y+3,root.chosen()?MenuAppearance.color(MenuColor.TEXT,0xFFE3F2E9):0xFFFFB0A7,false);
            },(root,clickX,clickY,button,twice)->{selected=root;controls();if(twice)NativePicker.open(runtime,NativePicker.Kind.PRESET_FOLDER,root.id());},root->root.name()+" | "+root.path()));
        rows.items(roots);controls();
        button("Back to import",left,height-29,contentWidth,this::onClose);
    }
    private void controls(){
        boolean free=runtime.pendingPreset()==pending&&!runtime.scanning();locate.active=free&&selected!=null;recheck.active=free;
    }
    @Override public void tick(){
        if(runtime.presetSongPlan()!=null&&shown!=runtime.presetSongPlan())rebuildWidgets();controls();
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        g.drawString(font,repairGuidance,left,height-81,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3));
        if(runtime.scanning())ScanProgressView.draw(g,font,runtime.scanProgress(),left,height-66,contentWidth);
        else{
            g.drawString(font,fit(selected==null?chooseGuidance:"Expected inside folder: "+examples.get(selected.id()),contentWidth),left,height-65,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3));
            g.drawString(font,fit(runtime.taskStatus(),contentWidth),left,height-48,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        }
    }
}

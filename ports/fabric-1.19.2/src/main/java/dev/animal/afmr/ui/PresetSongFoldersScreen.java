package dev.animal.afmr.ui;

import net.minecraft.network.chat.Component;
import dev.animal.afmr.core.MenuColor;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.PresetSongs;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

/** Cached import matches; choosing files/folders does not change the live library. */
final class PresetSongFoldersScreen extends MusicScreen {
    private record Row(String id,boolean folder,String title,String path,boolean found,String search){}
    private final ClientRuntime.PresetImport pending;
    private PresetSongs.Plan shown;
    private Row selected;
    private FlatButton locate,linked,folder,details;
    private DenseList<Row> list;
    private List<Row> rows=List.of();
    private final Map<String,String> labels=new HashMap<>();
    private final Set<String> signedFolders=new HashSet<>();
    private int view;
    private String query="",repairGuidance,chooseGuidance;
    PresetSongFoldersScreen(Screen parent,ClientRuntime runtime,ClientRuntime.PresetImport pending){
        super("Find imported song files",parent,runtime);this.pending=pending;
    }
    @Override protected void init(){
        super.init();shown=runtime.presetSongPlan();int half=(contentWidth-6)/2;
        repairGuidance=fit(Component.translatable("afmr.copy.presetsongfolders.missing_or_renamed_songs_can_also_be_repaired_after_import").getString(),contentWidth);
        chooseGuidance=Component.translatable("afmr.copy.presetsongfolders.choose_a_folder_containing_the_saved_relative_paths").getString();
        locate=button("Choose selected file...",left,48,half,this::locate);
        folder=button("Search a folder...",left+half+6,48,half,()->NativePicker.open(runtime,NativePicker.Kind.PRESET_SEARCH_FOLDER,""));
        linked=button("Search linked libraries",left,74,half,()->{
            if(runtime.presetSearchActive())runtime.cancelPresetSearch();else runtime.searchPresetLibraries(pending);
        });
        button("Show: "+switch(view){case 0->"Missing songs";case 1->"All songs";default->"Original folders";},left+half+6,74,half,()->{view=(view+1)%3;rebuildWidgets();});
        MenuField search=edit(query,"Filter song titles / paths",left,100,contentWidth,value->{query=value;filter();});
        list=addRenderableWidget(new DenseList<Row>(left,127,contentWidth,Math.max(28,bodyBottom()-217),
            (g,row,x,y,w,hover)->{
                if(selected!=null&&selected.id().equals(row.id()))net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
                menuFont.draw(g,labels.get(row.id()),x+4,y+3,row.found()?MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6):0xFFFFB0A7);
            },(row,clickX,clickY,button,twice)->{selected=row;controls();if(twice)locate();},row->row.title()+" | "+row.path()));
        var built=new ArrayList<Row>();labels.clear();
        signedFolders.clear();for(var song:pending.data().songs)if(!song.sha256.isEmpty())signedFolders.add(song.folder);
        if(view==2){
            var counts=new HashMap<String,int[]>();
            for(int i=0;i<pending.data().songs.size();i++){
                var song=pending.data().songs.get(i);if(song.vanilla)continue;
                var count=counts.computeIfAbsent(song.folder,key->new int[2]);count[1]++;
                if(shown!=null&&shown.songs().get(i).found())count[0]++;
            }
            for(var root:shown==null?List.<PresetSongs.Root>of():shown.roots()){
                var count=counts.get(root.id());boolean complete=count!=null&&count[0]==count[1];
                String path=root.chosen()?root.path():complete?"All songs matched":Component.translatable("afmr.copy.presetsongfolders.additional_guidance").getString();
                String title=root.name()+(count==null?"":" ("+count[0]+"/"+count[1]+" matched)");
                built.add(new Row(root.id(),true,title,path,complete,(root.name()+" "+path).toLowerCase(Locale.ROOT)));
            }
        }else{
            for(int i=0;i<pending.data().songs.size();i++){
                var song=pending.data().songs.get(i);if(song.vanilla)continue;
                var match=shown==null?null:shown.songs().get(i);boolean found=match!=null&&match.found();
                if(view==0&&found)continue;
                String path=found?match.track().path:song.path;
                built.add(new Row(song.id,false,song.title,path,found,(song.title+" "+path).toLowerCase(Locale.ROOT)));
            }
        }
        rows=List.copyOf(built);
        if(selected!=null){String id=selected.id();selected=rows.stream().filter(row->row.id().equals(id)).findFirst().orElse(null);}
        for(var row:rows)labels.put(row.id(),fit((row.folder()?"Folder: ":row.found()?"Matched: ":"Missing: ")+row.title()+" | "+row.path(),contentWidth-8));
        filter();
        details=button("Search details",left,bodyBottom()-29,half,()->{
            var plan=runtime.presetSongPlan();if(plan!=null&&!plan.errors().isEmpty())minecraft.setScreen(new ScanMessagesScreen(this,runtime,plan.errors()));
        });
        button("Back to import",left+half+6,bodyBottom()-29,half,this::onClose);controls();
    }
    private void filter(){
        if(list==null)return;String q=query.toLowerCase(Locale.ROOT);
        list.items(rows.stream().filter(row->q.isBlank()||row.search().contains(q)).toList());
    }
    private void locate(){
        if(selected==null||runtime.scanning())return;
        NativePicker.open(runtime,selected.folder()?NativePicker.Kind.PRESET_FOLDER:NativePicker.Kind.PRESET_FILE,selected.id());
    }
    private void controls(){
        boolean free=runtime.pendingPreset()==pending&&!runtime.scanning();
        locate.active=free&&selected!=null;folder.active=free;
        locate.setMessage(Component.literal(selected!=null&&selected.folder()?"Choose selected folder...":"Choose selected file..."));
        linked.setMessage(Component.literal(runtime.presetSearchCancelling()?"Cancelling...":runtime.presetSearchActive()?"Cancel search":"Search linked libraries"));
        linked.active=runtime.presetSearchActive()?!runtime.presetSearchCancelling():free;
        details.active=free&&shown!=null&&!shown.errors().isEmpty();
    }
    @Override public void tick(){if(shown!=runtime.presetSongPlan())rebuildWidgets();controls();}
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        String summary=shown==null?"Choose a file or search your local music.":shown.found()+" matched / "+shown.missing()+" missing / "+shown.verified()+" verified by contents";
        menuFont.drawShadow(g,fit(summary,contentWidth),left,bodyBottom()-81,MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6));
        if(runtime.scanning())ScanProgressView.draw(g,menuFont,runtime.scanProgress(),left,bodyBottom()-66,contentWidth);
        else{
            boolean legacy=selected!=null&&selected.folder()&&!signedFolders.contains(selected.id());
            String guidance=legacy?chooseGuidance:repairGuidance;
            menuFont.drawShadow(g,fit(guidance,contentWidth),left,bodyBottom()-65,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
            menuFont.drawShadow(g,fit(runtime.taskStatus(),contentWidth),left,bodyBottom()-48,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        }
    }
}

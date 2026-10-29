package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.SongColumn;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;

/** Playlist workspace. All model mutations remain on the client thread. */
public final class LibraryScreen extends Screen {
    private final MenuDescriptions descriptions=new MenuDescriptions();
    private net.minecraft.client.gui.Font menuFont;
    Screen parent;private final ClientRuntime runtime;
    float scale;private int vw,vh,split,tableX,tableWidth;
    private SongTableLayout songTable;
    private String[] columnHeaders;
    private final Map<String,String[]> songCells=new HashMap<>();
    private boolean draggingColumns;
    private double columnDragGrab;
    private WorkspaceLayout layout;
    private DenseList<Playlist> priorities;
    private final Map<String,Integer> ranks=new HashMap<>();
    private final EnumMap<WorkspacePage,MusicScreen> pages=new EnumMap<>(WorkspacePage.class);
    private FlatButton addPlaying,skipWait,repairButton,undoButton,redoButton,songActions,playlistActions;
    private TransportButton playPause,nextTrack;
    private static final int PLAYLIST_ACTION_WIDTH=22,PLAYLIST_ACTION_GAP=4;
    private Playlist actionPlaylist;
    private final PriorityDrag priorityDrag=new PriorityDrag();
    private long nextDragScroll;
    private final Map<String,PlaylistSettingsPanel> settingsPanels=new HashMap<>();
    private PlaylistSettingsPanel settingsPanel;
    private final List<FlatButton> menuButtons=new ArrayList<>();
    private int menuFocus;
    private dev.animal.afmr.library.LibraryHealth.Report repairReport;
    private String playlistId="day_outside",libraryQuery="",songQuery="",folderSelection="";
    private final Set<String> expanded=new HashSet<>();
    private boolean initialTree=true;
    private final Set<String> librarySelection=new LinkedHashSet<>(),songSelection=new LinkedHashSet<>();
    private String libraryAnchor="",songAnchor="";
    private String playbackLabel="";
    private Track actionSong;
    private SeekBar seekBar;
    private NowPlayingLink nowPlayingTitle;
    private Track inspected;private final EditorArtwork art=new EditorArtwork();
    private DenseList<TreeRow> folders;private DenseList<Track> songs;
    private List<TreeRow> treeRows=List.of();private List<Track> songRows=List.of();
    private SongColumn sortColumn=SongColumn.TITLE;private boolean descending,wasScanning;private long songRevision=-1;
    private MenuField librarySearch,songSearch;
    private List<MenuSurface> backgrounds=List.of();
    private final List<Node> treeRoots=new ArrayList<>();
    private List<String> treeFolders=List.of();
    private long treeRevision=-1;
    private String treeFilter=null;
    private record TreeRow(String id,String label,int depth,Track track,boolean group,boolean open){}
    private static final class Node {
        String id,label,search="";boolean matches=true;Track track;Map<String,Node> children=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Node(String id,String label){this.id=id;this.label=label;}
    }
    public LibraryScreen(Screen parent,ClientRuntime runtime,String playlistId){super(new net.minecraft.network.chat.TextComponent("animal's final music replacer"));this.parent=parent;this.runtime=runtime;this.playlistId=playlistId;}
    private Playlist playlist(){return runtime.library().playlist(playlistId);}
    void selectPlaylist(String id){playlistId=id;songSelection.clear();songQuery="";if(songs!=null)songs.top();}
    @Override protected void clearWidgets() {
        // 1.18 clears directly in final Screen.init, before the page can retain its draft widgets.
        if(MusicMenu.isRestoring(this)&&menuFont==runtime.fonts().font(CustomFonts.Target.MENU))return;
        super.clearWidgets();
    }
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        menuFont=runtime.fonts().font(CustomFonts.Target.MENU);font=menuFont;
        menuButtons.clear();
        int folderOffset=folders==null?0:folders.offset(),songOffset=songs==null?0:songs.offset();
        int priorityOffset=priorities==null?0:priorities.offset();
        layout=WorkspaceLayout.fit(width,height);scale=layout.scale();vw=layout.width();vh=layout.height();
        split=layout.sidebarWidth();tableX=layout.tableX();tableWidth=layout.tableWidth();
        backgrounds=new ArrayList<>();backgrounds.add(new MenuSurface(0,0,vw,vh,MenuColor.BACKGROUND,0x17191D));
        for(var panel:List.of(new MenuSurface(0,0,vw,64,MenuColor.PANELS,0x202329),
            new MenuSurface(0,64,split,layout.footerY(),MenuColor.PANELS,0x202329),
            new MenuSurface(layout.rulesX(),74,layout.rulesX()+layout.rulesWidth(),layout.footerY()-12,MenuColor.PANELS,0x202329),
            new MenuSurface(tableX,layout.songHeaderY(),tableX+tableWidth,layout.songListY(),MenuColor.PANELS,0x282E34),
            new MenuSurface(0,layout.footerY(),vw,vh,MenuColor.PANELS,0x202329))){
            MenuSurface.cutOut(backgrounds,panel.x0(),panel.y0(),panel.x1(),panel.y1());backgrounds.add(panel);
        }
        MenuSurface.cutOut(backgrounds,12,94,split-12,94+layout.priorityHeight());
        MenuSurface.cutOut(backgrounds,12,layout.libraryListY(),split-12,layout.libraryListY()+layout.libraryListHeight());
        MenuSurface.cutOut(backgrounds,tableX,layout.songListY(),tableX+tableWidth,layout.songListY()+layout.songListHeight());
        songTable=new SongTableLayout(runtime.library().settings.songColumns,tableWidth-8-SongTableLayout.ACTIONS-SongTableLayout.ACTION_GAP,songTable==null?0:songTable.scroll());
        draggingColumns=false;
        if(initialTree){initialTree=false;expanded.add(runtime.library().folders.isEmpty()?"all":"folder:"+Path.of(runtime.library().folders.get(0).path).toAbsolutePath().normalize());}
        if(runtime.library().playlists.isEmpty()){var p=runtime.library().createPlaylist("New playlist");selectPlaylist(p.id);runtime.save();}
        if(playlist()==null)selectPlaylist(runtime.library().playlists.get(0).id);
        runtime.playlistEdits().watch(runtime.library(),playlist());
        int navX=12;
        for(WorkspacePage page:WorkspacePage.values()){
            var tab=flat(page.label,navX,24,page.width,22,()->openPage(page));tab.selected=page==WorkspacePage.PLAYLISTS;navX+=page.width+4;
        }
        flat("Search settings...",vw-484,4,126,18,()->openSearch());
        flat("Import / export",vw-352,4,126,18,()->minecraft.setScreen(new PlaylistSharingScreen(this,runtime,playlistId)));
        flat("Done",vw-72,4,60,18,this::onClose);
        repairReport=runtime.sourceHealth();
        repairButton=flat("Repair / missing",vw-212,4,130,18,()->minecraft.setScreen(new LibraryRepairScreen(this,runtime)));
        updateRepairButton();
        priorities=addRenderableWidget(new DenseList<>(12,94,split-24,layout.priorityHeight(),this::drawPriority,(p,cx,cy,button,twice)->{
            if(settingsPanel!=null&&!settingsPanel.commit())return;selectPlaylist(p.id);init(minecraft,width,height);priorities.reveal(p);
        },p->p.name));
        playlistActions=flat("...",priorities.x+priorities.getWidth()-8-PLAYLIST_ACTION_WIDTH,94,PLAYLIST_ACTION_WIDTH,14,()->{
            if(actionPlaylist!=null)showPlaylistMenu(actionPlaylist,playlistActions.x,playlistActions.y+14);
        });playlistActions.visible=false;playlistActions.description(new net.minecraft.network.chat.TextComponent("Playlist actions"));
        ranks.clear();for(int i=0;i<runtime.library().playlists.size();i++)ranks.put(runtime.library().playlists.get(i).id,i+1);
        priorities.items(runtime.library().playlists);
        int controlsY=100+layout.priorityHeight();
        flat("New playlist",12,controlsY,split-64,22,()->minecraft.setScreen(new NewPlaylistScreen(this,runtime,null)));
        flat("...",split-46,controlsY,34,22,()->showPlaylistMenu(playlist(),split-46,controlsY+24));
        var importIntoLibraryHelpControl=flat("+",split-38,layout.libraryY()-5,26,20,()->minecraft.setScreen(new PlaylistImportScreen(this,runtime,"library")));
        var importIntoLibraryHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.library.import_into_library");
        if(!importIntoLibraryHelpText.getString().isBlank())importIntoLibraryHelpControl.description(importIntoLibraryHelpText);
        librarySearch=search(libraryQuery,"Search music",12,layout.libraryY()+18,split-24,v->{libraryQuery=v;folders.top();refreshFolders();});
        folders=addRenderableWidget(new DenseList<>(12,layout.libraryListY(),split-24,layout.libraryListHeight(),this::drawFolder,this::clickFolder,r->r.label));
        var addSelectedOrFolderHelpControl=flat("Add selected / folder ->",12,layout.footerY()-26,split-24,22,this::addSelection);
        var addSelectedOrFolderHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.library.add_selected_or_folder");
        if(!addSelectedOrFolderHelpText.getString().isBlank())addSelectedOrFolderHelpControl.description(addSelectedOrFolderHelpText);
        flat("Playlist actions ...",tableX,104,160,22,()->showPlaylistMenu(playlist(),tableX,128));
        songSearch=search(songQuery,"Search playlist",tableX,138,tableWidth-184,v->{songQuery=v;songs.top();refreshSongs();});
        var songColumnsHelpControl=flat("Columns",tableX+tableWidth-178,136,74,22,()->minecraft.setScreen(new SongColumnsScreen(this,runtime)));
        var songColumnsHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.library.song_columns");
        if(!songColumnsHelpText.getString().isBlank())songColumnsHelpControl.description(songColumnsHelpText);
        flat("Add music",tableX+tableWidth-98,136,98,22,()->minecraft.setScreen(new PlaylistImportScreen(this,runtime,playlistId)));
        songs=addRenderableWidget(new DenseList<>(tableX,layout.songListY(),tableWidth,layout.songListHeight()-(songTable.overflow()?SongTableLayout.BAR:0),this::drawSong,this::clickSong,t->t.title));
        songActions=flat("...",tableX+SongTableLayout.PINNED+songTable.viewport+SongTableLayout.ACTION_GAP,layout.songListY(),SongTableLayout.ACTIONS,14,()->{if(actionSong!=null)showSongMenu(actionSong,songActions.x,songActions.y+14);});
        songActions.visible=false;
        var songActionsHelp=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.library.song_actions");if(!songActionsHelp.getString().isBlank())songActions.description(songActionsHelp);
        undoButton=flat("Undo",tableX+166,104,58,22,()->restoreEdit(false));
        redoButton=flat("Redo",tableX+230,104,58,22,()->restoreEdit(true));
        undoButton.active=runtime.playlistEdits().canUndo(runtime.library());redoButton.active=runtime.playlistEdits().canRedo(runtime.library());
        if(settingsPanels.containsKey(playlistId)&&settingsPanels.get(playlistId).playlist!=playlist())settingsPanels.remove(playlistId);
        settingsPanel=settingsPanels.computeIfAbsent(playlistId,id->new PlaylistSettingsPanel(this,runtime,playlist()));
        for(var control:settingsPanel.build(layout))addRenderableWidget(control);
        int transportX=vw/2-100,transportY=layout.footerY()+15;
        playPause=addRenderableWidget(new TransportButton(transportX,transportY,scale,"afmr.copy.library.pause_and_resume",()->{
            if(runtime.engine().paused()||runtime.engine().current()!=null)runtime.togglePause();
            else runtime.playNow();
        }));
        nextTrack=addRenderableWidget(new TransportButton(transportX+34,transportY,scale,"afmr.copy.library.next",runtime::nextSong));
        nextTrack.state(TransportButton.Icon.NEXT,"Next song now");
        skipWait=flat("Skip & wait",transportX+68,transportY,112,22,runtime::skip);
        var skipHelp=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.library.skip_wait");
        if(!skipHelp.getString().isBlank())skipWait.description(skipHelp);
        addPlaying=flat("Add playing to playlist",transportX,transportY+26,228,22,()->{
            var track=runtime.engine().current();if(track!=null)minecraft.setScreen(new TrackPlaylistScreen(this,runtime,track.id,track.title));
        });addPlaying.active=runtime.engine().current()!=null;
        var returnToAutomaticHelpControl=flat("Use automatic playlists",vw-170,transportY+26,158,22,runtime::returnAutomatic);
        var returnToAutomaticHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.library.return_to_automatic");
        if(!returnToAutomaticHelpText.getString().isBlank())returnToAutomaticHelpControl.description(returnToAutomaticHelpText);
        seekBar=addRenderableWidget(new SeekBar(runtime,12,layout.footerY()+66,vw-24));
        nowPlayingTitle=addRenderableWidget(new NowPlayingLink(64,layout.footerY()+10,vw/2-185,scale,this::locatePlayingSong));
        updateTransport();
        refresh();folders.offset(folderOffset);songs.offset(songOffset);priorities.offset(priorityOffset);
        songRevision=runtime.catalogRevision();wasScanning=runtime.scanning();
        updatePlaybackLabel();
    }
    void openSearch(){if(settingsPanel==null||settingsPanel.commit())minecraft.setScreen(new SettingsSearchScreen(this,runtime));}
    void revealPlaylistSetting(String text){settingsPanel.reveal(text);}
    String selectedPlaylistId(){return playlistId;}
    void locatePlayingSong(){
        var engine=runtime.engine();var current=engine.current();var source=runtime.library().playlist(engine.sourcePlaylistId());
        if(current==null||source==null||settingsPanel!=null&&!settingsPanel.commit())return;
        selectPlaylist(source.id);songQuery="";songAnchor=current.id;songSelection.add(current.id);inspected=current;
        minecraft.setScreen(this);priorities.reveal(source);songs.reveal(current);setFocused(songs);if(!songs.isFocused())songs.changeFocus(true);
        for(int i=0;i<songTable.columns.length;i++)if(songTable.columns[i]==SongColumn.TITLE){songTable.scroll(songTable.offsets[i]);break;}
    }
    private void updateTransport(){
        runtime.engine().requestCurrentDuration();var track=runtime.engine().current();
        if(playPause!=null){
            boolean canPlay=runtime.inWorld()&&runtime.enabled();
            playPause.active=nextTrack.active=canPlay;skipWait.active=canPlay&&track!=null;
            playPause.state(track!=null&&!runtime.engine().paused()?TransportButton.Icon.PAUSE:TransportButton.Icon.PLAY,runtime.engine().paused()?"Resume":track==null?"Play now":"Pause");
        }
        nowPlayingTitle.active=track!=null&&runtime.library().playlist(runtime.engine().sourcePlaylistId())!=null;
        nowPlayingTitle.label(runtime.minecraftMusicMuted()?"Music muted in Minecraft":track==null?playbackLabel:track.title);
    }
    void openPage(WorkspacePage page){
        if(settingsPanel!=null&&!settingsPanel.commit())return;
        if(page!=WorkspacePage.PLAYLISTS)runtime.playlistEdits().watch(runtime.library(),null);
        if(page==WorkspacePage.PLAYLISTS){minecraft.setScreen(this);return;}
        MusicScreen screen=pages.computeIfAbsent(page,key->switch(key){
            case LIBRARY->new SourcesScreen(this,runtime);case TIMING->new SettingsScreen(this,runtime);
            case DISPLAY->new NowPlayingSettingsScreen(this,runtime);case KEYS->new OverrideBindingsScreen(this,runtime);
            case INSPECTOR->new PlaybackInspectorScreen(this,runtime);
            case ACTIVITY->new ListeningScreen(this,runtime);
            case PROFILES->new ProfilesScreen(this,runtime);
            default->throw new IllegalStateException();
        });
        minecraft.setScreen(screen);
    }
    private void movePlaylist(int target){runtime.playlistEdits().move(runtime.library(),playlistId,target);runtime.save();init(minecraft,width,height);priorities.reveal(playlist());}
    private void deletePlaylist(){
        Playlist target=playlist();
        minecraft.setScreen(new ConfirmScreen(yes->{if(yes){runtime.playlistEdits().delete(runtime.library(),target.id);runtime.save();}minecraft.setScreen(this);},
            new net.minecraft.network.chat.TextComponent("Delete "+target.name+"?"),new net.minecraft.network.chat.TextComponent("Remove playlist and assignments?")));
    }
    void rebuildSettings(){init(minecraft,width,height);}
    void openSettingsScreen(Screen screen){runtime.playlistEdits().watch(runtime.library(),playlist());settingsPanel.clearDrafts();minecraft.setScreen(screen);}
    private void restoreEdit(boolean redo){
        priorityDrag.cancel();
        if(settingsPanel!=null&&!settingsPanel.commit()){settingsPanel.clearDrafts();settingsPanels.clear();init(minecraft,width,height);return;}
        runtime.playlistEdits().checkpoint(runtime.library());
        boolean restored=redo?runtime.playlistEdits().redo(runtime.library()):runtime.playlistEdits().undo(runtime.library());
        if(restored){runtime.save();runtime.message(redo?"Redone":"Undone");}else runtime.message("No edit available; the playlist may have changed outside the editor.");
        settingsPanels.clear();init(minecraft,width,height);priorities.reveal(playlist());
    }
    private void showSongMenu(Track track,int x,int y){
        if(!songSelection.contains(track.id)){songSelection.clear();songSelection.add(track.id);}inspected=track;songAnchor=track.id;
        priorityDrag.cancel();menuButtons.clear();menuFocus=0;setFocused(null);
        String count=songSelection.size()+" "+(songSelection.size()==1?"song":"songs");
        int mx=Math.max(4,Math.min(x,vw-232)),my=Math.max(4,Math.min(y,vh-5*24-4));
        menuItem("Preview song",mx,my,this::previewSelected,true,"afmr.copy.library.preview");
        menuItem("Queue "+count,mx,my,this::queueSelected,true,"afmr.copy.library.queue");
        menuItem("Enable "+count,mx,my,()->setSongs(true),true,null);
        menuItem("Disable "+count,mx,my,()->setSongs(false),true,"afmr.copy.library.disable_songs");
        menuItem("Remove "+count,mx,my,()->{runtime.playlistEdits().assign(runtime.library(),playlist(),songSelection,null);runtime.save();refreshSongs();},true,"afmr.copy.library.remove_assignments");
        if(!menuButtons.get(0).isFocused())menuButtons.get(0).changeFocus(true);
    }
    private void showPlaylistMenu(Playlist target,int x,int y){
        if(target==null||runtime.library().playlist(target.id)!=target||settingsPanel!=null&&!settingsPanel.commit())return;
        if(!target.id.equals(playlistId)){selectPlaylist(target.id);init(minecraft,width,height);}
        menuButtons.clear();menuFocus=0;setFocused(null);settingsPanel.cancelHold();
        int rank=runtime.library().playlists.indexOf(playlist()),last=runtime.library().playlists.size()-1;
        int mx=Math.max(4,Math.min(x,vw-232)),my=Math.max(4,Math.min(y,vh-12*24-4));
        menuItem("Rename playlist",mx,my,()->minecraft.setScreen(new RenamePlaylistScreen(this,runtime,playlist())),true,null);
        menuItem("Duplicate playlist",mx,my,()->minecraft.setScreen(new NewPlaylistScreen(this,runtime,playlist())),true,"afmr.copy.library.clone_playlist");
        menuItem("Move to top",mx,my,()->movePlaylist(0),rank>0,null);
        menuItem("Move up",mx,my,()->movePlaylist(rank-1),rank>0,null);
        menuItem("Move down",mx,my,()->movePlaylist(rank+1),rank<last,null);
        menuItem("Move to bottom",mx,my,()->movePlaylist(last),rank<last,null);
        menuItem("Export playlist setup...",mx,my,()->minecraft.setScreen(new PlaylistSharingScreen(this,runtime,playlistId)),true,null);
        menuItem("Play one song now",mx,my,()->runtime.overridePlaylist(playlistId,OverrideMode.ONE_SONG),true,"afmr.copy.library.play_one_song");
        menuItem("Keep this playlist playing",mx,my,()->runtime.overridePlaylist(playlistId,OverrideMode.UNTIL_RELEASED),true,"afmr.copy.library.until_released");
        menuItem("Manual playback only: "+(playlist().manualOnly?"on":"off"),mx,my,()->{playlist().manualOnly=!playlist().manualOnly;runtime.save();init(minecraft,width,height);},true,"afmr.copy.library.manual_only");
        menuItem("Balance song volumes...",mx,my,()->{if(runtime.normalizePlaylist(playlistId))minecraft.setScreen(new LoudnessScreen(this,runtime,playlistId));},!runtime.scanning(),"afmr.copy.library.normalize_playlist");
        menuItem("Delete playlist...",mx,my,this::deletePlaylist,!runtime.scanning(),null);
        if(!menuButtons.get(0).isFocused())menuButtons.get(0).changeFocus(true);
    }
    private void menuItem(String label,int x,int y,Runnable action,boolean active,String help){
        var control=new FlatButton(label,x,y+menuButtons.size()*24,228,24,scale,()->{menuButtons.clear();action.run();});control.active=active;control.opaque=true;
        if(help!=null){var text=new net.minecraft.network.chat.TranslatableComponent(help);if(!text.getString().isBlank())control.description(text);}
        menuButtons.add(control);
    }
    private void drawPriority(PoseStack g,Playlist p,int x,int y,int w,boolean hover){
        if(p.id.equals(playlistId)||hover)net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,p.id.equals(playlistId)?MenuAppearance.color(MenuColor.SELECTION,0xFF304963):MenuAppearance.color(MenuColor.HOVER,0xFF2C303E));
        menuFont.draw(g,Integer.toString(ranks.getOrDefault(p.id,0)),x+4,y+3,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA1B4BC));
        menuFont.draw(g,fit(p.name,w-28-PLAYLIST_ACTION_WIDTH-PLAYLIST_ACTION_GAP-15),x+28,y+3,p.enabled&&!p.manualOnly?MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6):MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF));
        if(runtime.engine().current()!=null&&p.id.equals(runtime.engine().sourcePlaylistId()))PlaybackIndicator.draw(g,x+w-PLAYLIST_ACTION_WIDTH-PLAYLIST_ACTION_GAP-11,y+2,runtime.engine().paused());
        if(!priorityDrag.armed()&&y>=priorities.y&&y+14<=priorities.y+priorities.getHeight()&&(hover||actionPlaylist==null&&(priorities.isFocused()||playlistActions.isFocused())&&p.id.equals(playlistId))){
            actionPlaylist=p;playlistActions.y=y;playlistActions.visible=true;
        }
    }
    private FlatButton flat(String label,int x,int y,int w,int h,Runnable action){return addRenderableWidget(new FlatButton(label,x,y,w,h,scale,action));}
    private MenuField search(String value,String hint,int x,int y,int w,java.util.function.Consumer<String> callback){
        var box=new MenuField(menuFont,x,y,w,16,new net.minecraft.network.chat.TextComponent(hint));box.setMaxLength(500);box.setValue(value);box.placeholder(new net.minecraft.network.chat.TextComponent(hint));box.setResponder(callback);return addRenderableWidget(box);
    }
    private boolean contains(Track t,String q){return (t.title+" "+t.artist+" "+t.album+" "+t.albumArtist+" "+t.genre+" "+t.date+" "+t.path).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT));}
    private void refresh(){
        if(folders==null||songs==null)return;
        if(inspected!=null)inspected=runtime.library().track(inspected.id);
        refreshFolders();refreshSongs();
    }
    private void refreshFolders(){if(folders!=null){treeRows=buildTree();folders.items(treeRows);}}
    private void refreshSongs(){
        if(undoButton!=null){undoButton.active=runtime.playlistEdits().canUndo(runtime.library());redoButton.active=runtime.playlistEdits().canRedo(runtime.library());}
        if(songs==null)return;Playlist p=playlist();if(p==null)return;
        Set<String> vanilla=runtime.applicableVanilla();
        if(!runtime.library().settings.songColumns.contains(sortColumn)){sortColumn=SongColumn.TITLE;descending=false;}
        Comparator<Track> comparator=sortColumn.comparator(descending);
        songRows=runtime.library().pool(p,vanilla,true).stream().filter(t->songQuery.isBlank()||contains(t,songQuery)).sorted(comparator.thenComparing(t->t.path)).toList();songs.items(songRows);
        columnHeaders=new String[songTable.columns.length];
        for(int i=0;i<columnHeaders.length;i++)columnHeaders[i]=fit(songTable.columns[i].label+(songTable.columns[i]==sortColumn?(descending?" v":" ^"):""),songTable.widths[i]-7);
        songCells.clear();
        for(var track:songRows){
            String[] cells=new String[songTable.columns.length];
            for(int i=0;i<cells.length;i++)cells[i]=fit(songTable.columns[i].text(track),songTable.widths[i]-7);
            songCells.put(track.id,cells);
        }
    }
    private List<TreeRow> buildTree(){
        List<String> paths=runtime.library().folders.stream().map(f->f.path).distinct().toList();
        if(treeRevision!=runtime.catalogRevision()||!treeFolders.equals(paths)){
            rebuildTree();treeRevision=runtime.catalogRevision();treeFolders=paths;treeFilter=null;
        }
        String query=libraryQuery.toLowerCase(Locale.ROOT);
        if(!query.equals(treeFilter)){for(Node root:treeRoots)markMatches(root,query);treeFilter=query;}
        List<TreeRow> result=new ArrayList<>();for(Node root:treeRoots)flatten(root,0,result);return result;
    }
    private void rebuildTree(){
        treeRoots.clear();
        List<Track> local=runtime.library().tracks.stream().filter(t->!t.vanilla).toList();
        Node all=new Node("all","All local music ("+local.size()+")");
        for(Track t:local){Node n=trackNode(t,Path.of(t.path).getFileName().toString());all.children.put(t.title+"\0"+t.id,n);}treeRoots.add(all);
        Set<String> paths=new HashSet<>();
        for(Folder folder:runtime.library().folders){
            if(!paths.add(folder.path))continue;Path base=Path.of(folder.path).toAbsolutePath().normalize();
            Node root=new Node("folder:"+base,base.getFileName()==null?base.toString():base.getFileName().toString());
            for(Track t:local){
                Path path=Path.of(t.path).toAbsolutePath().normalize();if(!path.startsWith(base))continue;Path relative=base.relativize(path);Node parent=root;Path accumulated=base;
                for(int i=0;i<relative.getNameCount()-1;i++){
                    String part=relative.getName(i).toString();accumulated=accumulated.resolve(part);String key="dir:"+part;Node next=parent.children.get(key);
                    if(next==null){next=new Node("folder:"+accumulated,part);parent.children.put(key,next);}parent=next;
                }
                Node n=trackNode(t,path.getFileName().toString());parent.children.put("file:"+t.title+"\0"+t.id,n);
            }
            treeRoots.add(root);
        }
        Node mc=new Node("vanilla","Minecraft soundtrack");for(Track t:runtime.library().tracks)if(t.vanilla){Node n=trackNode(t,t.title);mc.children.put(t.title+"\0"+t.id,n);}treeRoots.add(mc);
    }
    private Node trackNode(Track t,String label){Node n=new Node(t.id,label);n.track=t;n.search=(t.title+" "+t.artist+" "+t.album+" "+t.albumArtist+" "+t.genre+" "+t.date+" "+t.path).toLowerCase(Locale.ROOT);return n;}
    private boolean markMatches(Node n,String query){
        boolean matches=query.isBlank()||(n.track==null?n.label.toLowerCase(Locale.ROOT):n.search).contains(query);
        for(Node child:n.children.values())if(markMatches(child,query))matches=true;
        n.matches=matches;return matches;
    }
    private void flatten(Node n,int depth,List<TreeRow> out){
        if(!n.matches)return;boolean group=n.track==null,open=expanded.contains(n.id)||!libraryQuery.isBlank();
        out.add(new TreeRow(n.id,n.label,depth,n.track,group,open));if(group&&open)n.children.values().forEach(c->flatten(c,depth+1,out));
    }
    private void clickFolder(TreeRow row,double clickX,double clickY,int button,boolean twice){
        if(row.group){if(!expanded.remove(row.id))expanded.add(row.id);folderSelection=row.id;librarySelection.clear();refreshFolders();return;}
        if(Screen.hasShiftDown())choose(librarySelection,row.track.id,libraryAnchor,treeRows.stream().filter(r->r.track!=null).map(r->r.track.id).toList(),button);
        else choose(librarySelection,row.track.id,libraryAnchor,List.of(),button);
        libraryAnchor=row.track.id;folderSelection="";inspected=row.track;if(twice)addSelection();
    }
    private void clickSong(Track track,double clickX,double clickY,int button,boolean twice){
        choose(songSelection,track.id,songAnchor,Screen.hasShiftDown()?songRows.stream().map(t->t.id).toList():List.of(),button);songAnchor=track.id;inspected=track;
        if(clickX<tableX+28){runtime.playlistEdits().assign(runtime.library(),playlist(),List.of(track.id),!playlist().tracks.getOrDefault(track.id,true));runtime.save();refreshSongs();}
    }
    private void choose(Set<String> selection,String id,String anchor,List<String> order,int button){
        if(Screen.hasShiftDown()&&!anchor.isBlank()){int a=order.indexOf(anchor),b=order.indexOf(id);if(a>=0&&b>=0){if(!Screen.hasControlDown())selection.clear();selection.addAll(order.subList(Math.min(a,b),Math.max(a,b)+1));return;}}
        if(Screen.hasControlDown()){if(!selection.remove(id))selection.add(id);}else{selection.clear();selection.add(id);}
    }
    private void addSelection(){
        Set<String> ids=new LinkedHashSet<>(librarySelection);
        if(ids.isEmpty()&&!folderSelection.isBlank()){
            if(folderSelection.equals("all"))runtime.library().tracks.stream().filter(t->!t.vanilla).forEach(t->ids.add(t.id));
            else if(folderSelection.equals("vanilla"))runtime.library().tracks.stream().filter(t->t.vanilla).forEach(t->ids.add(t.id));
            else if(folderSelection.startsWith("folder:")){Path f=Path.of(folderSelection.substring(7));runtime.library().tracks.stream().filter(t->!t.vanilla&&Path.of(t.path).startsWith(f)).forEach(t->ids.add(t.id));}
        }
        runtime.playlistEdits().assign(runtime.library(),playlist(),ids,true);
        runtime.save();runtime.message(ids.size()+" added / "+playlist().name);refreshSongs();
    }
    private void setSongs(boolean enabled){runtime.playlistEdits().assign(runtime.library(),playlist(),songSelection,enabled);runtime.save();refreshSongs();}
    private void previewSelected(){if(inspected!=null)runtime.preview(inspected);}
    private void queueSelected(){
        int added=0;
        for(var track:songRows)if(songSelection.contains(track.id)&&runtime.queueTrack(track.id,playlistId))added++;
        runtime.message(added+" queued"+(songSelection.size()>added?" / "+(songSelection.size()-added)+" unavailable or queue full":""));
    }
    private void drawFolder(PoseStack g,TreeRow r,int x,int y,int w,boolean hover){
        boolean selected=r.group?folderSelection.equals(r.id):librarySelection.contains(r.id);if(selected||hover)net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,selected?MenuAppearance.color(MenuColor.SELECTION,0xFF304963):MenuAppearance.color(MenuColor.HOVER,0xFF2C303E));
        menuFont.draw(g,fit((r.group?(r.open?"v ":"> "):"  ")+r.label,w-r.depth*10-8),x+4+r.depth*10,y+3,r.group?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFB5CCD3):MenuAppearance.color(MenuColor.TEXT,0xFFDFE1E7));
    }
    private void drawSong(PoseStack g,Track t,int x,int y,int w,boolean hover){
        if(songSelection.contains(t.id)||hover)net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,songSelection.contains(t.id)?MenuAppearance.color(MenuColor.SELECTION,0xFF304963):MenuAppearance.color(MenuColor.HOVER,0xFF2C303E));
        Boolean explicit=playlist().tracks.get(t.id);int color=Boolean.FALSE.equals(explicit)?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF7A808F):MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED);
        menuFont.draw(g,explicit==null?"auto":explicit?"on":"off",x+3,y+3,Boolean.FALSE.equals(explicit)?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF7A808F):MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        if(runtime.engine().current()!=null&&t.id.equals(runtime.engine().current().id))PlaybackIndicator.draw(g,x+30,y+2,runtime.engine().paused());
        if(y>=songs.y&&y+14<=songs.y+songs.getHeight()&&(hover||actionSong==null&&(songs.isFocused()||songActions.isFocused())&&t.id.equals(songAnchor))){actionSong=t;songActions.y=y;songActions.visible=true;}
        var cells=songCells.get(t.id);if(cells==null)return;
        var window=minecraft.getWindow();double pixels=window.getGuiScale();
        int clipX=(int)Math.floor((x+SongTableLayout.PINNED)*scale*pixels);
        int clipRight=(int)Math.floor((x+SongTableLayout.PINNED+songTable.viewport)*scale*pixels);
        int clipY=(int)Math.floor(Math.max(y,songs.y)*scale*pixels);
        int clipBottom=(int)Math.ceil(Math.min(y+14,songs.y+songs.getHeight())*scale*pixels);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(clipX,window.getHeight()-clipBottom,Math.max(0,clipRight-clipX),Math.max(0,clipBottom-clipY));
        for(int i=0;i<cells.length;i++){
            int cx=x+songTable.x(i);
            if(cx+songTable.widths[i]>x+SongTableLayout.PINNED&&cx<x+SongTableLayout.PINNED+songTable.viewport)menuFont.draw(g,cells[i],cx+4,y+3,color);
        }
        int outerX=(int)Math.floor(songs.x*scale*pixels),outerY=(int)Math.floor(songs.y*scale*pixels);
        int outerRight=(int)Math.ceil((songs.x+songs.getWidth()-8)*scale*pixels),outerBottom=(int)Math.ceil((songs.y+songs.getHeight())*scale*pixels);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(outerX,window.getHeight()-outerBottom,outerRight-outerX,outerBottom-outerY);
    }
    private String fit(String text,int w){return menuFont.plainSubstrByWidth(text,Math.max(0,w));}
    private static String duration(double s){int seconds=(int)s%60;return s<=0?"--:--":(int)(s/60)+":"+(seconds<10?"0":"")+seconds;}
    @Override public void tick(){
        if(repairReport!=runtime.sourceHealth()){repairReport=runtime.sourceHealth();updateRepairButton();}
        if(settingsPanel!=null)settingsPanel.tick();
        if(undoButton!=null){undoButton.active=runtime.playlistEdits().canUndo(runtime.library())||settingsPanel.invalidDraft();redoButton.active=runtime.playlistEdits().canRedo(runtime.library())&&!settingsPanel.invalidDraft();}
        if(priorityDrag.dragging()&&System.nanoTime()>=nextDragScroll&&priorityDrag.x()>=priorities.x&&priorityDrag.x()<priorities.x+priorities.getWidth()-8){
            int direction=priorityDrag.y()<priorities.y+12?-1:priorityDrag.y()>priorities.y+priorities.getHeight()-12?1:0;
            if(direction!=0){priorities.offset(priorities.offset()+direction);priorityDrag.update(priorityDrag.x(),priorityDrag.y(),priorities.y,priorities.offset(),runtime.library().playlists.size());nextDragScroll=System.nanoTime()+80_000_000L;}
        }
        if(songRevision!=runtime.catalogRevision()||wasScanning!=runtime.scanning()){songRevision=runtime.catalogRevision();wasScanning=runtime.scanning();refresh();}
        updateTransport();
        art.track(runtime.engine().current());
        updatePlaybackLabel();
        if(addPlaying!=null)addPlaying.active=runtime.engine().current()!=null;
    }
    private void updatePlaybackLabel(){playbackLabel=dev.animal.afmr.core.PlaybackStatus.label(runtime.engine(),runtime.library().settings,runtime.inWorld(),runtime.context().dead(),runtime.playbackUnavailable(),runtime.now());}
    private void updateRepairButton(){
        int missing=repairReport==null?0:repairReport.issues().size();
        repairButton.setMessage(new net.minecraft.network.chat.TextComponent(missing==0?"Repair / missing":"Missing: "+missing));
        repairButton.setLabelTextColor(missing==0?null:0xFFFF9A91);
    }
    @Override public void render(PoseStack g,int mx,int my,float dt){
        runtime.menuBackground().draw(g,width,height,1);
        g.pushPose();g.scale(scale,scale,1);
        int footer=layout.footerY(),rx=layout.rulesX(),rw=layout.rulesWidth();
        for(var surface:backgrounds)net.minecraft.client.gui.GuiComponent.fill(g,surface.x0(),surface.y0(),surface.x1(),surface.y1(),MenuAppearance.background(surface.color(),surface.rgb()));
        net.minecraft.client.gui.GuiComponent.fill(g,split,64,split+1,footer,MenuAppearance.color(MenuColor.BORDERS,0xFF3B424C));
        menuFont.draw(g,fit("AFMR / "+runtime.library().profiles.activeName,vw-500),12,6,MenuAppearance.color(MenuColor.TEXT,0xFFEFF0F3));
        menuFont.draw(g,"PRIORITY",12,76,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        menuFont.draw(g,"Music library",12,layout.libraryY(),MenuAppearance.color(MenuColor.TEXT,0xFFEFF0F3));
        menuFont.draw(g,fit(playlist().name,tableWidth),tableX,78,MenuAppearance.color(MenuColor.TEXT,0xFFEFF0F3));
        menuFont.draw(g,"Playlist Settings",rx+12,82,MenuAppearance.color(MenuColor.TEXT,0xFFEFF0F3));
        settingsPanel.draw(g);
        menuFont.draw(g,"On",tableX+3,layout.songHeaderY()+5,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        var window=minecraft.getWindow();double pixels=window.getGuiScale();
        int clipX=(int)Math.floor((tableX+SongTableLayout.PINNED)*scale*pixels);
        int clipRight=(int)Math.floor((tableX+SongTableLayout.PINNED+songTable.viewport)*scale*pixels);
        int clipY=(int)Math.floor(layout.songHeaderY()*scale*pixels),clipBottom=(int)Math.ceil(layout.songListY()*scale*pixels);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(clipX,window.getHeight()-clipBottom,clipRight-clipX,clipBottom-clipY);
        for(int i=0;i<columnHeaders.length;i++)menuFont.draw(g,columnHeaders[i],tableX+songTable.x(i)+4,layout.songHeaderY()+5,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
        if(songTable.overflow()){
            int barX=tableX+SongTableLayout.PINNED,barY=columnBarY();
            net.minecraft.client.gui.GuiComponent.fill(g,tableX,barY,tableX+tableWidth,barY+SongTableLayout.BAR,MenuAppearance.background(MenuColor.CONTROLS,0x15141B));
            net.minecraft.client.gui.GuiComponent.fill(g,barX+songTable.thumbX(),barY+2,barX+songTable.thumbX()+songTable.thumbWidth(),barY+8,MenuAppearance.color(MenuColor.SCROLLBARS,0xFF66778B));
        }
        net.minecraft.client.gui.GuiComponent.fill(g,0,footer,vw,footer+1,MenuAppearance.color(MenuColor.BORDERS,0xFF3B424C));
        art.draw(g,12,footer+12,42);
        Track t=runtime.engine().current();
        int infoWidth=vw/2-185;
        String state=playbackLabel;
        menuFont.draw(g,fit(t==null?(runtime.minecraftMusicMuted()?playbackLabel:""):t.artist+" / "+duration(t.durationSeconds),infoWidth),64,footer+29,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        menuFont.draw(g,t==null?"":state,64,footer+44,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        var active=runtime.engine().selected();
        menuFont.draw(g,fit((runtime.engine().overrideId()==null?"Automatic / ":"Override / ")+(active==null?"None":active.name),220),vw-232,footer+14,MenuAppearance.color(MenuColor.ACCENT,0xFFBCBDF8));
        var source=runtime.library().playlist(runtime.engine().sourcePlaylistId());
        menuFont.draw(g,fit("Source / "+(source==null?runtime.engine().previewing()?"Preview":"None":source.name),220),vw-232,footer+29,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        if(runtime.scanning())ScanProgressView.draw(g,menuFont,runtime.scanProgress(),500,0,vw-592);
        playlistActions.visible=false;actionPlaylist=null;
        songActions.visible=false;actionSong=null;
        super.render(g,(int)(mx/scale),(int)(my/scale),dt);
        if(!playlistActions.visible&&playlistActions.isFocused()){if(playlistActions.isFocused())playlistActions.changeFocus(true);setFocused(priorities);}
        if(!songActions.visible&&songActions.isFocused()){if(songActions.isFocused())songActions.changeFocus(true);setFocused(songs);}
        menuFont.draw(g,fit(runtime.status(),vw-24),12,vh-12,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        if(priorityDrag.dragging()){
            int line=Math.max(priorities.y,Math.min(priorities.y+priorities.getHeight()-2,priorities.y+(priorityDrag.slot()-priorities.offset())*DenseList.ROW));
            net.minecraft.client.gui.GuiComponent.fill(g,priorities.x,line,priorities.x+priorities.getWidth()-8,line+2,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        }
        g.translate(0,0,400);
        for(var button:menuButtons)button.render(g,(int)(mx/scale),(int)(my/scale),dt);g.popPose();
            descriptions.draw(this,g,mx,my);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        runtime.playlistEdits().checkpoint(runtime.library());runtime.playlistEdits().separate();
        double tx=x/scale,ty=y/scale;
        if(!menuButtons.isEmpty()){
            for(var control:List.copyOf(menuButtons))if(control.mouseClicked(tx,ty,button))return true;
            menuButtons.clear();return true;
        }
        if(button==1){
            var target=priorities.itemAt(tx,ty);
            if(target!=null){showPlaylistMenu(target,(int)tx,(int)ty);return true;}
            var song=songs.itemAt(tx,ty);if(song!=null){showSongMenu(song,(int)tx,(int)ty);return true;}
            return false;
        }
        if(button==InputConstants.MOUSE_BUTTON_LEFT){var target=priorities.itemAt(tx,ty);if(target!=null&&tx>=playlistActions.x&&tx<playlistActions.x+playlistActions.getWidth()){
            playlistActions.playDownSound(minecraft.getSoundManager());showPlaylistMenu(target,playlistActions.x,(int)ty+14);return true;
        }}
        if(button==0){var target=priorities.itemAt(tx,ty);if(target!=null){
            if(settingsPanel!=null&&!settingsPanel.commit())return true;priorities.playDownSound(minecraft.getSoundManager());selectPlaylist(target.id);init(minecraft,width,height);priorities.reveal(target);priorityDrag.begin(target.id,tx,ty);setFocused(priorities);return true;
        }}
        if(button==InputConstants.MOUSE_BUTTON_LEFT){var target=songs.itemAt(tx,ty);if(target!=null&&tx>=songActions.x&&tx<songActions.x+songActions.getWidth()){
            songActions.playDownSound(minecraft.getSoundManager());showSongMenu(target,songActions.x,(int)ty+14);return true;
        }}
        if(button==0&&onColumnBar(tx,ty)){
            double position=tx-tableX-SongTableLayout.PINNED;
            columnDragGrab=position>=songTable.thumbX()&&position<=songTable.thumbX()+songTable.thumbWidth()?position-songTable.thumbX():songTable.thumbWidth()/2d;
            draggingColumns=true;songTable.dragThumb(position-columnDragGrab);setFocused(songs);return true;
        }
        if(button==0&&ty>=layout.songHeaderY()&&ty<layout.songListY()&&tx>=tableX&&tx<tableX+tableWidth){
            int chosen=songTable.columnAt(tx-tableX);
            if(chosen>=0){var column=songTable.columns[chosen];if(sortColumn==column)descending=!descending;else{sortColumn=column;descending=false;}refreshSongs();}
            return true;
        }
        return super.mouseClicked(tx,ty,button);
    }
    private int columnBarY(){return layout.songListY()+layout.songListHeight()-SongTableLayout.BAR;}
    private boolean onColumnBar(double x,double y){return songTable.overflow()&&x>=tableX+SongTableLayout.PINNED&&x<tableX+tableWidth-8&&y>=columnBarY()&&y<columnBarY()+SongTableLayout.BAR;}
    @Override public boolean mouseReleased(double x,double y,int button){if(priorityDrag.armed()){
        if(priorityDrag.dragging()&&x/scale>=priorities.x&&x/scale<priorities.x+priorities.getWidth()-8&&y/scale>=priorities.y&&y/scale<priorities.y+priorities.getHeight()){
            var p=runtime.library().playlist(priorityDrag.id());if(p!=null&&runtime.playlistEdits().move(runtime.library(),p.id,priorityDrag.target(runtime.library().playlists.indexOf(p)))){runtime.save();init(minecraft,width,height);priorities.reveal(p);}
        }priorityDrag.cancel();return true;}
        if(draggingColumns){draggingColumns=false;return true;}return super.mouseReleased(x/scale,y/scale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(priorityDrag.armed()){priorityDrag.update(x/scale,y/scale,priorities.y,priorities.offset(),runtime.library().playlists.size());return true;}
        if(draggingColumns){songTable.dragThumb(x/scale-tableX-SongTableLayout.PINNED-columnDragGrab);return true;}
        return super.mouseDragged(x/scale,y/scale,button,dx/scale,dy/scale);
    }
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/scale,y/scale);}
    @Override public boolean mouseScrolled(double x,double y,double delta){
        double tx=x/scale,ty=y/scale;
        if(!menuButtons.isEmpty())return true;
        if(settingsPanel!=null&&settingsPanel.scroll(tx,ty,delta))return true;
        if(songTable.overflow()&&(onColumnBar(tx,ty)||hasShiftDown()&&tx>=tableX&&tx<tableX+tableWidth&&ty>=layout.songHeaderY()&&ty<columnBarY())){
            songTable.scroll(songTable.scroll()-(int)Math.round(delta*40));return true;
        }
        return super.mouseScrolled(tx,ty,delta);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==InputConstants.KEY_ESCAPE&&seekBar!=null&&seekBar.cancelDrag())return true;
        if(priorityDrag.armed()&&key==InputConstants.KEY_ESCAPE){priorityDrag.cancel();return true;}
        if(!(getFocused() instanceof MenuField)&&hasControlDown()&&(key==InputConstants.KEY_Z||key==InputConstants.KEY_Y)){restoreEdit(key==InputConstants.KEY_Y||hasShiftDown());return true;}

        if(!menuButtons.isEmpty()){
            if(key==InputConstants.KEY_ESCAPE){menuButtons.clear();return true;}
            if(key==InputConstants.KEY_UP||key==InputConstants.KEY_DOWN){if(menuButtons.get(menuFocus).isFocused())menuButtons.get(menuFocus).changeFocus(true);menuFocus=Math.floorMod(menuFocus+(key==InputConstants.KEY_DOWN?1:-1),menuButtons.size());if(!menuButtons.get(menuFocus).isFocused())menuButtons.get(menuFocus).changeFocus(true);return true;}
            menuButtons.get(menuFocus).keyPressed(key,scan,modifiers);return true;
        }
        if(getFocused()==priorities&&key==InputConstants.KEY_F2){if(settingsPanel.commit())minecraft.setScreen(new RenamePlaylistScreen(this,runtime,playlist()));return true;}

        if((getFocused()==priorities||getFocused()==playlistActions)&&key==InputConstants.KEY_F10&&(modifiers&org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT)!=0){
            showPlaylistMenu(getFocused()==playlistActions?actionPlaylist:playlist(),playlistActions.x,playlistActions.y+14);return true;
        }
        if((getFocused()==songs||getFocused()==songActions)&&key==InputConstants.KEY_F10&&(modifiers&org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT)!=0){var track=runtime.library().track(songAnchor);if(track!=null&&songCells.containsKey(track.id)){showSongMenu(track,songActions.x,songs.y+14);return true;}}
        if(getFocused()!=librarySearch&&getFocused()!=songSearch){
            if(getFocused()==songs&&songTable.overflow()&&(key==InputConstants.KEY_LEFT||key==InputConstants.KEY_RIGHT)){
                songTable.scroll(songTable.scroll()+(key==InputConstants.KEY_RIGHT?60:-60));return true;
            }
            if(Screen.isSelectAll(key)&&(getFocused()==folders||getFocused()==songs)){if(getFocused()==folders)treeRows.stream().filter(r->r.track!=null).forEach(r->librarySelection.add(r.track.id));else songRows.forEach(t->songSelection.add(t.id));return true;}
            if(getFocused()==songs&&key==InputConstants.KEY_SPACE&&!songSelection.isEmpty()){setSongs(songSelection.stream().anyMatch(id->Boolean.FALSE.equals(playlist().tracks.get(id))));return true;}
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void onFilesDrop(List<Path> paths){
        if(paths.stream().anyMatch(dev.animal.afmr.library.ConfigBackup::supported)){
            if(paths.size()!=1){runtime.message("Drop one .afmrb backup at a time.");return;}
            runtime.prepareBackupImport(paths.get(0));
            minecraft.setScreen(new ConfigBackupScreen(this,runtime));return;
        }
        if(paths.stream().anyMatch(dev.animal.afmr.library.PlaylistPresets::supported)){
            if(paths.size()!=1){runtime.message("Drop one .afmrp setup at a time, separately from music files.");return;}
            runtime.preparePresetImport(paths.get(0),runtime.library().profiles.activeId,playlistId);
            minecraft.setScreen(new PlaylistSharingScreen(this,runtime,playlistId));return;
        }
        if(paths.stream().allMatch(dev.animal.afmr.library.FpliteFiles::supported))runtime.importPlaylists(paths,runtime.library().profiles.activeId,playlistId);
        else if(paths.stream().anyMatch(dev.animal.afmr.library.FpliteFiles::supported))runtime.message("Drop playlist files and music files in separate batches.");
        else runtime.importPaths(paths,runtime.library().profiles.activeId,"library",true);
    }
    @Override public void onClose(){if(settingsPanel!=null&&!settingsPanel.commit())return;MusicMenu.remember(this);minecraft.setScreen(parent);}
    @Override public void removed(){if(seekBar!=null)seekBar.cancel();priorityDrag.cancel();draggingColumns=false;menuButtons.clear();if(settingsPanel!=null){if(settingsPanel.commit())settingsPanel.clearDrafts();settingsPanel.flush();settingsPanel.cancelHold();}art.close();}
    @Override public boolean isPauseScreen(){return false;}
}

package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.SongColumn;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;

/** Playlist workspace. All model mutations remain on the client thread. */
public final class LibraryScreen extends Screen {
    Screen parent;private final ClientRuntime runtime;
    private float scale;private int vw,vh,split,tableX,tableWidth;
    private SongTableLayout songTable;
    private String[] columnHeaders;
    private final Map<String,String[]> songCells=new HashMap<>();
    private boolean draggingColumns;
    private double columnDragGrab;
    private WorkspaceLayout layout;
    private DenseList<Playlist> priorities;
    private final Map<String,Integer> ranks=new HashMap<>();
    private final EnumMap<WorkspacePage,MusicScreen> pages=new EnumMap<>(WorkspacePage.class);
    private FlatButton addPlaying,pause,volumeButton,silenceButton,repairButton;
    private dev.animal.afmr.library.LibraryHealth.Report repairReport;
    private AutoplayHereButton autoplayHere;
    private String conditionSummary="";
    private String playlistId="day_outside",libraryQuery="",songQuery="",folderSelection="";
    private final Set<String> expanded=new HashSet<>();
    private boolean initialTree=true;
    private final Set<String> librarySelection=new LinkedHashSet<>(),songSelection=new LinkedHashSet<>();
    private String libraryAnchor="",songAnchor="";
    private Track inspected;private final EditorArtwork art=new EditorArtwork();
    private DenseList<TreeRow> folders;private DenseList<Track> songs;
    private List<TreeRow> treeRows=List.of();private List<Track> songRows=List.of();
    private SongColumn sortColumn=SongColumn.TITLE;private boolean descending,wasScanning;private long songRevision=-1;
    private EditBox librarySearch,songSearch;
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
    public LibraryScreen(Screen parent,ClientRuntime runtime,String playlistId){super(Component.literal("animal's final music replacer"));this.parent=parent;this.runtime=runtime;this.playlistId=playlistId;}
    private Playlist playlist(){return runtime.library().playlist(playlistId);}
    void selectPlaylist(String id){playlistId=id;songSelection.clear();songQuery="";if(songs!=null)songs.top();}
    @Override protected void init(){
        int folderOffset=folders==null?0:folders.offset(),songOffset=songs==null?0:songs.offset();
        int priorityOffset=priorities==null?0:priorities.offset();
        layout=WorkspaceLayout.fit(width,height);scale=layout.scale();vw=layout.width();vh=layout.height();
        split=layout.sidebarWidth();tableX=layout.tableX();tableWidth=layout.tableWidth();
        backgrounds=new ArrayList<>();backgrounds.add(new MenuSurface(0,0,vw,vh,MenuColor.BACKGROUND,0x141B1B));
        for(var panel:List.of(new MenuSurface(0,0,vw,64,MenuColor.PANELS,0x1D2727),
            new MenuSurface(0,64,split,layout.footerY(),MenuColor.PANELS,0x1D2727),
            new MenuSurface(layout.rulesX(),74,layout.rulesX()+layout.rulesWidth(),layout.footerY()-12,MenuColor.PANELS,0x1D2727),
            new MenuSurface(tableX,layout.songHeaderY(),tableX+tableWidth,layout.songListY(),MenuColor.PANELS,0x253232),
            new MenuSurface(0,layout.footerY(),vw,vh,MenuColor.PANELS,0x1D2727))){
            MenuSurface.cutOut(backgrounds,panel.x0(),panel.y0(),panel.x1(),panel.y1());backgrounds.add(panel);
        }
        MenuSurface.cutOut(backgrounds,12,94,split-12,94+layout.priorityHeight());
        MenuSurface.cutOut(backgrounds,12,layout.libraryListY(),split-12,layout.libraryListY()+layout.libraryListHeight());
        MenuSurface.cutOut(backgrounds,tableX,layout.songListY(),tableX+tableWidth,layout.songListY()+layout.songListHeight());
        songTable=new SongTableLayout(runtime.library().settings.songColumns,tableWidth-8,songTable==null?0:songTable.scroll());
        draggingColumns=false;
        if(initialTree){initialTree=false;expanded.add(runtime.library().folders.isEmpty()?"all":"folder:"+Path.of(runtime.library().folders.getFirst().path).toAbsolutePath().normalize());}
        if(runtime.library().playlists.isEmpty()){var p=runtime.library().createPlaylist("New playlist");selectPlaylist(p.id);runtime.save();}
        if(playlist()==null)selectPlaylist(runtime.library().playlists.getFirst().id);
        int navX=12;
        for(WorkspacePage page:WorkspacePage.values()){
            var tab=flat(page.label,navX,30,page.width,22,()->openPage(page));tab.selected=page==WorkspacePage.PLAYLISTS;navX+=page.width+4;
        }
        flat("Import / export",navX,30,126,22,()->minecraft.gui.setScreen(new PlaylistSharingScreen(this,runtime,playlistId)));
        flat("Done",vw-72,30,60,22,this::onClose);
        repairReport=runtime.sourceHealth();
        repairButton=flat("Repair / missing",vw-212,30,130,22,()->minecraft.gui.setScreen(new LibraryRepairScreen(this,runtime)));
        updateRepairButton();
        priorities=addRenderableWidget(new DenseList<>(12,94,split-24,layout.priorityHeight(),this::drawPriority,(p,e,twice)->{
            selectPlaylist(p.id);rebuildWidgets();priorities.reveal(p);
        },p->p.name));
        ranks.clear();for(int i=0;i<runtime.library().playlists.size();i++)ranks.put(runtime.library().playlists.get(i).id,i+1);
        priorities.items(runtime.library().playlists);
        int controlsY=100+layout.priorityHeight(),quarter=(split-36)/4;
        int rank=runtime.library().playlists.indexOf(playlist());
        flat("Top",12,controlsY,quarter,22,()->movePlaylist(0)).active=rank>0;
        flat("Up",16+quarter,controlsY,quarter,22,()->movePlaylist(rank-1)).active=rank>0;
        flat("Down",20+2*quarter,controlsY,quarter,22,()->movePlaylist(rank+1)).active=rank<runtime.library().playlists.size()-1;
        flat("Last",24+3*quarter,controlsY,quarter,22,()->movePlaylist(runtime.library().playlists.size()-1)).active=rank<runtime.library().playlists.size()-1;
        int third=(split-32)/3;
        flat("New",12,controlsY+26,third,22,()->minecraft.gui.setScreen(new NewPlaylistScreen(this,runtime,null)));
        var clonePlaylistHelpControl=flat("Clone",16+third,controlsY+26,third,22,()->minecraft.gui.setScreen(new NewPlaylistScreen(this,runtime,playlist())));
        var clonePlaylistHelpText=Component.translatable("afmr.copy.library.clone_playlist");
        if(!clonePlaylistHelpText.getString().isBlank())clonePlaylistHelpControl.setTooltip(Tooltip.create(clonePlaylistHelpText));
        flat("Delete",20+2*third,controlsY+26,third,22,this::deletePlaylist).active=!runtime.scanning();
        var importIntoLibraryHelpControl=flat("+",split-38,layout.libraryY()-5,26,20,()->minecraft.gui.setScreen(new PlaylistImportScreen(this,runtime,"library")));
        var importIntoLibraryHelpText=Component.translatable("afmr.copy.library.import_into_library");
        if(!importIntoLibraryHelpText.getString().isBlank())importIntoLibraryHelpControl.setTooltip(Tooltip.create(importIntoLibraryHelpText));
        librarySearch=search(libraryQuery,"Search music",12,layout.libraryY()+18,split-24,v->{libraryQuery=v;folders.top();refreshFolders();});
        folders=addRenderableWidget(new DenseList<>(12,layout.libraryListY(),split-24,layout.libraryListHeight(),this::drawFolder,this::clickFolder,r->r.label));
        var addSelectedOrFolderHelpControl=flat("Add selected / folder ->",12,layout.footerY()-26,split-24,22,this::addSelection);
        var addSelectedOrFolderHelpText=Component.translatable("afmr.copy.library.add_selected_or_folder");
        if(!addSelectedOrFolderHelpText.getString().isBlank())addSelectedOrFolderHelpControl.setTooltip(Tooltip.create(addSelectedOrFolderHelpText));
        var playOneSongHelpControl=flat("Play one song",tableX,104,138,22,()->runtime.overridePlaylist(playlistId,OverrideMode.ONE_SONG));
        var playOneSongHelpText=Component.translatable("afmr.copy.library.play_one_song");
        if(!playOneSongHelpText.getString().isBlank())playOneSongHelpControl.setTooltip(Tooltip.create(playOneSongHelpText));
        var untilReleasedHelpControl=flat("Until released",tableX+144,104,138,22,()->runtime.overridePlaylist(playlistId,OverrideMode.UNTIL_RELEASED));
        var untilReleasedHelpText=Component.translatable("afmr.copy.library.until_released");
        if(!untilReleasedHelpText.getString().isBlank())untilReleasedHelpControl.setTooltip(Tooltip.create(untilReleasedHelpText));
        songSearch=search(songQuery,"Search playlist",tableX,138,tableWidth-184,v->{songQuery=v;songs.top();refreshSongs();});
        var songColumnsHelpControl=flat("Columns",tableX+tableWidth-178,136,74,22,()->minecraft.gui.setScreen(new SongColumnsScreen(this,runtime)));
        var songColumnsHelpText=Component.translatable("afmr.copy.library.song_columns");
        if(!songColumnsHelpText.getString().isBlank())songColumnsHelpControl.setTooltip(Tooltip.create(songColumnsHelpText));
        flat("Add music",tableX+tableWidth-98,136,98,22,()->minecraft.gui.setScreen(new PlaylistImportScreen(this,runtime,playlistId)));
        songs=addRenderableWidget(new DenseList<>(tableX,layout.songListY(),tableWidth,layout.songListHeight()-(songTable.overflow()?SongTableLayout.BAR:0),this::drawSong,this::clickSong,t->t.title));
        int actionY=layout.footerY()-32,bx=tableX;
        var previewHelpControl=flat("Preview",bx,actionY,76,22,this::previewSelected);
        var previewHelpText=Component.translatable("afmr.copy.library.preview");
        if(!previewHelpText.getString().isBlank())previewHelpControl.setTooltip(Tooltip.create(previewHelpText));bx+=82;
        var queueHelpControl=flat("Queue",bx,actionY,76,22,this::queueSelected);
        var queueHelpText=Component.translatable("afmr.copy.library.queue");
        if(!queueHelpText.getString().isBlank())queueHelpControl.setTooltip(Tooltip.create(queueHelpText));bx+=82;
        flat("Enable",bx,actionY,64,22,()->setSongs(true));bx+=70;
        var disableSongsHelpControl=flat("Disable",bx,actionY,64,22,()->setSongs(false));
        var disableSongsHelpText=Component.translatable("afmr.copy.library.disable_songs");
        if(!disableSongsHelpText.getString().isBlank())disableSongsHelpControl.setTooltip(Tooltip.create(disableSongsHelpText));bx+=70;
        var removeAssignmentsHelpControl=flat("Remove",bx,actionY,64,22,()->{songSelection.forEach(id->playlist().tracks.remove(id));runtime.save();refreshSongs();});
        var removeAssignmentsHelpText=Component.translatable("afmr.copy.library.remove_assignments");
        if(!removeAssignmentsHelpText.getString().isBlank())removeAssignmentsHelpControl.setTooltip(Tooltip.create(removeAssignmentsHelpText));bx+=70;
        var normalizePlaylistHelpControl=flat("Normalize",bx,actionY,90,22,()->{
            if(runtime.normalizePlaylist(playlistId))minecraft.gui.setScreen(new LoudnessScreen(this,runtime,playlistId));
        });
        var normalizePlaylistHelpText=Component.translatable("afmr.copy.library.normalize_playlist");
        if(!normalizePlaylistHelpText.getString().isBlank())normalizePlaylistHelpControl.setTooltip(Tooltip.create(normalizePlaylistHelpText));
        buildRules();
        int transportX=vw/2-105,transportY=layout.footerY()+15;
        pause=flat(runtime.engine().paused()?"Resume":"Pause",transportX,transportY,66,22,runtime::togglePause);
        addPlaying=flat("Add playing to playlist",transportX,transportY+26,228,22,()->{
            var track=runtime.engine().current();if(track!=null)minecraft.gui.setScreen(new TrackPlaylistScreen(this,runtime,track.id,track.title));
        });addPlaying.active=runtime.engine().current()!=null;
        var pauseAndResumeHelpText=Component.translatable("afmr.copy.library.pause_and_resume");
        if(!pauseAndResumeHelpText.getString().isBlank())pause.setTooltip(Tooltip.create(pauseAndResumeHelpText));
        var nextHelpControl=flat("Next",transportX+72,transportY,62,22,runtime::nextSong);
        var nextHelpText=Component.translatable("afmr.copy.library.next");
        if(!nextHelpText.getString().isBlank())nextHelpControl.setTooltip(Tooltip.create(nextHelpText));
        var skipOrPlayHelpControl=flat("Skip / play",transportX+140,transportY,88,22,()->{if(runtime.engine().current()==null)runtime.playNow();else runtime.skip();});
        var skipOrPlayHelpText=Component.translatable("afmr.copy.library.skip_or_play");
        if(!skipOrPlayHelpText.getString().isBlank())skipOrPlayHelpControl.setTooltip(Tooltip.create(skipOrPlayHelpText));
        var returnToAutomaticHelpControl=flat("Automatic",vw-116,transportY+26,104,22,runtime::returnAutomatic);
        var returnToAutomaticHelpText=Component.translatable("afmr.copy.library.return_to_automatic");
        if(!returnToAutomaticHelpText.getString().isBlank())returnToAutomaticHelpControl.setTooltip(Tooltip.create(returnToAutomaticHelpText));
        refresh();folders.offset(folderOffset);songs.offset(songOffset);priorities.offset(priorityOffset);
        songRevision=runtime.catalogRevision();wasScanning=runtime.scanning();
    }
    void openPage(WorkspacePage page){
        if(page==WorkspacePage.PLAYLISTS){minecraft.gui.setScreen(this);return;}
        MusicScreen screen=pages.computeIfAbsent(page,key->switch(key){
            case LIBRARY->new SourcesScreen(this,runtime);case TIMING->new SettingsScreen(this,runtime);
            case DISPLAY->new NowPlayingSettingsScreen(this,runtime);case KEYS->new OverrideBindingsScreen(this,runtime);
            case INSPECTOR->new PlaybackInspectorScreen(this,runtime);
            case ACTIVITY->new ListeningScreen(this,runtime);
            case PROFILES->new ProfilesScreen(this,runtime);
            default->throw new IllegalStateException();
        });
        minecraft.gui.setScreen(screen);
    }
    private void movePlaylist(int target){runtime.library().movePlaylist(playlistId,target);runtime.save();rebuildWidgets();priorities.reveal(playlist());}
    private void deletePlaylist(){
        Playlist target=playlist();
        minecraft.gui.setScreen(new ConfirmScreen(yes->{if(yes){runtime.library().removePlaylist(target.id);runtime.save();}minecraft.gui.setScreen(this);},
            Component.literal("Delete "+target.name+"?"),Component.literal("Remove playlist and assignments?")));
    }
    private void changed(){runtime.save();rebuildWidgets();}
    private void buildRules(){
        Playlist p=playlist();int x=layout.rulesX()+12,w=layout.rulesWidth()-24;
        var automaticSwitchHelpControl=flat("Automatic: "+(p.enabled?"on":"off"),x,100,w,22,()->{p.enabled=!p.enabled;changed();});
        var automaticSwitchHelpText=Component.translatable("afmr.copy.library.automatic_switch");
        if(!automaticSwitchHelpText.getString().isBlank())automaticSwitchHelpControl.setTooltip(Tooltip.create(automaticSwitchHelpText));
        var manualOnlyHelpControl=flat("Manual only: "+(p.manualOnly?"on":"off"),x,126,w,22,()->{p.manualOnly=!p.manualOnly;changed();});
        var manualOnlyHelpText=Component.translatable("afmr.copy.library.manual_only");
        if(!manualOnlyHelpText.getString().isBlank())manualOnlyHelpControl.setTooltip(Tooltip.create(manualOnlyHelpText));
        conditionSummary=p.conditions.summary();
        var matchAllOrAnyHelpControl=flat("Match: "+(p.conditions.matchAny?"any":"all"),x,184,w,22,()->{p.conditions.matchAny=!p.conditions.matchAny;changed();});
        var matchAllOrAnyHelpText=Component.translatable("afmr.copy.library.match_all_or_any");
        if(!matchAllOrAnyHelpText.getString().isBlank())matchAllOrAnyHelpControl.setTooltip(Tooltip.create(matchAllOrAnyHelpText));
        flat("Name & conditions",x,212,w,22,this::editConditions);
        volumeButton=flat("Volume: "+Math.round(p.volume*100)+"%",x,252,(w-6)/2,22,()->minecraft.gui.setScreen(new PlaylistVolumeScreen(this,runtime,p)));
        var playlistVolumeHelpControl=volumeButton;
        var playlistVolumeHelpText=Component.translatable("afmr.copy.library.playlist_volume");
        if(!playlistVolumeHelpText.getString().isBlank())playlistVolumeHelpControl.setTooltip(Tooltip.create(playlistVolumeHelpText));
        silenceButton=flat("Silence: "+(p.customSilence?"custom":"default"),x+(w-6)/2+6,252,w-(w-6)/2-6,22,()->minecraft.gui.setScreen(new PlaylistSilenceScreen(this,runtime,p)));
        var playlistSilenceHelpText=Component.translatable("afmr.copy.library.playlist_silence");
        if(!playlistSilenceHelpText.getString().isBlank())silenceButton.setTooltip(Tooltip.create(playlistSilenceHelpText));
        var minecraftMusicHelpControl=flat("Minecraft music: "+(p.includeVanilla?"on":"off"),x,278,w,22,()->{p.includeVanilla=!p.includeVanilla;changed();});
        var minecraftMusicHelpText=Component.translatable("afmr.copy.library.minecraft_music");
        if(!minecraftMusicHelpText.getString().isBlank())minecraftMusicHelpControl.setTooltip(Tooltip.create(minecraftMusicHelpText));
        var finishSongOnEntryHelpControl=flat("Finish song on entry: "+(p.finishCurrentSong?"on":"off"),x,304,w,22,()->{p.finishCurrentSong=!p.finishCurrentSong;changed();});
        var finishSongOnEntryHelpText=Component.translatable("afmr.copy.library.finish_song_on_entry");
        if(!finishSongOnEntryHelpText.getString().isBlank())finishSongOnEntryHelpControl.setTooltip(Tooltip.create(finishSongOnEntryHelpText));
        var finishSongOnExitHelpControl=flat("Finish song on exit: "+(p.finishCurrentSongOnExit?"on":"off"),x,330,w,22,()->{p.finishCurrentSongOnExit=!p.finishCurrentSongOnExit;changed();});
        var finishSongOnExitHelpText=Component.translatable("afmr.copy.library.finish_song_on_exit");
        if(!finishSongOnExitHelpText.getString().isBlank())finishSongOnExitHelpControl.setTooltip(Tooltip.create(finishSongOnExitHelpText));
        var playOnTriggerHelpControl=flat("Play on trigger: "+(p.playOnTrigger?"on":"off"),x,356,w,22,()->{p.playOnTrigger=!p.playOnTrigger;changed();});
        var playOnTriggerHelpText=Component.translatable("afmr.copy.library.play_on_trigger");
        if(!playOnTriggerHelpText.getString().isBlank())playOnTriggerHelpControl.setTooltip(Tooltip.create(playOnTriggerHelpText));
        flat("Overrides",x,388,w,22,()->minecraft.gui.setScreen(new OverridesScreen(this,runtime,-1,playlistId)));
        autoplayHere=addRenderableWidget(new AutoplayHereButton(x,414,w,22,scale,()->{playlist().autoplayHere(runtime.context());changed();}));
        autoplayHere.active=runtime.inWorld();
    }
    private void editConditions(){
        minecraft.gui.setScreen(new PlaylistConditionsScreen(this,runtime,playlist(),playlist().conditions,0));
    }
    private void drawPriority(GuiGraphicsExtractor g,Playlist p,int x,int y,int w,boolean hover){
        if(p.id.equals(playlistId)||hover)g.fill(x,y,x+w,y+14,p.id.equals(playlistId)?MenuAppearance.color(MenuColor.SELECTION,0xFF35594E):MenuAppearance.color(MenuColor.HOVER,0xFF2C373A));
        g.text(font,Integer.toString(ranks.getOrDefault(p.id,0)),x+4,y+3,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF9FBCAD),false);
        g.text(font,fit(p.name,w-32),x+28,y+3,p.enabled&&!p.manualOnly?MenuAppearance.color(MenuColor.TEXT,0xFFE3F2E9):MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF8FA7A0),false);
    }
    @Override protected void repositionElements(){if(!MusicMenu.retainWidgets(this))super.repositionElements();}
    @Override public void added(){
        if(volumeButton!=null&&playlist()!=null)volumeButton.setMessage(Component.literal("Volume: "+Math.round(playlist().volume*100)+"%"));
        if(silenceButton!=null&&playlist()!=null)silenceButton.setMessage(Component.literal("Silence: "+(playlist().customSilence?"custom":"default")));
        if(songTable!=null&&!Arrays.equals(songTable.columns,SongColumn.normalize(runtime.library().settings.songColumns).toArray(SongColumn[]::new)))rebuildWidgets();
        else refresh();
    }
    private FlatButton flat(String label,int x,int y,int w,int h,Runnable action){return addRenderableWidget(new FlatButton(label,x,y,w,h,scale,action));}
    private EditBox search(String value,String hint,int x,int y,int w,java.util.function.Consumer<String> callback){
        var box=new EditBox(font,x,y,w,16,Component.literal(hint));box.setMaxLength(500);box.setValue(value);box.setHint(Component.literal(hint));box.setResponder(callback);return addRenderableWidget(box);
    }
    private boolean contains(Track t,String q){return (t.title+" "+t.artist+" "+t.album+" "+t.albumArtist+" "+t.genre+" "+t.date+" "+t.path).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT));}
    private void refresh(){
        if(folders==null||songs==null)return;
        if(inspected!=null)inspected=runtime.library().track(inspected.id);
        refreshFolders();refreshSongs();
    }
    private void refreshFolders(){if(folders!=null){treeRows=buildTree();folders.items(treeRows);}}
    private void refreshSongs(){
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
    private void clickFolder(TreeRow row,MouseButtonEvent e,boolean twice){
        if(row.group){if(!expanded.remove(row.id))expanded.add(row.id);folderSelection=row.id;librarySelection.clear();refreshFolders();return;}
        if(e.hasShiftDown())choose(librarySelection,row.track.id,libraryAnchor,treeRows.stream().filter(r->r.track!=null).map(r->r.track.id).toList(),e);
        else choose(librarySelection,row.track.id,libraryAnchor,List.of(),e);
        libraryAnchor=row.track.id;folderSelection="";inspected=row.track;if(twice)addSelection();
    }
    private void clickSong(Track track,MouseButtonEvent e,boolean twice){
        choose(songSelection,track.id,songAnchor,e.hasShiftDown()?songRows.stream().map(t->t.id).toList():List.of(),e);songAnchor=track.id;inspected=track;
        if(e.x()<tableX+28){playlist().tracks.put(track.id,!playlist().tracks.getOrDefault(track.id,true));runtime.save();refreshSongs();}
    }
    private void choose(Set<String> selection,String id,String anchor,List<String> order,MouseButtonEvent event){
        if(event.hasShiftDown()&&!anchor.isBlank()){int a=order.indexOf(anchor),b=order.indexOf(id);if(a>=0&&b>=0){if(!event.hasControlDown())selection.clear();selection.addAll(order.subList(Math.min(a,b),Math.max(a,b)+1));return;}}
        if(event.hasControlDown()){if(!selection.remove(id))selection.add(id);}else{selection.clear();selection.add(id);}
    }
    private void addSelection(){
        Set<String> ids=new LinkedHashSet<>(librarySelection);
        if(ids.isEmpty()&&!folderSelection.isBlank()){
            if(folderSelection.equals("all"))runtime.library().tracks.stream().filter(t->!t.vanilla).forEach(t->ids.add(t.id));
            else if(folderSelection.equals("vanilla"))runtime.library().tracks.stream().filter(t->t.vanilla).forEach(t->ids.add(t.id));
            else if(folderSelection.startsWith("folder:")){Path f=Path.of(folderSelection.substring(7));runtime.library().tracks.stream().filter(t->!t.vanilla&&Path.of(t.path).startsWith(f)).forEach(t->ids.add(t.id));}
        }
        for(String id:ids)playlist().tracks.put(id,true);
        runtime.save();runtime.message(ids.size()+" added / "+playlist().name);refreshSongs();
    }
    private void setSongs(boolean enabled){songSelection.forEach(id->playlist().tracks.put(id,enabled));runtime.save();refreshSongs();}
    private void previewSelected(){if(inspected!=null)runtime.preview(inspected);}
    private void queueSelected(){
        int added=0;
        for(var track:songRows)if(songSelection.contains(track.id)&&runtime.queueTrack(track.id,playlistId))added++;
        runtime.message(added+" queued"+(songSelection.size()>added?" / "+(songSelection.size()-added)+" unavailable or queue full":""));
    }
    private void drawFolder(GuiGraphicsExtractor g,TreeRow r,int x,int y,int w,boolean hover){
        boolean selected=r.group?folderSelection.equals(r.id):librarySelection.contains(r.id);if(selected||hover)g.fill(x,y,x+w,y+14,selected?MenuAppearance.color(MenuColor.SELECTION,0xFF35594E):MenuAppearance.color(MenuColor.HOVER,0xFF2C373A));
        g.text(font,fit((r.group?(r.open?"v ":"> "):"  ")+r.label,w-r.depth*10-8),x+4+r.depth*10,y+3,r.group?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFB3D4C4):MenuAppearance.color(MenuColor.TEXT,0xFFD5DFDA),false);
    }
    private void drawSong(GuiGraphicsExtractor g,Track t,int x,int y,int w,boolean hover){
        if(songSelection.contains(t.id)||hover)g.fill(x,y,x+w,y+14,songSelection.contains(t.id)?MenuAppearance.color(MenuColor.SELECTION,0xFF35594E):MenuAppearance.color(MenuColor.HOVER,0xFF2C373A));
        Boolean explicit=playlist().tracks.get(t.id);int color=Boolean.FALSE.equals(explicit)?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF788880):MenuAppearance.color(MenuColor.TEXT,0xFFDCE6E0);
        g.text(font,explicit==null?"auto":explicit?"on":"off",x+3,y+3,Boolean.FALSE.equals(explicit)?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF788880):MenuAppearance.color(MenuColor.ACCENT,0xFF8DE0BE),false);
        var cells=songCells.get(t.id);if(cells==null)return;
        g.enableScissor(x+SongTableLayout.PINNED,y,x+w,y+14);
        for(int i=0;i<cells.length;i++){
            int cx=x+songTable.x(i);
            if(cx+songTable.widths[i]>x+SongTableLayout.PINNED&&cx<x+w)g.text(font,cells[i],cx+4,y+3,color,false);
        }
        g.disableScissor();
    }
    private String fit(String text,int w){return font.plainSubstrByWidth(text,Math.max(0,w));}
    private static String duration(double s){int seconds=(int)s%60;return s<=0?"--:--":(int)(s/60)+":"+(seconds<10?"0":"")+seconds;}
    @Override public void tick(){
        if(repairReport!=runtime.sourceHealth()){repairReport=runtime.sourceHealth();updateRepairButton();}
        if(autoplayHere!=null)autoplayHere.tick();
        if(songRevision!=runtime.catalogRevision()||wasScanning!=runtime.scanning()){songRevision=runtime.catalogRevision();wasScanning=runtime.scanning();refresh();}
        art.track(runtime.engine().current());
        if(pause!=null){addPlaying.active=runtime.engine().current()!=null;pause.setMessage(Component.literal(runtime.engine().paused()?"Resume":"Pause"));}
    }
    private void updateRepairButton(){
        int missing=repairReport==null?0:repairReport.issues().size();
        repairButton.setMessage(Component.literal(missing==0?"Repair / missing":"Missing: "+missing));
        repairButton.setLabelTextColor(missing==0?null:0xFFFF9A91);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float delta){
        if(minecraft.level==null)extractPanorama(g,delta);
        // Blur starts before the current stratum, so the image must occupy the preceding one.
        if(runtime.menuBackground().draw(g,width,height))g.nextStratum();
        if(runtime.library().settings.menuBlur&&runtime.library().settings.menuBackgroundOpacity<1&&minecraft.gui.screen()==this)extractBlurredBackground(g);
        minecraft.gui.hud.extractDeferredSubtitles();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.pose().pushMatrix();g.pose().scale(scale,scale);
        int footer=layout.footerY(),rx=layout.rulesX(),rw=layout.rulesWidth();
        for(var surface:backgrounds)g.fill(surface.x0(),surface.y0(),surface.x1(),surface.y1(),MenuAppearance.background(surface.color(),surface.rgb()));
        g.fill(split,64,split+1,footer,MenuAppearance.color(MenuColor.BORDERS,0xFF3C4D49));
        g.text(font,fit("AFMR / "+runtime.library().profiles.activeName,vw-24),12,12,MenuAppearance.color(MenuColor.TEXT,0xFFE5EEE6),false);
        g.text(font,"PRIORITY",12,76,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3),false);
        g.text(font,"Music library",12,layout.libraryY(),MenuAppearance.color(MenuColor.TEXT,0xFFE5EEE6),false);
        g.text(font,fit(playlist().name,tableWidth),tableX,78,MenuAppearance.color(MenuColor.TEXT,0xFFE5EEE6),false);
        g.text(font,"Conditions",rx+12,82,MenuAppearance.color(MenuColor.TEXT,0xFFE5EEE6),false);
        g.text(font,fit(conditionSummary,rw-24),rx+12,162,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3),false);
        g.text(font,"Playback",rx+12,238,MenuAppearance.color(MenuColor.TEXT,0xFFE5EEE6),false);
        g.text(font,"On",tableX+3,layout.songHeaderY()+5,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3),false);
        g.enableScissor(tableX+SongTableLayout.PINNED,layout.songHeaderY(),tableX+tableWidth-8,layout.songListY());
        for(int i=0;i<columnHeaders.length;i++)g.text(font,columnHeaders[i],tableX+songTable.x(i)+4,layout.songHeaderY()+5,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3),false);
        g.disableScissor();
        if(songTable.overflow()){
            int barX=tableX+SongTableLayout.PINNED,barY=columnBarY();
            g.fill(tableX,barY,tableX+tableWidth,barY+SongTableLayout.BAR,MenuAppearance.background(MenuColor.CONTROLS,0x111619));
            g.fill(barX+songTable.thumbX(),barY+2,barX+songTable.thumbX()+songTable.thumbWidth(),barY+8,MenuAppearance.color(MenuColor.SCROLLBARS,0xFF637B75));
        }
        g.fill(0,footer,vw,footer+1,MenuAppearance.color(MenuColor.BORDERS,0xFF3C4D49));
        art.draw(g,12,footer+12,42);
        Track t=runtime.engine().current();
        int infoWidth=vw/2-185;
        String state=runtime.engine().paused()?"Paused":runtime.engine().jukeboxActive()?"Jukebox":runtime.engine().previewing()?"Preview":runtime.engine().current()==null?"Waiting":"Playing";
        g.text(font,fit(t==null?state:t.title,infoWidth),64,footer+13,MenuAppearance.color(MenuColor.TEXT,0xFFE5EEE6),false);
        g.text(font,fit(t==null?"Silence / "+(int)runtime.engine().remainingSilence(runtime.now())+"s":t.artist+" / "+duration(t.durationSeconds),infoWidth),64,footer+29,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3),false);
        g.text(font,state,64,footer+44,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3),false);
        var active=runtime.engine().selected();
        g.text(font,fit((runtime.engine().overrideId()==null?"Automatic / ":"Override / ")+(active==null?"None":active.name),220),vw-232,footer+14,MenuAppearance.color(MenuColor.ACCENT,0xFFB9D7B7),false);
        var source=runtime.library().playlist(runtime.engine().sourcePlaylistId());
        g.text(font,fit("Source / "+(source==null?runtime.engine().previewing()?"Preview":"None":source.name),220),vw-232,footer+29,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3),false);
        if(runtime.scanning())ScanProgressView.draw(g,font,runtime.scanProgress(),500,0,vw-592);
        super.extractRenderState(g,(int)(mx/scale),(int)(my/scale),dt);
        g.text(font,fit(runtime.status(),vw-24),12,vh-12,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3),false);g.pose().popMatrix();
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        var tr=new MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo());
        if(onColumnBar(tr.x(),tr.y())){
            double position=tr.x()-tableX-SongTableLayout.PINNED;
            columnDragGrab=position>=songTable.thumbX()&&position<=songTable.thumbX()+songTable.thumbWidth()?position-songTable.thumbX():songTable.thumbWidth()/2d;
            draggingColumns=true;songTable.dragThumb(position-columnDragGrab);setFocused(songs);return true;
        }
        if(tr.y()>=layout.songHeaderY()&&tr.y()<layout.songListY()&&tr.x()>=tableX&&tr.x()<tableX+tableWidth){
            int chosen=songTable.columnAt(tr.x()-tableX);
            if(chosen>=0){var column=songTable.columns[chosen];if(sortColumn==column)descending=!descending;else{sortColumn=column;descending=false;}refreshSongs();}
            return true;
        }
        return super.mouseClicked(tr,twice);
    }
    private int columnBarY(){return layout.songListY()+layout.songListHeight()-SongTableLayout.BAR;}
    private boolean onColumnBar(double x,double y){return songTable.overflow()&&x>=tableX+SongTableLayout.PINNED&&x<tableX+tableWidth-8&&y>=columnBarY()&&y<columnBarY()+SongTableLayout.BAR;}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(draggingColumns){draggingColumns=false;return true;}return super.mouseReleased(new MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo()));}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(draggingColumns){songTable.dragThumb(e.x()/scale-tableX-SongTableLayout.PINNED-columnDragGrab);return true;}
        return super.mouseDragged(new MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo()),dx/scale,dy/scale);
    }
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/scale,y/scale);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
        double tx=x/scale,ty=y/scale;
        if(songTable.overflow()&&(onColumnBar(tx,ty)||dx!=0&&tx>=tableX&&tx<tableX+tableWidth&&ty>=layout.songHeaderY()&&ty<columnBarY())){
            songTable.scroll(songTable.scroll()-(int)Math.round((dx!=0?dx:dy)*40));return true;
        }
        return super.mouseScrolled(tx,ty,dx,dy);
    }
    @Override public boolean keyPressed(KeyEvent e){
        if(getFocused()!=librarySearch&&getFocused()!=songSearch){
            if(getFocused()==songs&&songTable.overflow()&&(e.key()==InputConstants.KEY_LEFT||e.key()==InputConstants.KEY_RIGHT)){
                songTable.scroll(songTable.scroll()+(e.key()==InputConstants.KEY_RIGHT?60:-60));return true;
            }
            if(e.isSelectAll()&&(getFocused()==folders||getFocused()==songs)){if(getFocused()==folders)treeRows.stream().filter(r->r.track!=null).forEach(r->librarySelection.add(r.track.id));else songRows.forEach(t->songSelection.add(t.id));return true;}
            if(getFocused()==songs&&e.key()==InputConstants.KEY_SPACE&&!songSelection.isEmpty()){setSongs(songSelection.stream().anyMatch(id->Boolean.FALSE.equals(playlist().tracks.get(id))));return true;}
        }
        return super.keyPressed(e);
    }
    @Override public void onFilesDrop(List<Path> paths){
        if(paths.stream().anyMatch(dev.animal.afmr.library.ConfigBackup::supported)){
            if(paths.size()!=1){runtime.message("Drop one .afmrb backup at a time.");return;}
            runtime.prepareBackupImport(paths.getFirst());
            minecraft.gui.setScreen(new ConfigBackupScreen(this,runtime));return;
        }
        if(paths.stream().anyMatch(dev.animal.afmr.library.PlaylistPresets::supported)){
            if(paths.size()!=1){runtime.message("Drop one .afmrp setup at a time, separately from music files.");return;}
            runtime.preparePresetImport(paths.getFirst(),runtime.library().profiles.activeId,playlistId);
            minecraft.gui.setScreen(new PlaylistSharingScreen(this,runtime,playlistId));return;
        }
        if(paths.stream().allMatch(dev.animal.afmr.library.FpliteFiles::supported))runtime.importPlaylists(paths,runtime.library().profiles.activeId,playlistId);
        else if(paths.stream().anyMatch(dev.animal.afmr.library.FpliteFiles::supported))runtime.message("Drop playlist files and music files in separate batches.");
        else runtime.importPaths(paths,runtime.library().profiles.activeId,"library",true);
    }
    @Override public void onClose(){MusicMenu.remember(this);minecraft.gui.setScreen(parent);}
    @Override public void removed(){draggingColumns=false;if(autoplayHere!=null)autoplayHere.cancelHold();art.close();}
    @Override public boolean isPauseScreen(){return false;}
}

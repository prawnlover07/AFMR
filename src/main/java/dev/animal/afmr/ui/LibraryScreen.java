package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;

/** A compact folder tree, playlist tabs and independently scrolling song table. */
public final class LibraryScreen extends Screen {
    private final Screen parent;private final ClientRuntime runtime;
    private float scale;private int vw,vh,split,tableX,tableWidth;
    private int[] columnOffsets;
    private static final String[] COLUMN_LABELS={"On","Title","Artist","Album","Time"};
    private String playlistId="day_outside",libraryQuery="",songQuery="",folderSelection="";
    private final Set<String> expanded=new HashSet<>();
    private boolean initialTree=true;
    private final Set<String> librarySelection=new LinkedHashSet<>(),songSelection=new LinkedHashSet<>();
    private String libraryAnchor="",songAnchor="";
    private Track inspected;private final EditorArtwork art=new EditorArtwork();
    private DenseList<TreeRow> folders;private DenseList<Track> songs;
    private List<TreeRow> treeRows=List.of();private List<Track> songRows=List.of();
    private int sortColumn=1;private boolean descending,wasScanning;private long trackCount;
    private EditBox librarySearch,songSearch;
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
    void selectPlaylist(String id){playlistId=id;songSelection.clear();songQuery="";}
    @Override protected void init(){
        // Local virtual canvas: the user's Minecraft GUI-scale preference is never changed.
        scale=Math.min(1f,Math.max(0.4f,width/900f));vw=(int)(width/scale);vh=(int)(height/scale);
        split=Math.clamp((int)(vw*.27),190,290);tableX=split+6;tableWidth=vw-tableX-8;
        columnOffsets=new int[]{0,28,(int)(tableWidth*.43),(int)(tableWidth*.66),tableWidth-56};
        if(initialTree){initialTree=false;expanded.add(runtime.library().folders.isEmpty()?"all":"folder:"+Path.of(runtime.library().folders.getFirst().path).toAbsolutePath().normalize());}
        int right=vw-8;
        if(playlist()==null)playlistId=runtime.library().playlists.getFirst().id;
        flat("Playlists",210,5,74,16,()->minecraft.gui.setScreen(new PlaylistManagerScreen(this,runtime,playlistId)));
        flat("Done",right-42,5,42,16,this::onClose);
        flat("Settings",right-104,5,58,16,()->minecraft.gui.setScreen(new SettingsScreen(this,runtime)));
        flat("Rescan",right-160,5,52,16,runtime::rescan);
        flat("Add folder",right-236,5,72,16,()->NativePicker.open(runtime,NativePicker.Kind.FOLDER,"library"));
        flat("Add files",right-306,5,66,16,()->NativePicker.open(runtime,NativePicker.Kind.FILES,"library"));
        flat("Skip / play",right-386,5,76,16,()->{if(runtime.engine().current()==null)runtime.playNow();else runtime.skip();});
        flat("Play selected",right-494,5,104,16,this::previewSelected);
        flat("Import playlist",right-604,5,106,16,()->minecraft.gui.setScreen(new PlaylistImportScreen(this,runtime,playlistId)));
        var playlists=runtime.library().playlists;
        int visible=Math.min(6,playlists.size()),offset=(playlists.indexOf(playlist())/6)*6;
        int tabX=tableX,tabArea=tableWidth;
        if(playlists.size()>6){
            flat("<",tableX,80,20,18,()->{playlistId=playlists.get(Math.max(0,offset-6)).id;songSelection.clear();songQuery="";rebuildWidgets();}).active=offset>0;
            flat(">",tableX+tableWidth-20,80,20,18,()->{playlistId=playlists.get(offset+6).id;songSelection.clear();songQuery="";rebuildWidgets();}).active=offset+6<playlists.size();
            tabX+=24;tabArea-=48;
        }
        int tabWidth=(tabArea-4*(visible-1))/Math.max(1,visible);
        for(int i=offset;i<Math.min(playlists.size(),offset+6);i++){
            Playlist p=playlists.get(i);var tab=flat(p.name,tabX+(i-offset)*(tabWidth+4),80,tabWidth,18,()->{playlistId=p.id;songSelection.clear();songQuery="";rebuildWidgets();});tab.selected=p.id.equals(playlistId);
        }
        flat("Folders...",split-76,80,70,18,()->minecraft.gui.setScreen(new SourcesScreen(this,runtime)));
        librarySearch=search(libraryQuery,"Filter folders / music",6,102,split-12,v->{libraryQuery=v;folders.top();refreshFolders();});
        songSearch=search(songQuery,"Filter this playlist",tableX,102,tableWidth,v->{songQuery=v;songs.top();refreshSongs();});
        folders=addRenderableWidget(new DenseList<>(6,122,split-12,vh-164,this::drawFolder,this::clickFolder,r->r.label));
        songs=addRenderableWidget(new DenseList<>(tableX,136,tableWidth,vh-178,this::drawSong,this::clickSong,t->t.title));
        flat("Add selected / folder ->",6,vh-35,split-12,19,this::addSelection);
        int bx=tableX;
        flat("Enable",bx,vh-35,56,19,()->setSongs(true));bx+=60;
        flat("Disable",bx,vh-35,56,19,()->setSongs(false));bx+=60;
        flat("Remove",bx,vh-35,58,19,()->{songSelection.forEach(id->playlist().tracks.remove(id));runtime.save();refreshSongs();});bx+=62;
        flat("Playlist settings",bx,vh-35,114,19,()->minecraft.gui.setScreen(new PlaylistRulesScreen(this,this,runtime,playlist())));bx+=118;
        flat("Home area...",bx,vh-35,90,19,()->minecraft.gui.setScreen(new HomeScreen(this,runtime)));
        bx+=94;flat("Inspector",bx,vh-35,80,19,()->minecraft.gui.setScreen(new PlaybackInspectorScreen(this,runtime)));
        bx+=84;flat("Overrides...",bx,vh-35,90,19,()->minecraft.gui.setScreen(new OverridesScreen(this,runtime,-1,playlistId)));
        refresh();trackCount=runtime.library().tracks.size();wasScanning=runtime.scanning();
    }
    private FlatButton flat(String label,int x,int y,int w,int h,Runnable action){return addRenderableWidget(new FlatButton(label,x,y,w,h,action));}
    private EditBox search(String value,String hint,int x,int y,int w,java.util.function.Consumer<String> callback){
        var box=new EditBox(font,x,y,w,16,Component.literal(hint));box.setMaxLength(500);box.setValue(value);box.setHint(Component.literal(hint));box.setResponder(callback);return addRenderableWidget(box);
    }
    private boolean contains(Track t,String q){return (t.title+" "+t.artist+" "+t.album+" "+t.path).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT));}
    private void refresh(){
        if(folders==null||songs==null)return;refreshFolders();refreshSongs();
    }
    private void refreshFolders(){if(folders!=null){treeRows=buildTree();folders.items(treeRows);}}
    private void refreshSongs(){
        if(songs==null)return;Playlist p=playlist();if(p==null)return;
        Set<String> vanilla=runtime.applicableVanilla();
        Comparator<Track> comparator=switch(sortColumn){case 2->Comparator.comparing(t->t.artist,String.CASE_INSENSITIVE_ORDER);case 3->Comparator.comparing(t->t.album,String.CASE_INSENSITIVE_ORDER);case 4->Comparator.comparingDouble(t->t.durationSeconds);default->Comparator.comparing(t->t.title,String.CASE_INSENSITIVE_ORDER);};
        if(descending)comparator=comparator.reversed();
        songRows=runtime.library().pool(p,vanilla,true).stream().filter(t->songQuery.isBlank()||contains(t,songQuery)).sorted(comparator.thenComparing(t->t.path)).toList();songs.items(songRows);
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
    private Node trackNode(Track t,String label){Node n=new Node(t.id,label);n.track=t;n.search=(t.title+" "+t.artist+" "+t.album+" "+t.path).toLowerCase(Locale.ROOT);return n;}
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
        for(String id:ids)playlist().tracks.put(id,true);runtime.save();runtime.message(ids.size()+" tracks added to "+playlist().name+".");refreshSongs();
    }
    private void setSongs(boolean enabled){songSelection.forEach(id->playlist().tracks.put(id,enabled));runtime.save();refreshSongs();}
    private void previewSelected(){if(inspected==null)runtime.message("Select a song in either pane, then choose Play selected.");else runtime.preview(inspected);}
    private void drawFolder(GuiGraphicsExtractor g,TreeRow r,int x,int y,int w,boolean hover){
        boolean selected=r.group?folderSelection.equals(r.id):librarySelection.contains(r.id);if(selected||hover)g.fill(x,y,x+w,y+14,selected?0xFF35594E:0xFF2C373A);
        g.text(font,fit((r.group?(r.open?"v ":"> "):"  ")+r.label,w-r.depth*10-8),x+4+r.depth*10,y+3,r.group?0xFFB3D4C4:0xFFD5DFDA,false);
    }
    private void drawSong(GuiGraphicsExtractor g,Track t,int x,int y,int w,boolean hover){
        if(songSelection.contains(t.id)||hover)g.fill(x,y,x+w,y+14,songSelection.contains(t.id)?0xFF35594E:0xFF2C373A);
        int[] c=columnOffsets;Boolean explicit=playlist().tracks.get(t.id);int color=Boolean.FALSE.equals(explicit)?0xFF788880:0xFFDCE6E0;
        g.text(font,explicit==null?"auto":explicit?"on":"off",x+3,y+3,Boolean.FALSE.equals(explicit)?0xFF788880:0xFF8DE0BE,false);
        cell(g,t.title,x+c[1],y,c[2]-c[1],color);cell(g,t.artist,x+c[2],y,c[3]-c[2],color);cell(g,t.album,x+c[3],y,c[4]-c[3],color);cell(g,duration(t.durationSeconds),x+c[4],y,w-c[4],color);
    }
    private void cell(GuiGraphicsExtractor g,String text,int x,int y,int w,int color){g.text(font,fit(text,w-7),x+4,y+3,color,false);}
    private String fit(String text,int w){return font.plainSubstrByWidth(text,Math.max(0,w));}
    private static String duration(double s){int seconds=(int)s%60;return s<=0?"--:--":(int)(s/60)+":"+(seconds<10?"0":"")+seconds;}
    @Override public void tick(){if(trackCount!=runtime.library().tracks.size()||wasScanning!=runtime.scanning()){trackCount=runtime.library().tracks.size();wasScanning=runtime.scanning();refresh();}art.track(inspected!=null?inspected:runtime.engine().current());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.pose().pushMatrix();g.pose().scale(scale,scale);g.fill(0,0,vw,vh,0xFF141A1D);g.fill(0,0,vw,24,0xFF20292D);g.text(font,"animal's final music replacer",8,9,0xFFB9DCCE,false);
        g.fill(6,28,54,76,0xFF2C423A);art.draw(g,6,28,48);Track t=inspected!=null?inspected:runtime.engine().current();int infoWidth=vw-310;
        if(runtime.scanning()) ScanProgressView.draw(g,font,runtime.scanProgress(),64,31,infoWidth-66);
        else {
        g.text(font,fit(t==null?"Select music to inspect it":t.title,infoWidth-66),64,31,0xFFE3F2E9,false);
        g.text(font,fit(t==null?"Add a folder, then select songs on the left.":t.artist,infoWidth-66),64,46,0xFF9FBCAD,false);
        g.text(font,fit(t==null?"Double-click a file to add it to the open playlist.":t.album+"  /  "+duration(t.durationSeconds),infoWidth-66),64,61,0xFF819A8F,false);
        }
        var active=runtime.engine().selected();g.text(font,fit((runtime.engine().overrideId()==null?"ACTIVE  ":"OVERRIDE  ")+(active==null?"None":"#"+(runtime.library().playlists.indexOf(active)+1)+" "+active.name),220),vw-230,32,0xFF8DE0BE,false);
        String state=runtime.engine().current()==null?"Silence: "+(int)runtime.engine().remainingSilence(runtime.now())+"s":(runtime.engine().previewing()?"Preview: ":"Playing: ")+runtime.engine().current().title;g.text(font,fit((runtime.engine().paused()?"Paused / ":"")+state,220),vw-230,47,0xFFC8D9D0,false);
        g.text(font,fit("ENV  "+runtime.environmentLabel(),220),vw-230,62,0xFF829D8F,false);
        g.text(font,"MUSIC FOLDERS",8,85,0xFF9CB8AA,false);g.fill(split,80,split+1,vh-18,0xFF40504B);g.fill(tableX,122,vw-8,136,0xFF293337);
        int[] c=columnOffsets;for(int i=0;i<COLUMN_LABELS.length;i++)g.text(font,COLUMN_LABELS[i]+(sortColumn==i?(descending?" v":" ^"):""),tableX+c[i]+3,125,0xFFABC8B9,false);
        super.extractRenderState(g,(int)(mx/scale),(int)(my/scale),dt);
        String status=runtime.status();if(status.isBlank())status=songRows.size()+" songs  |  Ctrl/Shift: select multiple  |  Space: toggle selected";g.text(font,fit(status,vw-12),6,vh-12,0xFF8DAA9B,false);g.pose().popMatrix();
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        var tr=new MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo());
        if(tr.y()>=122&&tr.y()<136&&tr.x()>=tableX){int[] c=columnOffsets;int chosen=4;for(int i=0;i<c.length-1;i++)if(tr.x()<tableX+c[i+1]){chosen=i;break;}if(chosen>0){if(sortColumn==chosen)descending=!descending;else{sortColumn=chosen;descending=false;}refreshSongs();}return true;}
        return super.mouseClicked(tr,twice);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){return super.mouseReleased(new MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo()));}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return super.mouseDragged(new MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo()),dx/scale,dy/scale);}
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/scale,y/scale);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){return super.mouseScrolled(x/scale,y/scale,dx,dy);}
    @Override public boolean keyPressed(KeyEvent e){
        if(getFocused()!=librarySearch&&getFocused()!=songSearch){
            if(e.isSelectAll()){if(getFocused()==folders)treeRows.stream().filter(r->r.track!=null).forEach(r->librarySelection.add(r.track.id));else songRows.forEach(t->songSelection.add(t.id));return true;}
            if(e.key()==InputConstants.KEY_SPACE&&!songSelection.isEmpty()){setSongs(songSelection.stream().anyMatch(id->Boolean.FALSE.equals(playlist().tracks.get(id))));return true;}
        }
        return super.keyPressed(e);
    }
    @Override public void onFilesDrop(List<Path> paths){
        if(paths.stream().allMatch(dev.animal.afmr.library.FpliteFiles::supported))runtime.importPlaylists(paths,playlistId);
        else if(paths.stream().anyMatch(dev.animal.afmr.library.FpliteFiles::supported))runtime.message("Drop playlist files and music files in separate batches.");
        else runtime.importPaths(paths,"library",true);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void removed(){art.close();}
    @Override public boolean isPauseScreen(){return false;}
}

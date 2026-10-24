package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.PlaylistPresets.FileData;
import dev.animal.afmr.library.MusicFiles.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Portable song references and an owned, read-only filesystem plan for a setup import. */
public final class PresetSongs {
    public static final int MAX_SONGS=100_000;
    public static final class FolderRef {
        public String id="",name="";
        public FolderRef(){}
        FolderRef(String id,String name){this.id=id;this.name=name;}
    }
    public static final class Song {
        public String id="",folder="",path="",title="";
        public boolean vanilla;
    }
    public record Root(String id,String name,String path,boolean chosen){}
    public record Resolved(String song,Track track,boolean found){}
    public record Plan(FileData data,List<Root> roots,List<Resolved> songs,int missing,int unavailableResources){
        public Plan{roots=List.copyOf(roots);songs=List.copyOf(songs);}
        public int found(){return songs.size()-missing-unavailableResources;}
    }
    private PresetSongs(){}

    /** Only selected assignments are read. Never walk unrelated catalog entries for export. */
    public static void capture(Library lib,Map<String,String> selected,FileData data){
        var roots=new LinkedHashMap<Path,FolderRef>();var songs=new HashMap<String,String>();
        var linked=new HashSet<Path>();for(var f:lib.folders)linked.add(Path.of(f.path).toAbsolutePath().normalize());
        for(int i=0;i<data.playlists.size();i++){
            Playlist source=lib.playlist(selected.get(data.playlists.get(i).id)),target=data.playlists.get(i);
            for(var entry:source.tracks.entrySet()){
                String ref=songs.get(entry.getKey());
                if(ref==null){
                    Track t=lib.track(entry.getKey());
                    if(t==null)throw new IllegalArgumentException("A selected playlist references a removed song; rescan or remove that entry first.");
                    var song=new Song();song.id="song:"+data.songs.size();song.title=t.title;song.vanilla=t.vanilla;
                    if(t.vanilla)song.path=t.path;
                    else{
                        Path path=Path.of(t.path).toAbsolutePath().normalize(),root=path.getParent();
                        while(root!=null&&!linked.contains(root))root=root.getParent();
                        if(root==null)root=path.getParent();
                        FolderRef folder=roots.get(root);
                        if(folder==null){
                            folder=new FolderRef("folder:"+roots.size(),root.getFileName()==null?"Music":root.getFileName().toString());
                            roots.put(root,folder);data.folders.add(folder);
                        }
                        song.folder=folder.id;song.path=root.relativize(path).toString().replace('\\','/');
                    }
                    ref=song.id;songs.put(t.id,ref);data.songs.add(song);
                }
                target.tracks.put(ref,entry.getValue());
            }
        }
    }
    public static void validate(FileData data){
        if(data.folders==null||data.songs==null||data.folders.size()>MAX_SONGS||data.songs.size()>MAX_SONGS)throw new IllegalArgumentException("Too many song references");
        var roots=new HashSet<String>();var ids=new HashSet<String>();var paths=new HashSet<String>();
        for(var f:data.folders){
            if(f==null||!id(f.id,"folder:")||!name(f.name,512)||f.name.contains("/")||f.name.contains("\\")||f.name.contains(":")||f.name.equals(".")||f.name.equals("..")||!roots.add(f.id))
                throw new IllegalArgumentException("Invalid or duplicate music folder reference");
        }
        for(var s:data.songs){
            if(s==null||!id(s.id,"song:")||!ids.add(s.id)||s.folder==null||!name(s.title,1024)||s.path==null||s.path.length()>4096)throw new IllegalArgumentException("Invalid or duplicate song reference");
            if(s.vanilla){
                if(!s.folder.isEmpty()||!s.path.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||s.path.contains(".."))throw new IllegalArgumentException("Invalid resource music identifier");
            }else if(!roots.contains(s.folder)||!relative(s.path)||!MusicFiles.supported(Path.of(s.path)))throw new IllegalArgumentException("Use relative MP3, FLAC, OGG or WAV paths inside a music folder");
            if(!paths.add(s.folder+"\n"+s.path))throw new IllegalArgumentException("Duplicate song path");
        }
        var used=new HashSet<String>();int assignments=0;
        for(var p:data.playlists)for(var e:p.tracks.entrySet()){
            if(++assignments>1_000_000||!ids.contains(e.getKey())||e.getValue()==null)throw new IllegalArgumentException("Invalid or excessive song assignments");
            used.add(e.getKey());
        }
        if(used.size()!=ids.size())throw new IllegalArgumentException("Unassigned song references");
        var usedRoots=new HashSet<String>();for(var s:data.songs)if(!s.vanilla)usedRoots.add(s.folder);
        if(!usedRoots.equals(roots))throw new IllegalArgumentException("Unused music folder references");
    }
    private static boolean id(String value,String prefix){return value!=null&&value.matches(prefix+"[0-9]{1,6}");}
    private static boolean name(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max&&value.codePoints().noneMatch(Character::isISOControl);}
    private static boolean relative(String value){
        if(!name(value,4096)||value.contains("\\")||value.contains(":")||value.startsWith("/"))return false;
        for(String part:value.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))return false;
        return true;
    }
    /** A duplicate folder name is ambiguous; leave it for an explicit Explorer choice. */
    public static Map<String,Path> matchFolders(FileData data,List<String> linked){
        var byName=new HashMap<String,Path>();var ambiguous=new HashSet<String>();
        var sourceNames=new HashSet<String>();for(var f:data.folders)if(!sourceNames.add(f.name))ambiguous.add(f.name);
        for(String name:linked){
            Path path=Path.of(name).toAbsolutePath().normalize();if(path.getFileName()==null)continue;
            String key=path.getFileName().toString();Path before=byName.putIfAbsent(key,path);
            if(before!=null&&!before.equals(path))ambiguous.add(key);
        }
        var result=new HashMap<String,Path>();
        for(var f:data.folders)if(!ambiguous.contains(f.name)&&byName.containsKey(f.name))result.put(f.id,byName.get(f.name));
        return result;
    }
    /** All disk/tag work stays on the existing library worker; inputs are owned snapshots. */
    public static Plan plan(FileData data,List<Track> catalog,Map<String,Path> choices,Path missingRoot,Consumer<ScanProgress> progress)throws IOException{
        // Conditions were validated before publishing the file; do not normalize shared UI data here.
        validate(data);
        var known=new HashMap<Path,Track>();var resources=new HashMap<String,Track>();
        for(var t:catalog)if(t.vanilla)resources.put(t.path,t);else known.put(Path.of(t.path).toAbsolutePath().normalize(),t);
        var roots=new ArrayList<Root>();var paths=new HashMap<String,Path>();
        for(var f:data.folders){
            Path chosen=choices.get(f.id),path=(chosen==null?missingRoot.resolve(f.id.replace(':','-')).resolve(f.name):chosen).toAbsolutePath().normalize();
            roots.add(new Root(f.id,f.name,path.toString(),chosen!=null));paths.put(f.id,path);
        }
        var result=new ArrayList<Resolved>();int missing=0,unavailable=0,processed=0;
        for(var s:data.songs){
            if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            Track track;boolean found;
            if(s.vanilla){
                Track existing=resources.get(s.path);found=existing!=null;
                track=found?Library.copyTrack(existing):new Track("vanilla:"+s.path,s.path,s.title,true);
                if(!found)unavailable++;
            }else{
                Path root=paths.get(s.folder),path=root.resolve(s.path).normalize();
                if(!path.startsWith(root))throw new IOException("Song path escapes the chosen music folder");
                Track existing=known.get(path);found=false;
                try{
                    if(!Files.isRegularFile(path)||!Files.isReadable(path))throw new IOException("Song unavailable");
                    track=existing==null?MusicFiles.read(path):Library.copyTrack(existing);found=true;
                }catch(IOException|SecurityException e){
                    track=existing==null?new Track("file:"+UUID.nameUUIDFromBytes(path.toString().getBytes(StandardCharsets.UTF_8)),path.toString(),s.title,false):Library.copyTrack(existing);
                    missing++;
                }
                known.put(path,track);
            }
            result.add(new Resolved(s.id,track,found));
            progress.accept(new ScanProgress(ScanStage.READING_TAGS,data.songs.size(),++processed,s.path));
        }
        return new Plan(data,roots,result,missing,unavailable);
    }
    /** One transition-time catalog index; no file access. Existing tracks/assignments are retained. */
    public static void apply(Library lib,FileData data,Plan plan,List<Playlist> added){
        if(plan.data()!=data||plan.songs().size()!=data.songs.size())throw new IllegalArgumentException("Review the song list again before importing");
        var local=new HashMap<Path,Track>();var resources=new HashMap<String,Track>();
        for(var t:lib.tracks)if(t.vanilla)resources.put(t.path,t);else local.put(Path.of(t.path).toAbsolutePath().normalize(),t);
        var mapped=new HashMap<String,String>();var newTracks=new ArrayList<Track>();
        for(var resolved:plan.songs()){
            Track t=resolved.track();Path path=t.vanilla?null:Path.of(t.path).toAbsolutePath().normalize();
            Track existing=t.vanilla?resources.get(t.path):local.get(path);
            if(existing==null){existing=Library.copyTrack(t);newTracks.add(existing);if(t.vanilla)resources.put(t.path,existing);else local.put(path,existing);}
            mapped.put(resolved.song(),existing.id);
        }
        for(int i=0;i<added.size();i++)for(var e:data.playlists.get(i).tracks.entrySet()){
            String id=mapped.get(e.getKey());if(id==null)throw new IllegalArgumentException("Incomplete song plan");
            // Several explicitly chosen paths can refer to one file; disabled wins on a conflict.
            added.get(i).tracks.merge(id,e.getValue(),(a,b)->a&&b);
        }
        var newFolders=new ArrayList<Folder>();var missingIds=new HashSet<String>();
        for(var r:plan.songs())if(!r.found()&&!r.track().vanilla)missingIds.add(r.song());
        var missingRoots=new HashSet<String>();for(var s:data.songs)if(missingIds.contains(s.id))missingRoots.add(s.folder);
        var linked=new HashSet<Path>();for(var f:lib.folders)linked.add(Path.of(f.path).toAbsolutePath().normalize());
        for(var root:plan.roots()){
            Path path=Path.of(root.path());
            if(!root.chosen()&&missingRoots.contains(root.id())&&linked.add(path))
                newFolders.add(new Folder(root.path(),"library"));
        }
        lib.tracks.addAll(newTracks);lib.folders.addAll(newFolders);lib.rebuildTrackIndex();
    }
}

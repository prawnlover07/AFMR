package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.MusicFiles.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

/** Read-only planning on owned inputs. Applying a reviewed plan belongs to the client. */
public final class LibraryRepair {
    public record Sources(List<Track> tracks,List<String> folders){
        public Sources{tracks=List.copyOf(tracks);folders=List.copyOf(folders);}
    }
    public record Request(boolean folder,String source,Path replacement){}
    public record Move(String id,String before,Track replacement,boolean found,long size,String modified){}
    public record FolderMove(String before,String after){}
    public record Merge(String removed,String retained,String expectedPath){}
    public record Plan(Request request,List<Move> moves,List<FolderMove> folders,List<Merge> merges,int found){
        public Plan{moves=List.copyOf(moves);folders=List.copyOf(folders);merges=List.copyOf(merges);}
        public int missing(){return moves.size()-found;}
    }
    private LibraryRepair(){}
    public static Sources capture(Library lib){
        var tracks=new ArrayList<Track>();for(var t:lib.tracks)if(!t.vanilla)tracks.add(Library.copyTrack(t));
        return new Sources(tracks,lib.folders.stream().map(f->f.path).distinct().toList());
    }
    public static Plan plan(Request request,Sources sources,Consumer<ScanProgress> progress)throws IOException{
        var moves=new ArrayList<Move>();var folders=new ArrayList<FolderMove>();
        Path oldRoot=request.folder()?Path.of(request.source()).toAbsolutePath().normalize():null;
        Path replacement=request.replacement().toAbsolutePath().normalize();
        if(request.folder()){
            if(!sources.folders().contains(request.source()))throw new IOException("The linked folder was removed; select it again.");
            if(!Files.isDirectory(replacement)||!Files.isReadable(replacement))throw new IOException("Choose a readable replacement folder.");
            if(replacement.startsWith(oldRoot))throw new IOException("Choose the folder's new location outside its old folder.");
            for(String folder:sources.folders()){
                Path path=Path.of(folder).toAbsolutePath().normalize();
                if(path.startsWith(oldRoot))folders.add(new FolderMove(folder,replacement.resolve(oldRoot.relativize(path)).toString()));
            }
            Set<Path> moving=new HashSet<>();for(var folder:folders)moving.add(Path.of(folder.before()).toAbsolutePath().normalize());
            for(var folder:folders)for(String existing:sources.folders())if(!moving.contains(Path.of(existing).toAbsolutePath().normalize())
                &&Path.of(folder.after()).equals(Path.of(existing).toAbsolutePath().normalize()))
                throw new IOException("The new location is already linked. Unlink that duplicate folder (keep songs), then retry repair.");
        }else if(!Files.isRegularFile(replacement)||!Files.isReadable(replacement)||!MusicFiles.supported(replacement))
            throw new IOException("Choose an existing readable MP3, FLAC, OGG or WAV file.");
        var affected=new ArrayList<Track>();
        for(var t:sources.tracks())if(request.folder()?Path.of(t.path).toAbsolutePath().normalize().startsWith(oldRoot):t.id.equals(request.source()))affected.add(t);
        if(!request.folder()&&affected.isEmpty())throw new IOException("The song was removed; select it again.");
        int found=0,processed=0;
        for(var track:affected){
            if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            Path destination=request.folder()?replacement.resolve(oldRoot.relativize(Path.of(track.path).toAbsolutePath().normalize())):replacement;
            boolean exists=false;long size=0;String modified="";Track next;
            try{
                var attributes=Files.readAttributes(destination,BasicFileAttributes.class);
                if(!attributes.isRegularFile()||!Files.isReadable(destination)||!MusicFiles.supported(destination))throw new IOException("Unreadable or unsupported file");
                next=MusicFiles.read(destination);exists=true;size=attributes.size();modified=attributes.lastModifiedTime().toString();found++;
            }catch(IOException|SecurityException e){
                if(!request.folder())throw new IOException("Cannot read the selected song: "+e.getMessage(),e);
                next=Library.copyTrack(track);next.path=destination.toString();
            }
            next.id=track.id;next.loudness=null;
            moves.add(new Move(track.id,track.path,next,exists,size,modified));
            progress.accept(new ScanProgress(ScanStage.REPAIRING,affected.size(),++processed,destination.toString()));
        }
        if(request.folder()&&!affected.isEmpty()&&found==0)throw new IOException("No songs matched. Choose the folder containing the same relative paths; renamed songs can be located individually.");
        var destinations=new HashMap<Path,String>();var movingIds=new HashSet<String>();
        for(var move:moves){
            String previous=destinations.put(Path.of(move.replacement().path).toAbsolutePath().normalize(),move.id());movingIds.add(move.id());
            if(previous!=null&&!previous.equals(move.id()))throw new IOException("Several library entries resolve to the same file; repair them individually.");
        }
        var merges=new ArrayList<Merge>();
        for(var t:sources.tracks())if(!movingIds.contains(t.id)){
            String retained=destinations.get(Path.of(t.path).toAbsolutePath().normalize());
            if(retained!=null)merges.add(new Merge(t.id,retained,t.path));
        }
        return new Plan(request,moves,folders,merges,found);
    }
    /** Validate the complete reviewed plan before mutating any live state. No filesystem I/O. */
    public static Map<String,String> apply(Library lib,Plan plan){
        var moves=new HashMap<String,Move>();var destinationIds=new HashMap<Path,String>();
        for(var move:plan.moves()){
            var track=lib.track(move.id());
            if(track==null||!track.path.equals(move.before()))throw new IllegalStateException("The library changed; cancel and prepare the repair again.");
            moves.put(move.id(),move);destinationIds.put(Path.of(move.replacement().path).toAbsolutePath().normalize(),move.id());
        }
        var remapped=new HashMap<String,String>();
        for(var merge:plan.merges()){
            Track t=lib.track(merge.removed());
            if(t==null||!t.path.equals(merge.expectedPath()))throw new IllegalStateException("A duplicate entry changed; prepare the repair again.");
            remapped.put(merge.removed(),merge.retained());
        }
        for(var t:lib.tracks)if(!t.vanilla&&!moves.containsKey(t.id)){
            String target=destinationIds.get(Path.of(t.path).toAbsolutePath().normalize());
            if(target!=null&&!target.equals(remapped.get(t.id)))throw new IllegalStateException("New songs were imported at the destination; prepare the repair again.");
        }
        var folders=new HashMap<String,String>();
        for(var move:plan.folders()){
            if(lib.folders.stream().noneMatch(f->f.path.equals(move.before())))throw new IllegalStateException("A linked folder changed; prepare the repair again.");
            folders.put(move.before(),move.after());
        }
        Set<Path> newFolders=new HashSet<>();for(String path:folders.values())newFolders.add(Path.of(path).toAbsolutePath().normalize());
        for(var f:lib.folders)if(!folders.containsKey(f.path)&&newFolders.contains(Path.of(f.path).toAbsolutePath().normalize()))
            throw new IllegalStateException("The destination folder was linked while reviewing; cancel and retry repair.");
        for(int i=0;i<lib.tracks.size();i++){
            Track current=lib.tracks.get(i);Move move=moves.get(current.id);if(move==null)continue;
            Track next=Library.copyTrack(move.replacement());
            if(move.found()&&current.loudness!=null&&current.loudness.matches(move.size(),move.modified()))next.loudness=current.loudness;
            lib.tracks.set(i,next);
        }
        if(!remapped.isEmpty()){
            lib.tracks.removeIf(t->remapped.containsKey(t.id));
            remapAssignments(lib.playlists,remapped);remapAssignments(lib.legacyPlaylists,remapped);
            for(var profile:lib.profiles.inactive)remapAssignments(profile.playlists,remapped);
        }
        for(var f:lib.folders)if(folders.containsKey(f.path))f.path=folders.get(f.path);
        for(var profile:lib.profiles.inactive){
            var transferred=new HashMap<String,String>();
            for(var move:folders.entrySet())if(profile.folderDestinations.containsKey(move.getKey()))
                transferred.put(move.getValue(),profile.folderDestinations.remove(move.getKey()));
            profile.folderDestinations.putAll(transferred);
        }
        lib.rebuildTrackIndex();return Map.copyOf(remapped);
    }
    private static void remapAssignments(List<Playlist> playlists,Map<String,String> remapped){
        for(var playlist:playlists){
            var additions=new LinkedHashMap<String,Boolean>();
            var entries=playlist.tracks.entrySet().iterator();
            while(entries.hasNext()){
                var entry=entries.next();String next=remapped.get(entry.getKey());if(next==null)continue;
                additions.merge(next,entry.getValue(),(old,value)->old&&value);entries.remove();
            }
            for(var entry:additions.entrySet())playlist.tracks.merge(entry.getKey(),entry.getValue(),(old,value)->old&&value);
        }
    }
}

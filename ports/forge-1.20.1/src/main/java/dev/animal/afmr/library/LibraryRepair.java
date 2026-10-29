package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.SongIdentity;
import dev.animal.afmr.library.MusicFiles.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

/** Read-only planning on owned inputs. Applying a reviewed plan belongs to the client. */
public final class LibraryRepair {
    public record Sources(List<Track> tracks,List<String> folders){
        public Sources{tracks=List.copyOf(tracks);folders=List.copyOf(folders);}
    }
    public record Request(boolean folder,String source,Path replacement){}
    public record Move(String id,String before,Track replacement,boolean found,long size,String modified){}
    public record FolderMove(String before,String after){}
    public record Merge(String removed,String retained,String expectedPath){}
    public record Plan(Request request,List<Move> moves,List<FolderMove> folders,List<Merge> merges,int found,List<String> existingFolders){
        public Plan{moves=List.copyOf(moves);folders=List.copyOf(folders);merges=List.copyOf(merges);existingFolders=List.copyOf(existingFolders);}
        public int missing(){return moves.size()-found;}
    }
    private LibraryRepair(){}
    public static Sources capture(Library lib){
        var tracks=new ArrayList<Track>();for(var t:lib.tracks)if(!t.vanilla)tracks.add(Library.copyTrack(t));
        return new Sources(tracks,lib.folders.stream().map(f->f.path).distinct().toList());
    }
    public static Plan plan(Request request,Sources sources,Consumer<ScanProgress> progress,BooleanSupplier cancelled)throws IOException{
        if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
        var moves=new ArrayList<Move>();var folders=new ArrayList<FolderMove>();
        Path oldRoot=request.folder()?Path.of(request.source()).toAbsolutePath().normalize():null;
        Path replacement=request.replacement().toAbsolutePath().normalize();
        if(request.folder()){
            if(!sources.folders().contains(request.source()))throw new IOException("The linked folder was removed; select it again.");
            if(!Files.isDirectory(replacement)||!Files.isReadable(replacement))throw new IOException("Choose a readable replacement folder.");
            if(replacement.startsWith(oldRoot))throw new IOException("Choose the folder's new location outside its old folder.");
            for(String folder:sources.folders()){
                if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
                Path path=Path.of(folder).toAbsolutePath().normalize();
                if(path.startsWith(oldRoot))folders.add(new FolderMove(folder,replacement.resolve(oldRoot.relativize(path)).toString()));
            }
        }else if(!Files.isRegularFile(replacement)||!Files.isReadable(replacement)||!MusicFiles.supported(replacement))
            throw new IOException("Choose an existing readable MP3, FLAC, OGG or WAV file.");
        var affected=new ArrayList<Track>();
        for(var t:sources.tracks()){
            if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            if(request.folder()?Path.of(t.path).toAbsolutePath().normalize().startsWith(oldRoot):t.id.equals(request.source()))affected.add(t);
        }
        if(!request.folder()&&affected.isEmpty())throw new IOException("The song was removed; select it again.");
        if(request.folder()&&affected.stream().noneMatch(track->track.content!=null)){
            var moving=new HashSet<String>();for(var folder:folders)moving.add(folder.before());
            for(var folder:folders)for(String existing:sources.folders())if(!moving.contains(existing)
                &&Path.of(folder.after()).equals(Path.of(existing).toAbsolutePath().normalize()))
                throw new IOException("This folder has no content signatures. Unlink the duplicate destination first, or re-export the setup with song contents identified.");
        }
        var reader=new ContentFiles.Reader(cancelled);var wanted=new HashSet<SongIdentity>();
        for(var track:affected)if(track.content!=null)wanted.add(track.content);
        var matched=request.folder()?ContentFiles.find(List.of(replacement),List.of(),wanted,reader,progress).files():Map.<SongIdentity,Path>of();
        int found=0,processed=0;
        for(var track:affected){
            if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            Path destination=request.folder()?replacement.resolve(oldRoot.relativize(Path.of(track.path).toAbsolutePath().normalize())):replacement;
            if(track.content!=null&&matched.containsKey(track.content))destination=matched.get(track.content);
            boolean exists=false;long size=0;String modified="";Track next;
            try{
                var attributes=Files.readAttributes(destination,BasicFileAttributes.class);
                if(!attributes.isRegularFile()||!Files.isReadable(destination)||!MusicFiles.supported(destination))throw new IOException("Unreadable or unsupported file");
                if(track.content!=null&&!reader.matches(destination,track.content))throw new IOException("The selected file has different contents; choose an identical copy of this song");
                next=MusicFiles.read(destination);exists=true;size=attributes.size();modified=attributes.lastModifiedTime().toString();found++;
                next.content=track.content==null?reader.identify(destination):track.content;
            }catch(IOException|SecurityException e){
                if(!request.folder())throw new IOException("Cannot read the selected song: "+e.getMessage(),e);
                next=Library.copyTrack(track);next.path=track.content==null?destination.toString():track.path;
            }
            next.id=track.id;next.loudness=null;
            moves.add(new Move(track.id,track.path,next,exists,size,modified));
            progress.accept(new ScanProgress(ScanStage.REPAIRING,affected.size(),++processed,destination.toString()));
        }
        if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
        if(request.folder()&&!affected.isEmpty()&&found==0)throw new IOException(wanted.isEmpty()
            ?"No songs matched. Choose the folder containing the same relative paths; renamed songs can be located individually."
            :"No identical song files were found. Choose a folder containing copies of the original files.");
        var unresolved=new ArrayList<Path>();
        for(var move:moves)if(!move.found()&&move.replacement().content!=null)unresolved.add(Path.of(move.before()).toAbsolutePath().normalize());
        folders.removeIf(folder->unresolved.stream().anyMatch(path->path.startsWith(Path.of(folder.before()).toAbsolutePath().normalize())));
        var verifiedIds=new HashSet<String>();for(var track:affected)if(track.content!=null)verifiedIds.add(track.id);
        for(int i=0;i<folders.size();i++){
            var folder=folders.get(i);if(folder.before().equals(request.source()))continue;
            Path before=Path.of(folder.before()).toAbsolutePath().normalize(),common=null;boolean allVerified=true;
            for(var move:moves)if(Path.of(move.before()).toAbsolutePath().normalize().startsWith(before)){
                if(!move.found()||!verifiedIds.contains(move.id())){allVerified=false;break;}
                Path parent=Path.of(move.replacement().path).getParent();
                if(common==null)common=parent;else while(common!=null&&!parent.startsWith(common))common=common.getParent();
            }
            if(allVerified&&common!=null)folders.set(i,new FolderMove(folder.before(),common.toString()));
        }
        var destinations=new HashMap<Path,String>();var movingIds=new HashSet<String>();var merges=new ArrayList<Merge>();
        for(var move:moves){
            if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            String previous=destinations.putIfAbsent(Path.of(move.replacement().path).toAbsolutePath().normalize(),move.id());movingIds.add(move.id());
            if(previous!=null&&!previous.equals(move.id()))merges.add(new Merge(move.id(),previous,move.before()));
        }
        for(var t:sources.tracks()){
            if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            if(!movingIds.contains(t.id)){
                String retained=destinations.get(Path.of(t.path).toAbsolutePath().normalize());
                if(retained!=null)merges.add(new Merge(t.id,retained,t.path));
            }
        }
        if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
        return new Plan(request,moves,folders,merges,found,sources.folders());
    }
    /** Validate the complete reviewed plan before mutating any live state. No filesystem I/O. */
    public static Map<String,String> apply(Library lib,Plan plan){
        var moves=new HashMap<String,Move>();var destinationIds=new HashMap<Path,String>();
        for(var move:plan.moves()){
            var track=lib.track(move.id());
            if(track==null||!track.path.equals(move.before()))throw new IllegalStateException("The library changed; cancel and prepare the repair again.");
            moves.put(move.id(),move);destinationIds.putIfAbsent(Path.of(move.replacement().path).toAbsolutePath().normalize(),move.id());
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
        var targetFolders=new HashSet<Path>();for(String path:folders.values())targetFolders.add(Path.of(path).toAbsolutePath().normalize());
        for(var folder:lib.folders)if(!folders.containsKey(folder.path)&&targetFolders.contains(Path.of(folder.path).toAbsolutePath().normalize())
            &&!plan.existingFolders().contains(folder.path))throw new IllegalStateException("The destination folder was linked while reviewing; prepare the repair again.");
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
        var linked=new HashSet<Path>();
        for(var f:lib.folders)if(!folders.containsKey(f.path))linked.add(Path.of(f.path).toAbsolutePath().normalize());
        Folder preferred=null;
        for(var folder:lib.folders)if(folder.path.equals(plan.request().source())&&folders.containsKey(folder.path)){
            String after=folders.get(folder.path);
            if(linked.add(Path.of(after).toAbsolutePath().normalize())){preferred=folder;folder.path=after;}break;
        }
        var remaining=lib.folders.iterator();
        while(remaining.hasNext()){
            var folder=remaining.next();if(folder==preferred)continue;String after=folders.get(folder.path);if(after==null)continue;
            if(!linked.add(Path.of(after).toAbsolutePath().normalize()))remaining.remove();else folder.path=after;
        }
        for(var profile:lib.profiles.inactive){
            var transferred=new HashMap<String,String>();
            String preferredAfter=folders.get(plan.request().source());
            if(preferredAfter!=null&&profile.folderDestinations.containsKey(plan.request().source()))
                transferred.put(preferredAfter,profile.folderDestinations.get(plan.request().source()));
            for(var move:folders.entrySet())if(profile.folderDestinations.containsKey(move.getKey()))
                transferred.putIfAbsent(move.getValue(),profile.folderDestinations.remove(move.getKey()));
            for(var entry:transferred.entrySet())profile.folderDestinations.putIfAbsent(entry.getKey(),entry.getValue());
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

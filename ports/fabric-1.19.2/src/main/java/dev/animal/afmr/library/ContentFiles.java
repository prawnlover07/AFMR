package dev.animal.afmr.library;

import dev.animal.afmr.core.SongIdentity;
import dev.animal.afmr.library.MusicFiles.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.*;

/** Explicit worker-owned byte verification and recursive search. Never used by playback ticks. */
public final class ContentFiles {
    private ContentFiles(){}
    /** One reusable digest and read buffer per sequential search/export operation. */
    public static final class Reader {
        private final byte[] buffer=new byte[64*1024];
        private final MessageDigest digest;
        private final BooleanSupplier cancelled;
        public Reader(BooleanSupplier cancelled){
            this.cancelled=cancelled;
            try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
        }
        public void checkpoint(){if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new CancellationException();}
        public SongIdentity identify(Path path)throws IOException{
            checkpoint();var before=Files.readAttributes(path,BasicFileAttributes.class);
            if(!before.isRegularFile())throw new IOException("Expected a music file");
            digest.reset();long bytes=0;
            try(var input=Files.newInputStream(path)){
                int count;
                while((count=input.read(buffer))!=-1){checkpoint();digest.update(buffer,0,count);bytes+=count;}
            }
            checkpoint();var after=Files.readAttributes(path,BasicFileAttributes.class);
            if(bytes!=before.size()||after.size()!=before.size()||!after.lastModifiedTime().equals(before.lastModifiedTime())
                ||!Objects.equals(after.fileKey(),before.fileKey()))throw new IOException("File changed while checking its contents");
            return new SongIdentity(bytes,HexFormat.of().formatHex(digest.digest()));
        }
        public boolean matches(Path path,SongIdentity identity)throws IOException{
            checkpoint();return Files.size(path)==identity.size()&&identify(path).equals(identity);
        }
    }
    public record Matches(Map<SongIdentity,Path> files,List<String> errors){
        public Matches{files=Map.copyOf(files);errors=List.copyOf(errors);}
    }
    /** Size first, then SHA-256. Only matching candidates are retained; traversal follows no symlinks. */
    public static Matches find(Collection<Path> folders,Collection<Path> known,Set<SongIdentity> wanted,
                               Reader reader,Consumer<ScanProgress> progress)throws IOException{
        var found=new HashMap<SongIdentity,Path>();var remaining=new HashSet<>(wanted);
        var sizes=new HashMap<Long,Integer>();for(var identity:wanted)sizes.merge(identity.size(),1,Integer::sum);
        var errors=new ArrayList<String>();var checked=new HashSet<Path>();int[] processed={0};
        if(remaining.isEmpty()){reader.checkpoint();return new Matches(found,errors);}
        class Search extends SimpleFileVisitor<Path> {
            void error(Path path,IOException e){if(errors.size()<100)errors.add(path+": "+e.getMessage());}
            void candidate(Path path,long size){
                reader.checkpoint();processed[0]++;
                if(sizes.containsKey(size)&&!remaining.isEmpty())try{
                    var identity=reader.identify(path);
                    if(remaining.remove(identity)){
                        found.put(identity,path.toAbsolutePath().normalize());
                        if(sizes.merge(identity.size(),-1,Integer::sum)==0)sizes.remove(identity.size());
                    }
                }catch(IOException|SecurityException e){if(errors.size()<100)errors.add(path+": "+e.getMessage());}
                progress.accept(new ScanProgress(ScanStage.HASHING,0,processed[0],path.toString()));
            }
            @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs){
                reader.checkpoint();return remaining.isEmpty()?FileVisitResult.TERMINATE:FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path path,BasicFileAttributes attrs){
                reader.checkpoint();path=path.toAbsolutePath().normalize();
                if(attrs.isRegularFile()&&MusicFiles.supported(path)&&!checked.contains(path))candidate(path,attrs.size());
                return remaining.isEmpty()?FileVisitResult.TERMINATE:FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path file,IOException error){
                reader.checkpoint();error(file,error);return FileVisitResult.CONTINUE;
            }
        }
        var search=new Search();
        // Prefer existing catalog paths so applying a match can reuse their stable track IDs.
        for(var original:known){
            reader.checkpoint();if(remaining.isEmpty())break;Path path=original.toAbsolutePath().normalize();
            if(!checked.add(path)||!MusicFiles.supported(path))continue;
            try{var attributes=Files.readAttributes(path,BasicFileAttributes.class);if(attributes.isRegularFile())search.candidate(path,attributes.size());}
            catch(IOException|SecurityException e){if(errors.size()<100)errors.add(path+": "+e.getMessage());}
        }
        var roots=new ArrayList<Path>();
        for(var original:folders){
            reader.checkpoint();Path path=original.toAbsolutePath().normalize();
            if(roots.stream().anyMatch(path::startsWith))continue;
            roots.removeIf(root->root.startsWith(path));roots.add(path);
        }
        roots.sort(Comparator.comparing(Path::toString));
        for(var root:roots){
            reader.checkpoint();if(remaining.isEmpty())break;
            try{Files.walkFileTree(root,search);}catch(IOException e){search.error(root,e);}
            catch(SecurityException e){if(errors.size()<100)errors.add(root+": "+e.getMessage());}
        }
        reader.checkpoint();return new Matches(found,errors);
    }
}

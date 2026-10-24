package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.library.MusicFiles.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/** Filesystem checks run only on the library worker, never during ticks or rendering. */
public final class LibraryHealth {
    public enum Kind { FOLDER, TRACK }
    public record Issue(Kind kind,String id,String path,String title,String message){}
    public record Report(List<Issue> issues,List<String> scanErrors,int foldersChecked,int tracksChecked){
        public Report{issues=List.copyOf(issues);scanErrors=List.copyOf(scanErrors);}
    }
    private LibraryHealth(){}
    public static Report check(LibraryRepair.Sources sources,List<String> errors,Consumer<ScanProgress> progress){
        var issues=new ArrayList<Issue>();var unavailable=new HashMap<Path,String>();
        int total=sources.folders().size()+sources.tracks().size(),processed=0;
        for(String folder:sources.folders()){
            String problem=problem(folder,true);
            if(!problem.isEmpty()){
                issues.add(new Issue(Kind.FOLDER,folder,folder,folder,problem));
                try{unavailable.put(Path.of(folder).toAbsolutePath().normalize(),problem);}catch(InvalidPathException ignored){}
            }
            progress.accept(new ScanProgress(ScanStage.CHECKING,total,++processed,folder));
        }
        for(var track:sources.tracks()){
            if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            String failure="";
            try{
                Path path=Path.of(track.path).toAbsolutePath().normalize();
                for(Path parent=path.getParent();parent!=null;parent=parent.getParent())if(unavailable.containsKey(parent)){
                    failure="Parent folder unavailable: "+parent;break;
                }
            }catch(InvalidPathException e){failure="Invalid saved file path";}
            if(failure.isEmpty())failure=problem(track.path,false);
            if(!failure.isEmpty())issues.add(new Issue(Kind.TRACK,track.id,track.path,track.title,failure));
            progress.accept(new ScanProgress(ScanStage.CHECKING,total,++processed,track.path));
        }
        return new Report(issues,errors,sources.folders().size(),sources.tracks().size());
    }
    private static String problem(String name,boolean folder){
        try{
            Path path=Path.of(name);var attributes=Files.readAttributes(path,BasicFileAttributes.class);
            if(folder?!attributes.isDirectory():!attributes.isRegularFile())return folder?"Expected a folder":"Expected a music file";
            if(!Files.isReadable(path))return "Access denied or source is unreadable";
            return "";
        }catch(NoSuchFileException e){return folder?"Folder not found":"Song not found";}
        catch(AccessDeniedException|SecurityException e){return "Access denied";}
        catch(IOException|InvalidPathException e){return "Source unavailable: "+e.getMessage();}
    }
}

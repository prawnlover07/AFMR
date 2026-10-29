package dev.animal.afmr.core;

import java.nio.file.Path;
import java.util.*;

/** Owned rescan destinations, indexed by directory; built once before starting a shared-folder scan. */
public final class FolderAssignments {
    private record Destination(String profileId,String playlistId,boolean recursive){}
    private final Map<Path,List<Destination>> directories=new HashMap<>();

    public FolderAssignments(Library lib){
        for(var folder:lib.folders){
            Path path=Path.of(folder.path).toAbsolutePath().normalize();
            add(path,lib.profiles.activeId,folder.playlistId,folder.recursive);
            for(var profile:lib.profiles.inactive)
                add(path,profile.id,profile.folderDestinations.getOrDefault(folder.path,"library"),folder.recursive);
        }
    }
    private void add(Path path,String profileId,String playlistId,boolean recursive){
        if(!playlistId.equals("library"))directories.computeIfAbsent(path,unused->new ArrayList<>()).add(new Destination(profileId,playlistId,recursive));
    }
    public void assign(Library lib,String trackId,Path sourcePath){
        Path parent=sourcePath.toAbsolutePath().normalize().getParent();
        boolean direct=true;
        for(Path directory=parent;directory!=null;directory=directory.getParent()){
            var destinations=directories.get(directory);
            if(destinations!=null)for(var destination:destinations)if(direct||destination.recursive)
                lib.assignImported(destination.profileId,destination.playlistId,trackId);
            direct=false;
        }
    }
}

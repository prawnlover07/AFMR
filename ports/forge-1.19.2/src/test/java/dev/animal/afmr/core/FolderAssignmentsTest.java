package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FolderAssignmentsTest {
    @TempDir Path directory;
    @Test void sharedRescanPopulatesAllProfileDestinationsEvenAfterSwitching(){
        var l=Library.defaults();l.folders.add(new Folder(directory.toString(),"home"));String id=l.profiles.create(l,"Other",null);
        l.profiles.activate(l,id);l.folders.getFirst().playlistId="combat";var routes=new FolderAssignments(l);l.profiles.activate(l,"default");
        routes.assign(l,"file:new.mp3",directory.resolve("new.mp3"));assertTrue(l.playlist("home").tracks.containsKey("file:new.mp3"));
        l.profiles.activate(l,id);assertTrue(l.playlist("combat").tracks.containsKey("file:new.mp3"));assertTrue(l.playlist("home").tracks.isEmpty());
    }
    @Test void capturedRoutesDoNotFollowLaterDestinationEdits(){
        var l=Library.defaults();l.folders.add(new Folder(directory.toString(),"home"));var routes=new FolderAssignments(l);
        l.folders.getFirst().playlistId="combat";routes.assign(l,"file:new.mp3",directory.resolve("new.mp3"));
        assertTrue(l.playlist("home").tracks.containsKey("file:new.mp3"));assertTrue(l.playlist("combat").tracks.isEmpty());
    }
    @Test void recursionAndDirectoryBoundariesAreRespected(){
        var l=Library.defaults();var folder=new Folder(directory.resolve("music").toString(),"home");folder.recursive=false;l.folders.add(folder);
        var routes=new FolderAssignments(l);routes.assign(l,"file:music/direct.mp3",directory.resolve("music/direct.mp3"));routes.assign(l,"file:music/sub/nested.mp3",directory.resolve("music/sub/nested.mp3"));routes.assign(l,"file:music-other/no.mp3",directory.resolve("music-other/no.mp3"));
        assertEquals(1,l.playlist("home").tracks.size());assertTrue(l.playlist("home").tracks.containsKey("file:music/direct.mp3"));
        folder.recursive=true;new FolderAssignments(l).assign(l,"file:music/sub/nested.mp3",directory.resolve("music/sub/nested.mp3"));assertEquals(2,l.playlist("home").tracks.size());
    }
    @Test void overlappingFoldersAssignOnceWithoutReenablingDisabledSongs(){
        var l=Library.defaults();l.folders.add(new Folder(directory.toString(),"home"));l.folders.add(new Folder(directory.resolve("sub").toString(),"home"));
        l.playlist("home").tracks.put("file:sub/off.mp3",false);var routes=new FolderAssignments(l);
        routes.assign(l,"file:sub/off.mp3",directory.resolve("sub/off.mp3"));routes.assign(l,"file:sub/new.mp3",directory.resolve("sub/new.mp3"));
        assertEquals(2,l.playlist("home").tracks.size());assertFalse(l.playlist("home").tracks.get("file:sub/off.mp3"));
    }
    @Test void deletedProfilesAndPlaylistsAreNotRedirected(){
        var l=Library.defaults();l.folders.add(new Folder(directory.toString(),"home"));String id=l.profiles.create(l,"Other","default");
        var routes=new FolderAssignments(l);l.profiles.delete(id);l.removePlaylist("home");routes.assign(l,"file:new.mp3",directory.resolve("new.mp3"));
        assertTrue(l.playlists.stream().allMatch(p->p.tracks.isEmpty()));
    }
}

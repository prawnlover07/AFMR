package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistProfilesStoreTest {
    @TempDir Path directory;
    @Test void roundTripKeepsCurrentAndInactivePlaylistsPlaybackAndLinks() throws Exception{
        var l=Library.defaults();l.playlist("home").volume=0.32;l.settings.fadeInSeconds=3;
        l.folders.add(new Folder("music","home"));String id=l.profiles.create(l,"Server","default");
        l.profiles.bind("server:example",id);l.profiles.activate(l,id);l.playlist("home").volume=0.75;l.settings.fadeInSeconds=8;
        l.folders.getFirst().playlistId="combat";
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(l.snapshot());var loaded=store.load();assertEquals(id,loaded.profiles.activeId);assertEquals(0.75,loaded.playlist("home").volume);
            assertEquals(8,loaded.settings.fadeInSeconds);assertEquals(id,loaded.profiles.forWorld("server:example"));assertEquals("combat",loaded.folders.getFirst().playlistId);
            loaded.profiles.activate(loaded,"default");assertEquals(0.32,loaded.playlist("home").volume);assertEquals(3,loaded.settings.fadeInSeconds);
            assertEquals("home",loaded.folders.getFirst().playlistId);
        }
    }
    @Test void unchangedLibrariesWithoutProfileOrVolumeFieldsLoadAsDefault() throws Exception{
        Path file=directory.resolve("library.json");
        Files.writeString(file,"""
            {"settings":{"silenceMinSeconds":4,"silenceMaxSeconds":7},"tracks":[],"folders":[],
             "playlists":[{"id":"custom","name":"Existing","tracks":{"song":false}}]}
            """);
        try(var store=new LibraryStore(file)){
            var l=store.load();assertEquals("default",l.profiles.activeId);assertEquals(1,l.playlist("custom").volume);
            assertFalse(l.playlist("custom").tracks.get("song"));assertEquals(4,l.settings.silenceMinSeconds);
        }
    }
    @Test void libraryMetadataIsStoredOnceAndSharedAfterSwitch() throws Exception{
        var l=Library.defaults();l.tracks.add(new Track("file:one","unique-original-audio-path","Song",false));
        for(int i=0;i<4;i++)l.profiles.create(l,"Copy "+i,"default");Path file=directory.resolve("library.json");
        try(var store=new LibraryStore(file)){
            store.save(l.snapshot());assertEquals(1,Files.readString(file).split("unique-original-audio-path",-1).length-1);
            var loaded=store.load();var tracks=loaded.tracks;loaded.profiles.activate(loaded,loaded.profiles.inactive.getFirst().id);
            assertSame(tracks,loaded.tracks);assertEquals("file:one",loaded.track("file:one").id);
        }
    }
    @Test void asyncSnapshotsDoNotMixSetupsAfterLaterSwitches() throws Exception{
        var l=Library.defaults();String id=l.profiles.create(l,"Other","default");Path file=directory.resolve("library.json");
        try(var store=new LibraryStore(file)){
            l.profiles.activate(l,id);l.playlist("home").volume=0.21;store.saveAsync(l.snapshot());
            l.profiles.activate(l,"default");l.playlist("home").volume=0.87;store.saveAsync(l.snapshot());
            l.playlist("home").volume=0.1;
        }
        try(var store=new LibraryStore(file)){
            var loaded=store.load();assertEquals("default",loaded.profiles.activeId);assertEquals(0.87,loaded.playlist("home").volume);
            loaded.profiles.activate(loaded,id);assertEquals(0.21,loaded.playlist("home").volume);
        }
    }
    @Test void invalidProfilesPreserveTheOriginalFile() throws Exception{
        Path file=directory.resolve("library.json");String broken="""
            {"settings":{},"tracks":[],"folders":[],"playlists":[],
             "profiles":{"activeId":"missing","activeName":"Bad","inactive":[],"bindings":{}}}
            """;
        Files.writeString(file,broken);
        try(var store=new LibraryStore(file)){assertThrows(java.io.IOException.class,store::load);}
        assertEquals(broken,Files.readString(file));
    }
    @Test void inactivePlaylistVolumesAreSavedAndNormalized() throws Exception{
        var l=Library.defaults();String id=l.profiles.create(l,"Other",null);
        l.profiles.inactive.getFirst().playlists.getFirst().volume=0.49;
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(l.snapshot());var loaded=store.load();loaded.profiles.activate(loaded,id);assertEquals(0.49,loaded.playlists.getFirst().volume);
        }
    }
}

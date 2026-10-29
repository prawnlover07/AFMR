package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class LibraryStoreTest{
    @TempDir Path directory;
    @Test void malformedContentIdentityIsRejectedWithoutChangingTheSavedFile()throws Exception{
        Path file=directory.resolve("library.json");
        try(var store=new LibraryStore(file)){
            var library=Library.defaults();var track=new Library.Track("identity","song.wav","Song",false);
            track.content=new dev.animal.afmr.core.SongIdentity(1,"a".repeat(64));library.tracks.add(track);store.save(library);
            assertEquals(track.content,store.load().track(track.id).content);
            var tree=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            tree.getAsJsonArray("tracks").get(0).getAsJsonObject().getAsJsonObject("content").addProperty("size",-1);
            String invalid=tree.toString();Files.writeString(file,invalid);
            assertThrows(java.io.IOException.class,store::load);assertEquals(invalid,Files.readString(file));
        }
    }
    @Test void roundTripPreservesDisabledTracksAndLinkedFolders()throws Exception{
        Path file=directory.resolve("library.json");LibraryStore store=new LibraryStore(file);Library l=Library.defaults();
        var p=l.playlist("combat");p.tracks.put("a",false);p.finishCurrentSong=true;p.finishCurrentSongOnExit=true;l.settings.rescanOnLaunch=true;
        l.folders.add(new Library.Folder(directory.toString(),p.id));store.save(l);
        var read=store.load();assertEquals(Boolean.FALSE,read.playlist("combat").tracks.get("a"));
        assertTrue(read.playlist("combat").finishCurrentSong);assertTrue(read.playlist("combat").finishCurrentSongOnExit);assertTrue(read.settings.rescanOnLaunch);assertEquals(1,read.folders.size());
        store.save(read);assertTrue(Files.exists(directory.resolve("library.json.bak")));
    }
    @Test void malformedLibraryIsPreserved()throws Exception{
        Path file=directory.resolve("library.json");Files.writeString(file,"{ broken");
        assertThrows(java.io.IOException.class,()->new LibraryStore(file).load());assertEquals("{ broken",Files.readString(file));
    }
    @Test void futureSchemaIsNotSilentlyDowngraded()throws Exception{
        Path file=directory.resolve("library.json");Files.writeString(file,"{\"schemaVersion\":99}");
        assertThrows(java.io.IOException.class,()->new LibraryStore(file).load());
    }
    @Test void scanningLinksRecursiveFoldersAndDeduplicatesIdentity()throws Exception{
        Files.createDirectories(directory.resolve("nested"));Files.write(directory.resolve("nested/test.wav"),new byte[0]);
        var shallow=MusicFiles.scan(java.util.List.of(new MusicFiles.Request(directory,"general",false)),java.util.List.of(),p->{});
        assertEquals(0,shallow.found().size());
        var deep=MusicFiles.scan(java.util.List.of(new MusicFiles.Request(directory,"general",true)),java.util.List.of(),p->{});
        assertEquals(1,deep.found().size());assertEquals("test",deep.found().getFirst().track().title);
        var single=MusicFiles.scan(java.util.List.of(new MusicFiles.Request(directory.resolve("nested/test.wav"),"other",false)),java.util.List.of(),p->{});
        assertEquals(deep.found().getFirst().track().id,single.found().getFirst().track().id);
    }
}

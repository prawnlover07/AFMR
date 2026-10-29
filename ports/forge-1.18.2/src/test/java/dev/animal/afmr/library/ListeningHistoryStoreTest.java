package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.core.ListeningHistory;
import dev.animal.afmr.core.ListeningHistory.Origin;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ListeningHistoryStoreTest {
    @TempDir Path directory;
    private final Track track=new Track("song","private-music-path","Song",false);
    @Test void historyFlushesIndependentlyWithoutWritingTheLibraryOrMusicPaths()throws Exception{
        var history=new ListeningHistory();history.record(track,null,Origin.PREVIEW,"Preview");Path file=directory.resolve("library.json");
        try(var store=new LibraryStore(file)){store.saveHistoryAsync(history.snapshot());}
        assertFalse(Files.exists(file));assertFalse(Files.readString(directory.resolve("history.json")).contains(track.path));
        try(var store=new LibraryStore(file)){assertEquals(history.snapshot(),store.loadHistory());}
    }
    @Test void interleavedLibraryAndHistorySavesKeepTheLatestOwnedSnapshots()throws Exception{
        var history=new ListeningHistory();var lib=Library.defaults();Path file=directory.resolve("library.json");
        try(var store=new LibraryStore(file)){
            for(int i=0;i<130;i++){
                lib.playlists.getFirst().name="Playlist "+i;store.saveAsync(lib.snapshot());
                history.record(track,null,Origin.QUEUE,"Queue "+i);store.saveHistoryAsync(history.snapshot());
            }
        }
        try(var store=new LibraryStore(file)){assertEquals("Playlist 129",store.load().playlists.getFirst().name);assertEquals(history.snapshot(),store.loadHistory());assertEquals(100,store.loadHistory().size());}
        assertFalse(Files.exists(directory.resolve("history.json.tmp")));
    }
    @Test void clearWinsOverPendingWritesAndLeavesNoBackupOfTheOldHistory()throws Exception{
        var history=new ListeningHistory();Path file=directory.resolve("library.json");
        try(var store=new LibraryStore(file)){
            for(int i=0;i<30;i++){history.record(track,null,Origin.PREVIEW,"Preview");store.saveHistoryAsync(history.snapshot());}
            history.clear();store.saveHistoryAsync(history.snapshot());
        }
        try(var store=new LibraryStore(file)){assertEquals(List.of(),store.loadHistory());}
        assertFalse(Files.exists(directory.resolve("history.json.bak")));
    }
    @Test void malformedHistoryDoesNotDamageTheLibraryOrOverwriteTheOriginal()throws Exception{
        Path file=directory.resolve("library.json"),history=directory.resolve("history.json");
        try(var store=new LibraryStore(file)){
            store.save(Library.defaults());Files.writeString(history,"not valid JSON");assertThrows(java.io.IOException.class,store::loadHistory);assertEquals(6,store.load().playlists.size());
        }
        assertEquals("not valid JSON",Files.readString(history));
    }
    @Test void missingHistoryLoadsEmptyAndOversizedHistoryIsRejected()throws Exception{
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            assertTrue(store.loadHistory().isEmpty());Files.writeString(directory.resolve("history.json")," ".repeat(2*1024*1024+1));
            assertThrows(java.io.IOException.class,store::loadHistory);
        }
    }
    @Test void failedHistoryWriteIsReportedAndLaterSavesAfterCloseAreRejected()throws Exception{
        var history=new ListeningHistory();history.record(track,null,Origin.QUEUE,"Queued");
        Files.createDirectory(directory.resolve("history.json"));Files.writeString(directory.resolve("history.json/keep"),"unchanged");
        var store=new LibraryStore(directory.resolve("library.json"));store.saveHistoryAsync(history.snapshot());
        assertThrows(java.io.IOException.class,store::close);assertEquals("unchanged",Files.readString(directory.resolve("history.json/keep")));
        assertThrows(IllegalStateException.class,()->store.saveHistoryAsync(List.of()));
    }
}

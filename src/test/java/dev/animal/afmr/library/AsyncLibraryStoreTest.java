package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class AsyncLibraryStoreTest {
    @TempDir Path directory;
    @Test void snapshotDoesNotChangeWithLaterEditsOrScannedMetadata() {
        Library library=Library.defaults();library.tracks.add(new Track("a","path","Original",false));
        library.folders.add(new Folder("folder","day_outside"));library.playlist("day_outside").tracks.put("a",false);
        library.settings.fadeInSeconds=library.settings.fadeOutSeconds=3;
        Library copy=library.snapshot();
        library.tracks.getFirst().title="Changed";library.settings.fadeInSeconds=library.settings.fadeOutSeconds=55;library.playlist("home").conditions.area.radius=200;
        library.folders.getFirst().recursive=false;library.playlist("day_outside").tracks.put("a",true);
        assertEquals("Original",copy.tracks.getFirst().title);assertEquals(3,copy.settings.fadeInSeconds);
        assertEquals(64,copy.playlist("home").conditions.area.radius);assertTrue(copy.folders.getFirst().recursive);
        assertEquals(Boolean.FALSE,copy.playlist("day_outside").tracks.get("a"));
    }
    @Test void rapidSavesFinishWithNewestSnapshotAndShutdownFlushesIt() throws Exception {
        Path file=directory.resolve("library.json");Library library=Library.defaults();
        try(LibraryStore store=new LibraryStore(file)){
            for(int i=0;i<50;i++){library.playlist("home").conditions.area.radius=i+1;store.saveAsync(library.snapshot());}
        }
        Library read=new LibraryStore(file).load();assertEquals(50,read.playlist("home").conditions.area.radius);
        assertEquals(6,read.playlists.size());assertFalse(Files.exists(directory.resolve("library.json.tmp")));
    }
    @Test void failedBackgroundWriteIsReportedWithoutOverwritingOriginal() throws Exception {
        Path blocker=directory.resolve("not-a-folder");Files.writeString(blocker,"keep me");
        LibraryStore store=new LibraryStore(blocker.resolve("library.json"));store.saveAsync(Library.defaults().snapshot());
        assertThrows(java.io.IOException.class,store::close);
        assertEquals("keep me",Files.readString(blocker));
    }
}

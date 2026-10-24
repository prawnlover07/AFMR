package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentSettingsStoreTest {
    @TempDir Path directory;
    @Test void deletedTemplateStaysDeletedAndPreservesChoices() throws Exception {
        Library old=Library.defaults();old.playlists.removeIf(p->p.id.equals("dark_exploration"));
        Collections.swap(old.playlists,0,1);old.playlist("underground").enabled=false;old.playlist("underground").tracks.put("song",false);
        Path file=directory.resolve("library.json");var store=new LibraryStore(file);store.save(old);
        Library loaded=store.load();
        assertEquals(List.of("home","combat","underground","night_outside","day_outside"),loaded.playlists.stream().map(p->p.id).toList());
        assertFalse(loaded.playlist("underground").enabled);assertEquals(Boolean.FALSE,loaded.playlist("underground").tracks.get("song"));
        store.save(loaded);assertEquals(5,store.load().playlists.size());
    }
    @Test void conditionSettingsAndCustomPrioritySurviveSaveAndLoad() throws Exception {
        Library library=Library.defaults();library.playlist("underground").conditions.maxY=-10;library.playlist("underground").conditions.minCoverDepth=3;library.playlist("dark_exploration").conditions.groups.getFirst().maxLight=6;
        library.playlist("dark_exploration").conditions.groups.get(1).enabled=true;library.playlist("dark_exploration").conditions.groups.get(2).enabled=true;
        var dark=library.playlist("dark_exploration");library.playlists.remove(dark);library.playlists.add(dark);dark.enabled=false;
        var store=new LibraryStore(directory.resolve("library.json"));store.save(library);var loaded=store.load();
        assertEquals(-10,loaded.playlist("underground").conditions.maxY);assertEquals(3,loaded.playlist("underground").conditions.minCoverDepth);assertEquals(6,loaded.playlist("dark_exploration").conditions.groups.getFirst().maxLight);
        assertTrue(loaded.playlist("dark_exploration").conditions.groups.get(1).enabled);assertTrue(loaded.playlist("dark_exploration").conditions.groups.get(2).enabled);
        assertEquals("dark_exploration",loaded.playlists.getLast().id);assertFalse(loaded.playlist("dark_exploration").enabled);
    }
}

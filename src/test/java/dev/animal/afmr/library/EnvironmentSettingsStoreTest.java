package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentSettingsStoreTest {
    @TempDir Path directory;
    @Test void loadingExistingLibraryAddsOnlyMissingDarkPlaylistAndPreservesChoices() throws Exception {
        Library old=Library.defaults();old.playlists.removeIf(p->p.id.equals("dark_exploration"));
        Collections.swap(old.playlists,0,1);old.playlist("underground").enabled=false;old.playlist("underground").tracks.put("song",false);
        Path file=directory.resolve("library.json");var store=new LibraryStore(file);store.save(old);
        Library loaded=store.load();
        assertEquals(List.of("home","combat","dark_exploration","underground","night_outside","day_outside"),loaded.playlists.stream().map(p->p.id).toList());
        assertFalse(loaded.playlist("underground").enabled);assertEquals(Boolean.FALSE,loaded.playlist("underground").tracks.get("song"));
        store.save(loaded);assertEquals(6,store.load().playlists.size());
    }
    @Test void conditionSettingsAndCustomPrioritySurviveSaveAndLoad() throws Exception {
        Library library=Library.defaults();library.settings.undergroundMaxY=-10;library.settings.undergroundDepth=3;library.settings.darkMaxLight=6;
        library.settings.darkInNether=true;library.settings.darkInEnd=true;
        var dark=library.playlist("dark_exploration");library.playlists.remove(dark);library.playlists.add(dark);dark.enabled=false;
        var store=new LibraryStore(directory.resolve("library.json"));store.save(library);var loaded=store.load();
        assertEquals(-10,loaded.settings.undergroundMaxY);assertEquals(3,loaded.settings.undergroundDepth);assertEquals(6,loaded.settings.darkMaxLight);
        assertTrue(loaded.settings.darkInNether);assertTrue(loaded.settings.darkInEnd);
        assertEquals("dark_exploration",loaded.playlists.getLast().id);assertFalse(loaded.playlist("dark_exploration").enabled);
    }
}

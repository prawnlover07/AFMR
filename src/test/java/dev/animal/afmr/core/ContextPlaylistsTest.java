package dev.animal.afmr.core;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ContextPlaylistsTest {
    @TempDir Path directory;
    Context context(boolean day,boolean underground,boolean home,boolean combat){return new Context("minecraft:overworld","",combat,false,(day?6000:18000),underground,home,underground,15,"test-world",0,64,0,(underground?8:0),0.8,false);}
    @Test void onlySixCentralPlaylists(){assertEquals(List.of("combat","home","dark_exploration","underground","night_outside","day_outside"),Library.defaults().playlists.stream().map(p->p.id).toList());}
    @Test void outsideRulesExcludeUnderground(){var l=Library.defaults();assertEquals("day_outside",l.select(context(true,false,false,false)).id);assertEquals("night_outside",l.select(context(false,false,false,false)).id);assertEquals("underground",l.select(context(true,true,false,false)).id);assertEquals("underground",l.select(context(false,true,false,false)).id);}
    @Test void combatThenHomeOverrideOutsideAndUnderground(){var l=Library.defaults();assertEquals("home",l.select(context(true,true,true,false)).id);assertEquals("combat",l.select(context(true,true,true,true)).id);l.settings.combatEnabled=false;assertEquals("home",l.select(context(true,true,true,true)).id);l.playlist("home").enabled=false;assertEquals("underground",l.select(context(true,true,true,true)).id);}
    @Test void radiusIncludesBoundaryAndChecksVerticalDistance(){var h=new Home();h.configured=true;h.world="test-world";h.y=64;h.radius=10;assertTrue(h.contains(context(true,false,false,false)));assertTrue(h.contains(new Context("minecraft:overworld","",false,false,6000,false,false,false,15,"test-world",6,72,0,0,0.8,false)));assertFalse(h.contains(new Context("minecraft:overworld","",false,false,6000,false,false,false,15,"test-world",0,75,0,0,0.8,false)));}
    @Test void homeNeverLeaksBetweenWorldsOrDimensions(){var h=new Home();h.configured=true;h.world="another-world";assertFalse(h.contains(context(true,false,false,false)));h.world="test-world";h.dimension="minecraft:the_nether";assertFalse(h.contains(context(true,false,false,false)));h.configured=false;assertFalse(h.contains(context(true,false,false,false)));}
    @Test void priorityRemainsEditable(){var l=Library.defaults();Collections.swap(l.playlists,0,1);assertEquals("home",l.select(context(true,false,true,true)).id);}
    @Test void migrationPreservesMusicAndDisabledAssignments()throws Exception{
        Library old=new Library();old.schemaVersion=1;
        Playlist general=new Playlist("general","General",Rule.GENERAL,"");general.tracks.put("song",false);general.includeVanilla=false;old.playlists.add(general);
        Playlist biome=new Playlist("biome:forest","Forest",Rule.BIOME,"minecraft:forest");biome.tracks.put("forest-song",true);old.playlists.add(biome);
        old.folders.add(new Folder(directory.toString(),"general"));old.tracks.add(new Track("song","path","Song",false));
        Path config=directory.resolve("library.json");new LibraryStore(config).save(old);Library upgraded=new LibraryStore(config).load();
        assertEquals(2,upgraded.schemaVersion);assertEquals(6,upgraded.playlists.size());assertEquals(1,upgraded.tracks.size());
        for(String id:List.of("day_outside","night_outside","underground"))assertEquals(Boolean.FALSE,upgraded.playlist(id).tracks.get("song"));
        assertTrue(upgraded.legacyPlaylists.stream().anyMatch(p->p.tracks.containsKey("forest-song")));
        assertEquals("ambient",upgraded.folders.getFirst().playlistId);assertTrue(Files.exists(directory.resolve("library-v1-before-upgrade.json")));
    }
    @Test void newImportsStayInLibraryUntilAssigned(){var l=Library.defaults();l.assignImported("library","new");assertTrue(l.playlists.stream().allMatch(p->p.tracks.isEmpty()));l.assignImported("home","new");assertEquals(Boolean.TRUE,l.playlist("home").tracks.get("new"));}
    @Test void legacyLinkedFolderRescanPreservesOffAndAddsToAmbient(){var l=Library.defaults();l.playlist("day_outside").tracks.put("existing",false);l.assignImported("ambient","existing");l.assignImported("ambient","new");assertEquals(Boolean.FALSE,l.playlist("day_outside").tracks.get("existing"));assertTrue(l.playlist("night_outside").tracks.get("new"));assertTrue(l.playlist("underground").tracks.get("new"));}
}

package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.WidgetCorner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistTemplateStoreTest {
    @TempDir Path directory;
    @Test void savedRulesExpandOnceWithoutChangingPlaylistIdentityOrPlaybackChoices()throws Exception{
        Path file=directory.resolve("library.json");String original="""
            {"schemaVersion":2,"settings":{"undergroundMaxY":-10,"undergroundDepth":3,"darkMaxLight":6,"darkInNether":true},
            "home":{"configured":true,"world":"world","dimension":"test:home","x":4,"y":5,"z":6,"radius":25},
            "playlists":[
            {"id":"renamed-dark-id","name":"My darkness","rule":"DARK_EXPLORATION","match":"","enabled":false,"manualOnly":true,"finishCurrentSong":true,"finishCurrentSongOnExit":true,"includeVanilla":false,"tracks":{"a":false}},
            {"id":"home","name":"Base","rule":"HOME","match":"","tracks":{}},
            {"id":"day_outside","name":"Day","rule":"DAY_OUTSIDE","match":"","tracks":{}},
            {"id":"underground","name":"Caves","rule":"UNDERGROUND","match":"","tracks":{}}
            ]}
            """;
        Files.writeString(file,original);
        try(var store=new LibraryStore(file)){
            var lib=store.load();assertEquals(original,Files.readString(file));assertEquals(4,lib.playlists.size());
            var dark=lib.playlist("renamed-dark-id");assertEquals("My darkness",dark.name);assertFalse(dark.enabled);assertTrue(dark.manualOnly);
            assertTrue(dark.finishCurrentSong);assertTrue(dark.finishCurrentSongOnExit);assertEquals(Boolean.FALSE,dark.tracks.get("a"));
            assertEquals(6,dark.conditions.groups.getFirst().maxLight);assertTrue(dark.conditions.groups.get(1).enabled);assertFalse(dark.conditions.groups.get(2).enabled);
            var area=lib.playlist("home").conditions.area;assertTrue(area.configured);assertEquals("world",area.world);assertEquals("test:home",area.dimension);assertEquals(25,area.radius);assertEquals(5,area.y);
            assertEquals(-10,lib.playlist("underground").conditions.maxY);assertEquals(3,lib.playlist("underground").conditions.minCoverDepth);
            assertEquals(-10,lib.playlist("day_outside").conditions.groups.getFirst().maxY);
            dark.conditions.groups.getFirst().maxLight=1;lib.removePlaylist("home");store.save(lib);
            String saved=Files.readString(file);assertFalse(saved.contains("\"rule\""));assertFalse(saved.contains("undergroundMaxY"));assertFalse(saved.contains("\"home\": {"));
            var reloaded=store.load();assertNull(reloaded.playlist("home"));assertEquals(3,reloaded.playlists.size());assertEquals(1,reloaded.playlist(dark.id).conditions.groups.getFirst().maxLight);
        }
    }
    @Test void groupsClonesAndCornerSelectionRoundTripIndependently()throws Exception{
        var lib=Library.defaults();var source=lib.playlist("dark_exploration");var copy=lib.clonePlaylist(source.id,"Branch");
        copy.conditions.groups.get(1).enabled=true;copy.conditions.groups.getFirst().groups.getFirst().negated=false;
        lib.settings.widgetCorner=WidgetCorner.BOTTOM_RIGHT;
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(lib);var read=store.load();assertEquals(WidgetCorner.BOTTOM_RIGHT,read.settings.widgetCorner);
            assertTrue(read.playlist(copy.id).conditions.groups.get(1).enabled);assertFalse(read.playlist(source.id).conditions.groups.get(1).enabled);
            assertFalse(read.playlist(copy.id).conditions.groups.getFirst().groups.getFirst().negated);assertTrue(read.playlist(source.id).conditions.groups.getFirst().groups.getFirst().negated);
        }
        var snapshot=lib.snapshot();lib.settings.widgetCorner=WidgetCorner.TOP_LEFT;assertEquals(WidgetCorner.BOTTOM_RIGHT,snapshot.settings.widgetCorner);
        lib.settings.widgetCorner=null;lib.normalize();assertEquals(WidgetCorner.DEFAULT,lib.settings.widgetCorner);
    }
    @Test void unknownSavedRulePreservesTheOriginalFile()throws Exception{
        Path file=directory.resolve("library.json");String json="{\"playlists\":[{\"id\":\"a\",\"rule\":\"UNKNOWN\"}]}";Files.writeString(file,json);
        try(var store=new LibraryStore(file)){assertThrows(java.io.IOException.class,store::load);}
        assertEquals(json,Files.readString(file));
    }
}

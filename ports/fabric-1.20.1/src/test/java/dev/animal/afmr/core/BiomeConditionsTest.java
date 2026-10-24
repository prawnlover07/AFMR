package dev.animal.afmr.core;

import com.google.gson.Gson;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BiomeConditionsTest {
    @TempDir Path directory;
    private final Settings settings=new Settings();
    private Context context(String biome,int time){return new Context("minecraft:overworld",biome,false,false,time,false,15,"world",0,80,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false);}
    private PlaylistConditions forest(){var c=new PlaylistConditions();c.biomeEnabled=true;c.biomes.addAll(List.of("minecraft:forest","example:autumn_forest"));return c;}

    @Test void selectedBiomesMatchAnyExactIdIncludingModdedBiomes(){
        var c=forest();assertTrue(c.matches(context("minecraft:forest",6000),settings));assertTrue(c.matches(context("example:autumn_forest",6000),settings));
        assertFalse(c.matches(context("minecraft:flower_forest",6000),settings));assertFalse(c.matches(context("another:autumn_forest",6000),settings));
        assertFalse(c.matches(context("",6000),settings));
    }
    @Test void disablingPreservesSelectionButAllowsOtherBiomes(){
        var c=forest();c.biomeEnabled=false;assertTrue(c.matches(context("minecraft:desert",6000),settings));
        assertEquals(2,c.biomes.size());c.biomeEnabled=true;assertFalse(c.matches(context("minecraft:desert",6000),settings));
    }
    @Test void enabledEmptyFilterDoesNotBecomeAnUnconditionalPlaylist(){
        var c=new PlaylistConditions();c.biomeEnabled=true;assertFalse(c.matches(context("minecraft:forest",6000),settings));
        c.biomeEnabled=false;assertTrue(c.matches(context("minecraft:forest",6000),settings));
    }
    @Test void biomeFilterAndOtherEnabledConditionsMustAllMatch(){
        var c=forest();c.timeEnabled=true;c.startTime=12000;c.endTime=23999;c.temperatureEnabled=true;c.maxTemperature=1;
        assertTrue(c.matches(context("minecraft:forest",18000),settings));assertFalse(c.matches(context("minecraft:forest",6000),settings));
        assertFalse(c.matches(context("minecraft:desert",18000),settings));c.maxTemperature=0.5;assertFalse(c.matches(context("minecraft:forest",18000),settings));
    }
    @Test void crossingBiomesFallsBackWithoutChangingDefaultPlaylistRules(){
        var lib=Library.defaults();var p=lib.createPlaylist("Forest");p.conditions=forest();
        lib.tracks.add(new Track("song","file","Song",false));p.tracks.put("song",true);lib.playlist("day_outside").tracks.put("song",true);
        assertSame(p,lib.selectPlayable(context("minecraft:forest",6000),Set.of(),Set.of()));
        assertEquals("day_outside",lib.selectPlayable(context("minecraft:desert",6000),Set.of(),Set.of()).id);
        assertTrue(lib.playlist("day_outside").matches(context("minecraft:desert",6000),settings));
    }
    @Test void snapshotOwnsBiomeSelectionAndJsonRetainsUnknownModIds() throws Exception {
        var lib=Library.defaults();var p=lib.createPlaylist("Forest");p.conditions=forest();var snapshot=lib.snapshot();
        p.conditions.biomes.clear();p.conditions.biomeEnabled=false;
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(snapshot);var restored=store.load().playlist(p.id).conditions;
            assertTrue(restored.biomeEnabled);assertEquals(Set.of("minecraft:forest","example:autumn_forest"),restored.biomes);
            assertTrue(restored.matches(context("example:autumn_forest",6000),settings));}
    }
    @Test void existingCustomConditionsLoadWithBiomeFilterOff() throws Exception {
        var lib=Library.defaults();var p=lib.createPlaylist("Existing");p.conditions.timeEnabled=true;p.conditions.startTime=12000;p.conditions.endTime=23999;p.tracks.put("song",false);
        var json=new Gson().toJsonTree(lib).getAsJsonObject();
        for(var entry:json.getAsJsonArray("playlists")){var c=entry.getAsJsonObject().getAsJsonObject("conditions");c.remove("biomes");c.remove("biomeEnabled");}
        Path file=directory.resolve("library.json");Files.writeString(file,json.toString());
        try(var store=new LibraryStore(file)){var restored=store.load().playlist(p.id);assertFalse(restored.conditions.biomeEnabled);assertTrue(restored.conditions.biomes.isEmpty());
            assertTrue(restored.conditions.matches(context("minecraft:desert",18000),settings));assertFalse(restored.conditions.matches(context("minecraft:forest",6000),settings));assertEquals(Boolean.FALSE,restored.tracks.get("song"));}
    }
    @Test void normalizationRemovesEmptyIdsAndKeepsUnavailableSelections(){
        var c=forest();c.biomes.addAll(Arrays.asList(null," "," example:autumn_forest "));c.normalize();
        assertEquals(Set.of("minecraft:forest","example:autumn_forest"),c.biomes);assertEquals("biomes: 2 selected",c.summary());
        c.biomes=null;c.normalize();assertTrue(c.biomes.isEmpty());assertFalse(c.matches(context("minecraft:forest",6000),settings));
    }
    @Test void biomePlaybackUsesTheTrackIndexWithSeventyThousandSongs(){
        var lib=Library.defaults();var tracks=new LibraryIndexTest.GuardedTracks();lib.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("t"+i,"file","Song",false));
        var p=lib.createPlaylist("Forest");p.conditions=forest();p.tracks.put("t69999",true);
        lib.playlist("day_outside").tracks.put("t0",true);lib.rebuildTrackIndex();tracks.locked=true;
        for(int i=0;i<200;i++)assertEquals(i%2==0?p.id:"day_outside",lib.selectPlayable(context(i%2==0?"minecraft:forest":"minecraft:desert",6000),Set.of(),Set.of()).id);
    }
}

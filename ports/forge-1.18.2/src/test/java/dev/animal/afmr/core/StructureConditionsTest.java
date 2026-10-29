package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StructureConditionsTest {
    @TempDir Path directory;
    private final Settings settings=new Settings();
    private final StructureContext.Bounds village=new StructureContext.Bounds("minecraft:village_plains",-20,60,-20,20,80,20);
    private final StructureContext.Bounds modded=new StructureContext.Bounds("example:castle",100,60,100,120,90,120);
    private final StructureContext observed=new StructureContext(true,List.of(village,modded));
    private Context context(double x,double y,double z,StructureContext structures){
        return new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"world",x,y,z,0,0.8,false,structures,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    }
    private PlaylistConditions villages(){
        var c=new PlaylistConditions();c.structureEnabled=true;c.structures.add(village.id());return c;
    }
    @Test void insideFullBoundsIncludesEdgesAndTheWholeLastBlock(){
        var c=villages();
        assertTrue(c.matches(context(-20,60,-20,observed),settings));
        assertTrue(c.matches(context(20.99,80.99,20.99,observed),settings));
        assertFalse(c.matches(context(21,70,0,observed),settings));
        assertFalse(c.matches(context(-20.01,70,0,observed),settings));
        assertFalse(c.matches(context(0,59.99,0,observed),settings));
        assertFalse(c.matches(context(0,81,0,observed),settings));
    }
    @Test void radiusUsesDistanceFromBoundsIncludingVerticalDistance(){
        var c=villages();c.structureRadius=5;
        assertTrue(c.matches(context(25,70,0,observed),settings));
        assertTrue(c.matches(context(23,84,20,observed),settings)); // 3-4-5 corner, inclusive
        assertTrue(c.matches(context(0,85,0,observed),settings));
        assertFalse(c.matches(context(24,84,20,observed),settings)); // not an expanded cube
        assertFalse(c.matches(context(26,70,0,observed),settings));
        assertFalse(c.matches(context(0,86,0,observed),settings));
        c.structureRadius=4.9;assertFalse(c.matches(context(23,84,20,observed),settings));
    }
    @Test void anySelectedExactIdCanMatchIncludingModdedStructures(){
        var c=villages();c.structures.add(modded.id());
        assertTrue(c.matches(context(110,70,110,observed),settings));
        c.structures.clear();c.structures.add("other:castle");
        assertFalse(c.matches(context(110,70,110,observed),settings));
    }
    @Test void emptyOrUnavailableDataNeverActivatesAnEnabledFilter(){
        var c=villages();
        assertEquals(PlaylistConditions.Failure.STRUCTURE_DATA,c.failure(context(0,70,0,StructureContext.UNAVAILABLE),settings));
        assertFalse(c.matches(context(0,70,0,new StructureContext(true,List.of())),settings));
        c.structures.clear();assertFalse(c.matches(context(0,70,0,observed),settings));
        c.structureEnabled=false;assertTrue(c.matches(context(0,70,0,StructureContext.UNAVAILABLE),settings));
    }
    @Test void turningFilterOffPreservesSelectionsAndRadius(){
        var c=villages();c.structureRadius=50;c.structureEnabled=false;
        assertTrue(c.matches(context(900,70,900,StructureContext.UNAVAILABLE),settings));
        assertEquals(Set.of(village.id()),c.structures);assertEquals(50,c.structureRadius);
        c.structureEnabled=true;assertFalse(c.matches(context(900,70,900,observed),settings));
    }
    @Test void structureAndBiomeAndTimeAndDimensionAllHaveToMatch(){
        var c=villages();c.biomeEnabled=true;c.biomes.add("minecraft:plains");
        c.timeEnabled=true;c.startTime=5000;c.endTime=7000;
        c.dimensionEnabled=true;c.dimension="minecraft:overworld";
        assertTrue(c.matches(context(0,70,0,observed),settings));
        c.biomes.clear();assertFalse(c.matches(context(0,70,0,observed),settings));
        c.biomeEnabled=false;c.startTime=12000;c.endTime=23999;assertFalse(c.matches(context(0,70,0,observed),settings));
        c.timeEnabled=false;c.dimension="minecraft:the_nether";assertFalse(c.matches(context(0,70,0,observed),settings));
    }
    @Test void crossingStructureBoundsAndUnavailableMultiplayerDataFallBack(){
        var lib=Library.defaults();var p=lib.createPlaylist("Village");p.conditions=villages();
        lib.tracks.add(new Track("song","file","Song",false));p.tracks.put("song",true);lib.playlist("day_outside").tracks.put("song",true);
        assertSame(p,lib.selectPlayable(context(0,70,0,observed),Set.of(),Set.of()));
        assertEquals("day_outside",lib.selectPlayable(context(21,70,0,observed),Set.of(),Set.of()).id);
        assertEquals("day_outside",lib.selectPlayable(context(0,70,0,StructureContext.UNAVAILABLE),Set.of(),Set.of()).id);
    }
    @Test void higherPriorityAndEmptyPlaylistsRetainTheirNormalBehavior(){
        var lib=Library.defaults();var p=lib.createPlaylist("Village");p.conditions=villages();
        lib.tracks.add(new Track("song","file","Song",false));lib.playlist("day_outside").tracks.put("song",true);
        assertEquals("day_outside",lib.selectPlayable(context(0,70,0,observed),Set.of(),Set.of()).id);
        p.tracks.put("song",true);lib.playlist("home").tracks.put("song",true);
        lib.playlist("home").autoplayHere(context(0,70,0,observed));
        assertEquals("home",lib.selectPlayable(context(0,70,0,observed),Set.of(),Set.of()).id);
    }
    @Test void snapshotsAndSaveLoadRetainOwnedSelectionsAndRadius() throws Exception {
        var lib=Library.defaults();var p=lib.createPlaylist("Village");p.conditions=villages();p.conditions.structures.add(modded.id());p.conditions.structureRadius=32.5;
        var snapshot=lib.snapshot();p.conditions.structures.clear();p.conditions.structureRadius=0;p.conditions.structureEnabled=false;
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(snapshot);var restored=store.load().playlist(p.id).conditions;
            assertTrue(restored.structureEnabled);assertEquals(Set.of(village.id(),modded.id()),restored.structures);assertEquals(32.5,restored.structureRadius);
            assertTrue(restored.matches(context(110,70,110,observed),settings));
        }
    }
    @Test void normalizationBoundsRadiusAndPreservesUnavailableIds(){
        var c=villages();c.structures.addAll(Arrays.asList(null," "," example:castle "));c.structureRadius=Double.NaN;c.normalize();
        assertEquals(Set.of(village.id(),modded.id()),c.structures);assertEquals(0,c.structureRadius);
        c.structureRadius=-1;c.normalize();assertEquals(0,c.structureRadius);
        c.structureRadius=100000;c.normalize();assertEquals(StructureContext.MAX_RADIUS,c.structureRadius);
        c.structureRadius=Double.POSITIVE_INFINITY;c.normalize();assertEquals(0,c.structureRadius);
        c.structures=null;c.normalize();assertTrue(c.structures.isEmpty());assertTrue(c.structureEnabled);
    }
    @Test void observationsOwnTheirBoundsAndContextsKeepThem(){
        var boxes=new ArrayList<>(List.of(village));var observation=new StructureContext(true,boxes);boxes.clear();
        assertEquals(1,observation.bounds().size());assertThrows(UnsupportedOperationException.class,()->observation.bounds().clear());
        assertSame(observation,context(0,70,0,observation).structures());
    }
    @Test void inspectorExplainsMissingDataAndSelectedStructures(){
        var lib=Library.defaults();var p=lib.createPlaylist("Village");p.conditions=villages();
        var entry=PlaybackInspector.inspect(lib,context(0,70,0,StructureContext.UNAVAILABLE),Set.of(),new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1)))
            .stream().filter(e->e.id().equals(p.id)).findFirst().orElseThrow();
        assertTrue(entry.status().contains("Structure data unavailable"));
        assertTrue(entry.details().contains("Allowed structure: "+village.id()));
    }
    @Test void structurePlaybackRetainsIndexedSongResolution(){
        var lib=Library.defaults();var tracks=new LibraryIndexTest.GuardedTracks();lib.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("t"+i,"file","Song",false));
        var p=lib.createPlaylist("Village");p.conditions=villages();p.tracks.put("t69999",true);
        lib.playlist("day_outside").tracks.put("t0",true);lib.rebuildTrackIndex();tracks.locked=true;
        for(int i=0;i<100;i++)assertEquals(i%2==0?p.id:"day_outside",lib.selectPlayable(context(i%2==0?0:1000,70,0,observed),Set.of(),Set.of()).id);
    }
}

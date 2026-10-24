package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HeldItemConditionsTest {
    @TempDir Path directory;
    private static final String ROD="minecraft:fishing_rod",SHIELD="minecraft:shield";
    private final Settings settings=new Settings();
    private Context context(String main,String off){
        return new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"world",0,70,0,0,0.8,false,
            StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,main,off,GameMode.SURVIVAL,false);
    }
    private PlaylistConditions fishing(){var c=new PlaylistConditions();c.heldItemEnabled=true;c.heldItems.add(ROD);return c;}
    @Test void eitherHandAcceptsTheRodInEitherSlotAndRejectsEmptyHands(){
        var c=fishing();assertTrue(c.matches(context(ROD,SHIELD),settings));assertTrue(c.matches(context(SHIELD,ROD),settings));
        assertFalse(c.matches(context("",""),settings));assertFalse(c.matches(context(SHIELD,""),settings));
        assertEquals(Failure.HELD_ITEM,c.failure(context(SHIELD,""),settings));
    }
    @Test void mainAndOffHandAreIndependent(){
        var c=fishing();c.heldItemHand=Hand.MAIN_HAND;
        assertTrue(c.matches(context(ROD,SHIELD),settings));assertFalse(c.matches(context(SHIELD,ROD),settings));
        c.heldItemHand=Hand.OFF_HAND;
        assertTrue(c.matches(context(SHIELD,ROD),settings));assertFalse(c.matches(context(ROD,SHIELD),settings));
    }
    @Test void selectedItemsAreExactAlternativesIncludingModdedIds(){
        var c=fishing();c.heldItems.add("example:net");
        assertTrue(c.matches(context("example:net",""),settings));
        assertFalse(c.matches(context("other:net",""),settings));assertFalse(c.matches(context("minecraft:carrot_on_a_stick",""),settings));
    }
    @Test void enabledEmptySelectionFailsButDisabledRetainsChoices(){
        var c=fishing();c.heldItems.clear();assertFalse(c.matches(context(ROD,""),settings));
        c.heldItemEnabled=false;assertTrue(c.matches(context("",""),settings));
        c.heldItems.add(ROD);c.heldItemHand=Hand.OFF_HAND;
        assertTrue(c.matches(context(SHIELD,""),settings));assertEquals(Set.of(ROD),c.heldItems);assertEquals(Hand.OFF_HAND,c.heldItemHand);
    }
    @Test void blankSelectionsCannotMatchEmptyHands(){
        var c=fishing();c.heldItems.clear();c.heldItems.add("");
        assertFalse(c.matches(context("",""),settings));
    }
    @Test void heldItemsCombineWithWeatherInAllAndAnyModes(){
        var c=fishing();c.weatherEnabled=true;c.weather=Weather.RAIN;
        assertFalse(c.matches(context(ROD,""),settings));c.matchAny=true;assertTrue(c.matches(context(ROD,""),settings));
        assertFalse(c.matches(context(SHIELD,""),settings));c.weather=Weather.CLEAR;assertTrue(c.matches(context(SHIELD,""),settings));
    }
    @Test void nestedNotAndDisabledGroupsUseTheSameHeldItemPredicate(){
        var root=new PlaylistConditions();var child=fishing();child.negated=true;root.groups.add(child);
        assertFalse(root.matches(context(ROD,""),settings));assertTrue(root.matches(context(SHIELD,""),settings));
        child.enabled=false;assertTrue(root.matches(context(ROD,""),settings));
        child.enabled=true;root.matchAny=true;root.weatherEnabled=true;root.weather=Weather.CLEAR;
        assertTrue(root.matches(context(ROD,""),settings));
    }
    @Test void switchingItemsChangesTheAutomaticWinnerWithNormalPriorityAndEmptyFallthrough(){
        var lib=Library.defaults();var p=lib.createPlaylist("Fishing");p.conditions=fishing();
        lib.tracks.add(new Track("song","file","Song",false));lib.playlist("day_outside").tracks.put("song",true);
        assertEquals("day_outside",lib.selectPlayable(context(ROD,""),Set.of(),Set.of()).id);
        p.tracks.put("song",true);assertSame(p,lib.selectPlayable(context(ROD,""),Set.of(),Set.of()));
        assertEquals("day_outside",lib.selectPlayable(context(SHIELD,""),Set.of(),Set.of()).id);
        p.tracks.put("song",false);assertEquals("day_outside",lib.selectPlayable(context(ROD,""),Set.of(),Set.of()).id);
    }
    @Test void overridesStillBypassHeldItemConditionsAndAutomaticOff(){
        var lib=Library.defaults();var p=lib.createPlaylist("Fishing");p.conditions=fishing();p.enabled=false;
        lib.tracks.add(new Track("song","file","Song",false));p.tracks.put("song",true);
        var engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));
        assertTrue(engine.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED,0,lib,Set.of()));
        engine.tick(0,lib,context("",""),Set.of());assertSame(p,engine.selected());
        p.tracks.put("song",false);assertNull(lib.selectPlayable(context(ROD,""),Set.of(),Set.of()));
        assertFalse(new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1)).overridePlaylist(p.id,OverrideMode.ONE_SONG,0,lib,Set.of()));
    }
    @Test void clonesAndSaveSnapshotsOwnNestedHeldItemSelections() throws Exception {
        var lib=Library.defaults();var p=lib.createPlaylist("Fishing");var group=fishing();group.heldItems.add("example:net");group.heldItemHand=Hand.OFF_HAND;p.conditions.groups.add(group);
        var clone=lib.clonePlaylist(p.id,"Fishing copy");clone.conditions.groups.getFirst().heldItems.clear();
        assertEquals(Set.of(ROD,"example:net"),group.heldItems);
        var snapshot=lib.snapshot();group.heldItems.clear();group.heldItemEnabled=false;group.heldItemHand=Hand.MAIN_HAND;
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(snapshot);var restored=store.load().playlist(p.id).conditions.groups.getFirst();
            assertTrue(restored.heldItemEnabled);assertEquals(Set.of(ROD,"example:net"),restored.heldItems);assertEquals(Hand.OFF_HAND,restored.heldItemHand);
            assertTrue(restored.matches(context("","example:net"),settings));
        }
    }
    @Test void normalizationKeepsUnknownIdsAndDefaultsToEitherHand(){
        var c=fishing();c.heldItems.addAll(Arrays.asList(null," "," example:net "));c.heldItemHand=null;c.normalize();
        assertEquals(Set.of(ROD,"example:net"),c.heldItems);assertEquals(Hand.EITHER,c.heldItemHand);
        c.heldItems=null;c.normalize();assertTrue(c.heldItems.isEmpty());assertFalse(c.matches(context(ROD,""),settings));
    }
    @Test void inspectorDescribesTheHeldItemFailureAndNestedSelections(){
        var lib=Library.defaults();var p=lib.createPlaylist("Fishing");p.conditions=fishing();p.conditions.heldItemHand=Hand.MAIN_HAND;
        p.conditions.groups.add(fishing());
        var row=PlaybackInspector.inspect(lib,context(SHIELD,""),Set.of(),new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1)))
            .stream().filter(e->e.id().equals(p.id)).findFirst().orElseThrow();
        assertEquals("No selected item held in main hand",row.status());
        assertTrue(row.details().stream().anyMatch(s->s.contains("Main hand: "+SHIELD)));
        assertTrue(row.details().stream().anyMatch(s->s.startsWith("Group 1 held items")&&s.contains(ROD)));
        assertTrue(p.conditions.summary().contains("held items: 1 selected / Main hand"));
    }
    @Test void heldItemPlaybackUsesIndexedLibraryResolution(){
        var lib=Library.defaults();var tracks=new LibraryIndexTest.GuardedTracks();lib.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("t"+i,"file","Song",false));
        var p=lib.createPlaylist("Fishing");p.conditions=fishing();p.tracks.put("t69999",true);
        lib.playlist("day_outside").tracks.put("t0",true);lib.rebuildTrackIndex();tracks.locked=true;
        for(int i=0;i<100;i++)assertEquals(i%2==0?p.id:"day_outside",lib.selectPlayable(context(i%2==0?ROD:SHIELD,""),Set.of(),Set.of()).id);
    }
}

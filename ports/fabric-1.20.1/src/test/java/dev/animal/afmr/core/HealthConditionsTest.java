package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.*;
import dev.animal.afmr.library.LibraryStore;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HealthConditionsTest {
    @TempDir Path directory;
    private final Settings settings=new Settings();
    private Context context(double hearts,int time){
        return new Context("minecraft:overworld","minecraft:plains",false,false,time,false,15,"world",0,80,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,hearts,false,"","",GameMode.SURVIVAL,false);
    }
    private PlaylistConditions low(){var c=new PlaylistConditions();c.healthEnabled=true;return c;}

    @Test void aboveAndBelowUseStrictThresholdsAndFractionalHearts(){
        var c=low();assertTrue(c.matches(context(4.5,6000),settings));assertFalse(c.matches(context(5,6000),settings));assertFalse(c.matches(context(5.5,6000),settings));
        c.healthComparison=HealthComparison.ABOVE;assertFalse(c.matches(context(4.5,6000),settings));assertFalse(c.matches(context(5,6000),settings));assertTrue(c.matches(context(5.5,6000),settings));
        c.healthThreshold=0;assertFalse(c.matches(context(0,6000),settings));assertTrue(c.matches(context(0.5,6000),settings));
    }
    @Test void decimalEqualityUsesThePlayersFloatPrecision(){
        var c=low();c.healthThreshold=2.3;double actualHealthPoints=4.6f;
        assertFalse(c.matches(context(actualHealthPoints/2,6000),settings));c.healthComparison=HealthComparison.ABOVE;
        assertFalse(c.matches(context(actualHealthPoints/2,6000),settings));
    }
    @Test void disabledFilterIsIgnoredAndUnknownHealthDoesNotMatch(){
        var c=low();
        for(double hearts:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1}){
            assertEquals(Failure.HEALTH,c.failure(context(hearts,6000),settings));
            c.healthComparison=HealthComparison.ABOVE;assertFalse(c.matches(context(hearts,6000),settings));c.healthComparison=HealthComparison.BELOW;
        }
        c.healthEnabled=false;assertTrue(c.matches(context(Double.NaN,6000),settings));
    }
    @Test void healthWorksWithAllAnyNegationAndNestedGroups(){
        var c=low();c.timeEnabled=true;c.startTime=12000;c.endTime=23999;
        assertFalse(c.matches(context(4,6000),settings));c.matchAny=true;assertTrue(c.matches(context(4,6000),settings));assertFalse(c.matches(context(10,6000),settings));
        c.negated=true;assertFalse(c.matches(context(4,6000),settings));assertTrue(c.matches(context(10,6000),settings));
        var root=new PlaylistConditions();root.groups.add(c);assertTrue(root.matches(context(10,6000),settings));
        c.enabled=false;assertTrue(root.matches(context(4,6000),settings));
    }
    @Test void moddedHighHealthAndCombatOffAreIndependentOfTheHealthFilter(){
        var c=low();c.healthComparison=HealthComparison.ABOVE;c.healthThreshold=15;settings.combatEnabled=false;
        assertTrue(c.matches(context(20,6000),settings));assertFalse(c.matches(context(10,6000),settings));
    }
    @Test void healthSettingsSurviveOwnedSnapshotsNestedCopiesAndStorage()throws Exception{
        var lib=Library.defaults();var p=lib.createPlaylist("Low health");p.conditions.groups.add(low());
        var group=p.conditions.groups.getFirst();group.healthThreshold=3.5;group.combatPlayersOnly=true;group.combat=State.YES;
        var copy=lib.snapshot();group.healthThreshold=9;group.healthEnabled=false;
        try(var store=new LibraryStore(directory.resolve("health.json"))){
            store.save(copy);var saved=store.load().playlist(p.id).conditions.groups.getFirst();
            assertTrue(saved.healthEnabled);assertEquals(3.5,saved.healthThreshold);assertEquals(HealthComparison.BELOW,saved.healthComparison);assertTrue(saved.combatPlayersOnly);
            assertTrue(saved.summary().contains("health below 3.5 hearts"));assertTrue(saved.summary().contains("PvP"));
        }
    }
    @Test void normalizationRepairsInvalidHealthSettings(){
        var c=low();c.healthComparison=null;c.healthThreshold=Double.NaN;c.normalize();assertEquals(5,c.healthThreshold);assertEquals(HealthComparison.BELOW,c.healthComparison);
        c.healthThreshold=-1;c.normalize();assertEquals(5,c.healthThreshold);c.healthThreshold=0;c.normalize();assertEquals(0,c.healthThreshold);
    }
    @Test void healthChangesSelectThePriorityPlaylistAndEqualityReturnsToAmbient(){
        var lib=Library.defaults();var p=lib.createPlaylist("Low health");p.conditions=low();lib.movePlaylist(p.id,0);
        var low=new Track("low","file","Low",false);var normal=new Track("normal","file","Normal",false);lib.tracks.addAll(List.of(low,normal));
        p.tracks.put(low.id,true);lib.playlist("day_outside").tracks.put(normal.id,true);
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;
        var engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));
        engine.tick(0,lib,context(10,6000),Set.of());assertSame(normal,engine.current());
        engine.tick(1,lib,context(4,6000),Set.of());assertSame(low,engine.current());
        engine.tick(2,lib,context(5,6000),Set.of());assertSame(normal,engine.current());
        assertTrue(engine.overridePlaylist(p.id,OverrideMode.ONE_SONG,3,lib,Set.of()));engine.tick(3,lib,context(10,6000),Set.of());assertSame(low,engine.current());
    }
    @Test void inspectorExplainsHealthAndShowsCurrentRedHearts(){
        var lib=Library.defaults();var p=lib.createPlaylist("Low health");p.conditions=low();
        var rows=PlaybackInspector.inspect(lib,context(8,6000),Set.of(),new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1)));
        var row=rows.stream().filter(r->r.id().equals(p.id)).findFirst().orElseThrow();
        assertEquals("Health is not below 5.0 red hearts",row.status());assertTrue(row.details().stream().anyMatch(s->s.contains("8.00 red hearts")));
    }
}

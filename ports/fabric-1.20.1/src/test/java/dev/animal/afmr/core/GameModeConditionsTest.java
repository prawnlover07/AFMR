package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GameModeConditionsTest {
    @TempDir Path directory;
    private final Settings settings=new Settings();
    private Context context(GameMode mode){
        return new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"world",0,70,0,0,0.8,false,
            StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",mode,false,dev.animal.afmr.core.NearbyPlayers.EMPTY);
    }
    private PlaylistConditions creative(){
        var c=new PlaylistConditions();c.gameModeEnabled=true;c.gameModes=EnumSet.of(GameMode.CREATIVE);return c;
    }
    @Test void eachModeMatchesExactlyItsSelection(){
        var c=creative();
        for(var selected:GameMode.values())for(var current:GameMode.values()){
            c.gameModes=EnumSet.of(selected);
            assertEquals(selected==current,c.matches(context(current),settings),selected+" / "+current);
        }
    }
    @Test void multipleModesAreAlternativesAndAnEnabledEmptySelectionCannotMatch(){
        var c=creative();c.gameModes.add(GameMode.SPECTATOR);
        assertTrue(c.matches(context(GameMode.CREATIVE),settings));assertTrue(c.matches(context(GameMode.SPECTATOR),settings));
        assertFalse(c.matches(context(GameMode.ADVENTURE),settings));
        c.gameModes.clear();for(var mode:GameMode.values())assertFalse(c.matches(context(mode),settings));
        assertTrue(c.summary().contains("none selected"));
    }
    @Test void disabledFilterKeepsItsSelectionWithoutRestrictingPlay(){
        var c=creative();c.gameModeEnabled=false;
        for(var mode:GameMode.values())assertTrue(c.matches(context(mode),settings));
        assertEquals(Set.of(GameMode.CREATIVE),c.gameModes);
    }
    @Test void unavailableModeDoesNotSatisfyTheFilter(){
        var c=creative();assertEquals(Failure.GAME_MODE,c.failure(context(null),settings));
    }
    @Test void allAndAnyCombineGamemodeWithOtherFilters(){
        var c=creative();c.water=State.YES;
        assertFalse(c.matches(context(GameMode.CREATIVE),settings));
        c.matchAny=true;assertTrue(c.matches(context(GameMode.CREATIVE),settings));
        assertFalse(c.matches(context(GameMode.SURVIVAL),settings));
        c.water=State.NO;assertTrue(c.matches(context(GameMode.SURVIVAL),settings));
    }
    @Test void nestedNotAndDisabledGroupsUseTheSamePredicate(){
        var root=new PlaylistConditions();var child=creative();child.negated=true;root.groups.add(child);
        assertFalse(root.matches(context(GameMode.CREATIVE),settings));assertTrue(root.matches(context(GameMode.SURVIVAL),settings));
        child.enabled=false;assertTrue(root.matches(context(GameMode.CREATIVE),settings));
        child.enabled=true;root.matchAny=true;root.water=State.NO;assertTrue(root.matches(context(GameMode.CREATIVE),settings));
    }
    @Test void cloneSnapshotsAndDiskOwnTheirModeSelections() throws Exception {
        var lib=Library.defaults();var p=lib.createPlaylist("Building");p.conditions.groups.add(creative());
        var clone=lib.clonePlaylist(p.id,"Building copy");clone.conditions.groups.getFirst().gameModes.add(GameMode.SPECTATOR);
        assertEquals(Set.of(GameMode.CREATIVE),p.conditions.groups.getFirst().gameModes);
        var snapshot=lib.snapshot();p.conditions.groups.getFirst().gameModes.clear();
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(snapshot);var loaded=store.load();var restored=loaded.playlist(p.id).conditions.groups.getFirst();
            assertTrue(restored.gameModeEnabled);assertEquals(Set.of(GameMode.CREATIVE),restored.gameModes);
            assertTrue(restored.matches(context(GameMode.CREATIVE),settings));
            assertEquals(Set.of(GameMode.CREATIVE,GameMode.SPECTATOR),loaded.playlist(clone.id).conditions.groups.getFirst().gameModes);
        }
    }
    @Test void normalizationRemovesNullModesWithoutAddingSelections(){
        var c=creative();c.gameModes=new LinkedHashSet<>(Arrays.asList(null,GameMode.ADVENTURE));c.normalize();
        assertEquals(Set.of(GameMode.ADVENTURE),c.gameModes);
        c.gameModes=null;c.normalize();assertTrue(c.gameModes.isEmpty());assertFalse(c.matches(context(GameMode.SURVIVAL),settings));
    }
    @Test void modeChangesUseNormalPriorityAndSkipEmptyPlaylists(){
        var lib=Library.defaults();var p=lib.createPlaylist("Building");p.conditions=creative();p.includeVanilla=false;
        lib.tracks.add(new Track("song","file","Song",false));lib.playlist("day_outside").tracks.put("song",true);
        assertEquals("day_outside",lib.selectPlayable(context(GameMode.CREATIVE),Set.of(),Set.of()).id);
        p.tracks.put("song",true);assertSame(p,lib.selectPlayable(context(GameMode.CREATIVE),Set.of(),Set.of()));
        assertEquals("day_outside",lib.selectPlayable(context(GameMode.SURVIVAL),Set.of(),Set.of()).id);
        p.tracks.put("song",false);assertEquals("day_outside",lib.selectPlayable(context(GameMode.CREATIVE),Set.of(),Set.of()).id);
    }
    @Test void manualOverridesStillBypassGamemodeAndAutomaticOff(){
        var lib=Library.defaults();var p=lib.createPlaylist("Building");p.conditions=creative();p.enabled=false;
        lib.tracks.add(new Track("song","file","Song",false));p.tracks.put("song",true);
        var engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));
        assertTrue(engine.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED,0,lib,Set.of()));
        engine.tick(0,lib,context(GameMode.SURVIVAL),Set.of());assertSame(p,engine.selected());
    }
    @Test void modeSwitchesKeepTheConfiguredBufferFadeAndSilence(){
        var lib=Library.defaults();lib.playlists.clear();
        var fallback=lib.createPlaylist("Fallback");var p=lib.createPlaylist("Building");p.conditions=creative();
        lib.movePlaylist(p.id,0);
        fallback.includeVanilla=p.includeVanilla=false;
        lib.tracks.add(new Track("a","file-a","A",false));lib.tracks.add(new Track("b","file-b","B",false));
        p.tracks.put("a",true);fallback.tracks.put("b",true);
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=3;lib.settings.bufferSeconds=1;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=2;
        var engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));
        engine.tick(0,lib,context(GameMode.CREATIVE),Set.of());engine.tick(3,lib,context(GameMode.CREATIVE),Set.of());assertEquals(p.id,engine.sourcePlaylistId());
        engine.tick(4,lib,context(GameMode.SURVIVAL),Set.of());assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
        engine.tick(5,lib,context(GameMode.SURVIVAL),Set.of());assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
        engine.tick(7,lib,context(GameMode.SURVIVAL),Set.of());assertEquals(PlaybackEngine.Phase.SILENCE,engine.phase());
        engine.tick(9.99,lib,context(GameMode.SURVIVAL),Set.of());assertNull(engine.current());
        engine.tick(10,lib,context(GameMode.SURVIVAL),Set.of());assertEquals(fallback.id,engine.sourcePlaylistId());
    }
    @Test void inspectorExplainsModeFailuresAndNestedSelections(){
        var lib=Library.defaults();var p=lib.createPlaylist("Building");p.conditions=creative();p.conditions.groups.add(creative());
        var row=PlaybackInspector.inspect(lib,context(GameMode.SURVIVAL),Set.of(),new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1)))
            .stream().filter(e->e.id().equals(p.id)).findFirst().orElseThrow();
        assertEquals("Gamemode not selected: Survival",row.status());
        assertTrue(row.details().contains("Gamemode: Survival"));
        assertTrue(row.details().stream().anyMatch(s->s.startsWith("Group 1")&&s.contains("gamemode: Creative")));
        assertTrue(p.conditions.summary().contains("gamemode: Creative"));
    }
}

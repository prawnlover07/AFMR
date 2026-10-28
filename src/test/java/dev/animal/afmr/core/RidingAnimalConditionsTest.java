package dev.animal.afmr.core;

import com.google.gson.Gson;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.*;
import dev.animal.afmr.library.ConfigBackup;
import dev.animal.afmr.library.LibraryStore;
import dev.animal.afmr.library.PlaylistPresets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RidingAnimalConditionsTest {
    @TempDir Path directory;
    private final Settings settings=new Settings();
    private Context context(boolean riding){
        return new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"world",0,70,0,0,0.8,false,
            StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,NearbyPlayers.EMPTY,riding);
    }
    private PlaylistConditions riding(){
        var c=new PlaylistConditions();c.ridingAnimal=State.YES;return c;
    }
    @Test void conditionStartsOffAndAnyAllowsBothStates(){
        var c=new PlaylistConditions();assertEquals(State.ANY,c.ridingAnimal);
        assertTrue(c.matches(context(false),settings));assertTrue(c.matches(context(true),settings));
        assertFalse(c.summary().contains("riding"));
    }
    @Test void ridingRequiresTheMountedAnimalObservation(){
        var c=riding();assertTrue(c.matches(context(true),settings));
        assertEquals(Failure.RIDING_ANIMAL,c.failure(context(false),settings));
    }
    @Test void notRidingInvertsTheObservation(){
        var c=riding();c.ridingAnimal=State.NO;assertTrue(c.matches(context(false),settings));
        assertEquals(Failure.RIDING_ANIMAL,c.failure(context(true),settings));
    }
    @Test void allAndAnyKeepRidingIndependentOfOtherConditions(){
        var c=riding();c.water=State.YES;assertFalse(c.matches(context(true),settings));
        c.matchAny=true;assertTrue(c.matches(context(true),settings));assertFalse(c.matches(context(false),settings));
        c.water=State.NO;assertTrue(c.matches(context(false),settings));
    }
    @Test void nestedNotAndDisabledGroupsUseRidingNormally(){
        var root=new PlaylistConditions();var child=riding();child.negated=true;root.groups.add(child);
        assertTrue(root.matches(context(false),settings));assertFalse(root.matches(context(true),settings));
        child.enabled=false;assertTrue(root.matches(context(true),settings));
        child.enabled=true;root.matchAny=true;root.water=State.NO;assertTrue(root.matches(context(true),settings));
    }
    @Test void disablingTheWholeConditionRemovesItFromItsParent(){
        var root=new PlaylistConditions();var child=riding();child.enabled=false;root.groups.add(child);
        assertTrue(root.matches(context(false),settings));child.enabled=true;assertFalse(root.matches(context(false),settings));
    }
    @Test void normalizationRepairsNullWithoutChangingValidStates(){
        var c=riding();c.ridingAnimal=null;c.normalize();assertEquals(State.ANY,c.ridingAnimal);
        for(var state:State.values()){c.ridingAnimal=state;c.normalize();assertEquals(state,c.ridingAnimal);}
    }
    @Test void copiesAndSaveSnapshotsOwnNestedRidingSettings()throws Exception{
        var lib=Library.defaults();var p=lib.createPlaylist("Horseback");p.conditions.groups.add(riding());
        var clone=lib.clonePlaylist(p.id,"Horseback copy");clone.conditions.groups.getFirst().ridingAnimal=State.NO;
        assertEquals(State.YES,p.conditions.groups.getFirst().ridingAnimal);
        var snapshot=lib.snapshot();p.conditions.groups.getFirst().ridingAnimal=State.ANY;
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(snapshot);var loaded=store.load();
            assertEquals(State.YES,loaded.playlist(p.id).conditions.groups.getFirst().ridingAnimal);
            assertEquals(State.NO,loaded.playlist(clone.id).conditions.groups.getFirst().ridingAnimal);
        }
    }
    @Test void copiedProfilesKeepTheirOwnRidingRules(){
        var lib=Library.defaults();var p=lib.createPlaylist("Horseback");p.conditions=riding();
        String copy=lib.profiles.create(lib,"Riding copy",lib.profiles.activeId);p.conditions.ridingAnimal=State.NO;
        lib.profiles.activate(lib,copy);assertEquals(State.YES,lib.playlist(p.id).conditions.ridingAnimal);
        lib.profiles.activate(lib,"default");assertEquals(State.NO,lib.playlist(p.id).conditions.ridingAnimal);
    }
    @Test void playlistSharingRetainsEveryRidingStateAndNestedRules()throws Exception{
        for(var state:State.values()){
            var lib=Library.defaults();var p=lib.createPlaylist("Horseback");p.conditions.ridingAnimal=state;
            p.conditions.groups.add(riding());p.conditions.groups.getFirst().negated=true;
            var data=PlaylistPresets.capture(lib,Set.of(p.id),false);var path=directory.resolve("riding.afmrp");
            PlaylistPresets.write(path,data);data=PlaylistPresets.read(path);var target=Library.defaults();
            String id=PlaylistPresets.apply(target,"default","","",PlaylistPresets.Placement.TOP,data,"",null).ids().getFirst();
            var restored=target.playlist(id).conditions;
            assertEquals(state,restored.ridingAnimal);assertEquals(State.YES,restored.groups.getFirst().ridingAnimal);
            assertTrue(restored.groups.getFirst().negated);
        }
    }
    @Test void playlistSharingRejectsAnUnknownRidingState()throws Exception{
        var lib=Library.defaults();var p=lib.createPlaylist("Horseback");p.conditions=riding();
        var tree=new Gson().toJsonTree(PlaylistPresets.capture(lib,Set.of(p.id),false)).getAsJsonObject();
        tree.getAsJsonArray("playlists").get(0).getAsJsonObject().getAsJsonObject("conditions").addProperty("ridingAnimal","FLYING");
        Path path=directory.resolve("invalid.afmrp");Files.writeString(path,tree.toString());
        assertThrows(java.io.IOException.class,()->PlaylistPresets.read(path));
    }
    @Test void fullBackupsPreserveActiveAndInactiveProfileRidingRules()throws Exception{
        var lib=Library.defaults();var p=lib.createPlaylist("Horseback");p.conditions=riding();
        String copy=lib.profiles.create(lib,"Another setup",lib.profiles.activeId);p.conditions.ridingAnimal=State.NO;
        var keys=new LinkedHashMap<String,ConfigBackup.Key>();
        for(String name:List.of("open","skip","next","pause","inspect","overrides","automatic"))
            keys.put("key.afmr."+name,new ConfigBackup.Key("key.keyboard.unknown","NONE"));
        for(int i=1;i<=8;i++)keys.put("key.afmr.override_"+i,new ConfigBackup.Key("key.keyboard.unknown","NONE"));
        var data=ConfigBackup.capture(lib.snapshot(),List.of(),keys,directory);var path=directory.resolve("riding.afmrb");
        ConfigBackup.write(path,data);var restored=ConfigBackup.read(path).library();
        assertEquals(State.NO,restored.playlist(p.id).conditions.ridingAnimal);
        restored.profiles.activate(restored,copy);assertEquals(State.YES,restored.playlist(p.id).conditions.ridingAnimal);
    }
    private static final class Scenario {
        final Library lib=Library.defaults();
        final PlaybackEngine engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));
        final Playlist fallback,rider;
        Scenario(){
            lib.playlists.clear();fallback=lib.createPlaylist("On foot");rider=lib.createPlaylist("Horseback");lib.movePlaylist(rider.id,0);
            rider.conditions.ridingAnimal=State.YES;fallback.includeVanilla=rider.includeVanilla=false;
            lib.tracks.add(new Track("ambient","file-a","Ambient",false));lib.tracks.add(new Track("riding","file-b","Riding",false));lib.rebuildTrackIndex();
            fallback.tracks.put("ambient",true);rider.tracks.put("riding",true);
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;
            lib.settings.bufferSeconds=lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0;
        }
    }
    @Test void mountingUsesPriorityWhileEmptyAndDisabledSongsFallThrough(){
        var s=new Scenario();assertSame(s.rider,s.lib.selectPlayable(context(true),Set.of(),Set.of()));
        assertSame(s.fallback,s.lib.selectPlayable(context(false),Set.of(),Set.of()));
        s.rider.tracks.clear();assertSame(s.fallback,s.lib.selectPlayable(context(true),Set.of(),Set.of()));
        s.rider.tracks.put("riding",false);assertSame(s.fallback,s.lib.selectPlayable(context(true),Set.of(),Set.of()));
    }
    @Test void manualOverridesBypassRidingAndAutomaticOff(){
        var s=new Scenario();s.rider.enabled=false;
        assertTrue(s.engine.overridePlaylist(s.rider.id,OverrideMode.UNTIL_RELEASED,0,s.lib,Set.of()));
        s.engine.tick(0,s.lib,context(false),Set.of());assertSame(s.rider,s.engine.selected());
    }
    @Test void dismountUsesTheNormalBufferFadeAndSilence(){
        var s=new Scenario();s.lib.settings.silenceMinSeconds=s.lib.settings.silenceMaxSeconds=3;
        s.lib.settings.bufferSeconds=1;s.lib.settings.fadeInSeconds=s.lib.settings.fadeOutSeconds=2;
        s.engine.tick(0,s.lib,context(true),Set.of());s.engine.tick(3,s.lib,context(true),Set.of());assertEquals(s.rider.id,s.engine.sourcePlaylistId());
        s.engine.tick(4,s.lib,context(false),Set.of());assertEquals(PlaybackEngine.Phase.PLAYING,s.engine.phase());
        s.engine.tick(5,s.lib,context(false),Set.of());assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
        s.engine.tick(7,s.lib,context(false),Set.of());assertEquals(PlaybackEngine.Phase.SILENCE,s.engine.phase());
        s.engine.tick(9.99,s.lib,context(false),Set.of());assertNull(s.engine.current());
        s.engine.tick(10,s.lib,context(false),Set.of());assertEquals(s.fallback.id,s.engine.sourcePlaylistId());
    }
    @Test void independentEntryAndExitFinishProtectionStillApplies(){
        var entry=new Scenario();entry.rider.finishCurrentSong=true;
        entry.engine.tick(0,entry.lib,context(false),Set.of());entry.engine.tick(1,entry.lib,context(true),Set.of());
        assertEquals(entry.fallback.id,entry.engine.sourcePlaylistId());assertEquals(PlaybackEngine.Phase.PLAYING,entry.engine.phase());
        var exit=new Scenario();exit.rider.finishCurrentSongOnExit=true;
        exit.engine.tick(0,exit.lib,context(true),Set.of());exit.engine.tick(1,exit.lib,context(false),Set.of());
        assertEquals(exit.rider.id,exit.engine.sourcePlaylistId());assertEquals(PlaybackEngine.Phase.PLAYING,exit.engine.phase());
    }
    @Test void inspectorAndSummariesExplainBothRidingStates(){
        var s=new Scenario();assertTrue(s.rider.conditions.summary().contains("riding animal"));
        var row=PlaybackInspector.inspect(s.lib,context(false),Set.of(),s.engine).stream().filter(e->e.id().equals(s.rider.id)).findFirst().orElseThrow();
        assertEquals("Not riding an animal",row.status());assertTrue(row.details().contains("Riding animal: false"));
        s.rider.conditions.ridingAnimal=State.NO;
        row=PlaybackInspector.inspect(s.lib,context(true),Set.of(),s.engine).stream().filter(e->e.id().equals(s.rider.id)).findFirst().orElseThrow();
        assertEquals("Currently riding an animal",row.status());assertTrue(s.rider.conditions.summary().contains("not riding animal"));
    }
}

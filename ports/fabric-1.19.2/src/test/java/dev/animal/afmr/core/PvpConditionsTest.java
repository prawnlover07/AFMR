package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.*;
import dev.animal.afmr.core.CombatOpponent;
import dev.animal.afmr.library.LibraryStore;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PvpConditionsTest {
    @TempDir Path directory;
    private final Settings settings=new Settings();
    private Context context(boolean combat,boolean pvp,boolean dead){
        return new Context("minecraft:overworld","minecraft:plains",combat,false,6000,false,15,"world",0,80,0,0,0.8,dead,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,dead?0:10,pvp,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    }
    private PlaylistConditions players(){var c=new PlaylistConditions();c.combat=State.YES;c.combatPlayersOnly=true;return c;}

    @Test void playersOnlyRejectsMobCombatButAcceptsPvp(){
        var c=players();assertFalse(c.matches(context(true,false,false),settings));assertTrue(c.matches(context(true,true,false),settings));
        c.combatPlayersOnly=false;assertTrue(c.matches(context(true,false,false),settings));
    }
    @Test void peacefulPlayersOnlyMeansNoPvpAndAnyDisablesTheFilter(){
        var c=players();c.combat=State.NO;assertTrue(c.matches(context(true,false,false),settings));assertFalse(c.matches(context(true,true,false),settings));
        c.combat=State.ANY;assertTrue(c.matches(context(true,true,false),settings));assertFalse(c.combatDriven(context(true,true,false),settings));
    }
    @Test void globalCombatSwitchAppliesToPvp(){
        var c=players();settings.combatEnabled=false;assertFalse(c.matches(context(true,true,false),settings));
        c.combat=State.NO;assertTrue(c.matches(context(true,true,false),settings));
    }
    @Test void mobEventsCannotKeepAPlayersOnlyPlaylistEligible(){
        var tracker=new CombatTracker();settings.combatQuietSeconds=5;var c=players();
        tracker.attack(1,CombatOpponent.PLAYER,settings);tracker.damage(5,CombatOpponent.MOB,settings);
        assertTrue(c.matches(context(tracker.active(5,settings),tracker.playersActive(5,settings),false),settings));
        assertFalse(c.matches(context(tracker.active(6,settings),tracker.playersActive(6,settings),false),settings));assertTrue(tracker.active(6,settings));
    }
    @Test void anyModeDeathPolicyRequiresAnActualPvpMatchRatherThanOrdinaryCombat(){
        var c=players();c.matchAny=true;c.weatherEnabled=true;c.weather=Weather.CLEAR;
        assertTrue(c.matches(context(true,false,false),settings));assertFalse(c.combatDriven(context(true,false,false),settings));
        assertTrue(c.combatDriven(context(true,true,false),settings));
        var root=new PlaylistConditions();root.matchAny=true;root.groups.add(c);assertFalse(root.combatDriven(context(true,false,false),settings));
        c.negated=true;assertFalse(root.combatDriven(context(true,true,false),settings));
    }
    @Test void pvpSongReceivesCombatDeathPolicyAndFallbackPriority(){
        var lib=Library.defaults();var p=lib.createPlaylist("PvP");p.conditions=players();lib.movePlaylist(p.id,0);
        var song=new Track("pvp","file","PvP",false);var mob=new Track("mob","file","Mob",false);lib.tracks.addAll(List.of(song,mob));p.tracks.put(song.id,true);lib.playlist("combat").tracks.put(mob.id,true);
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=lib.settings.bufferSeconds=lib.settings.fadeOutSeconds=0;lib.settings.combatDeathMusic=DeathMusic.CUT;
        var engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));engine.tick(0,lib,context(true,false,false),Set.of());assertSame(mob,engine.current());
        engine.tick(1,lib,context(true,true,false),Set.of());assertSame(song,engine.current());
        engine.tick(2,lib,context(false,false,true),Set.of());assertNull(engine.current());
    }
    @Test void combatScopeAndPlayersOnlySaveIndependently(){
        var lib=Library.defaults();assertTrue(lib.settings.combatThreatsOnly);lib.settings.combatThreatsOnly=false;
        var p=lib.createPlaylist("PvP");p.conditions=players();var copy=lib.snapshot();lib.settings.combatThreatsOnly=true;p.conditions.combatPlayersOnly=false;
        try(var store=new LibraryStore(directory.resolve("pvp.json"))){
            store.save(copy);var restored=store.load();assertFalse(restored.settings.combatThreatsOnly);assertTrue(restored.playlist(p.id).conditions.combatPlayersOnly);
            restored.settings.restoreMinecraftPlaybackDefaults();assertFalse(restored.settings.combatThreatsOnly);
        }catch(Exception e){throw new AssertionError(e);}
    }
    @Test void inspectorDistinguishesPvpFromGeneralCombat(){
        var lib=Library.defaults();var p=lib.createPlaylist("PvP");p.conditions=players();
        var row=PlaybackInspector.inspect(lib,context(true,false,false),Set.of(),new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1))).stream().filter(r->r.id().equals(p.id)).findFirst().orElseThrow();
        assertEquals("Not currently in PvP",row.status());assertTrue(row.details().stream().anyMatch(s->s.startsWith("PvP: false")));
    }
}

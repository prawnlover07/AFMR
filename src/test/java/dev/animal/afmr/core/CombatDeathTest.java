package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.State;
import dev.animal.afmr.core.PlaylistConditions.Weather;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CombatDeathTest {
    private Library library;
    private PlaybackEngineTest.FakeAudio audio;
    private PlaybackEngine engine;
    private Track combat,ambient;
    private Context context(boolean busy,boolean dead){return new Context("minecraft:overworld","minecraft:plains",busy,false,6000,false,15,"world",0,64,0,0,0.8,dead,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);}
    private void tick(double time,boolean busy,boolean dead){engine.tick(time,library,context(busy,dead),Set.of());}
    @BeforeEach void setup(){
        library=Library.defaults();audio=new PlaybackEngineTest.FakeAudio();engine=new PlaybackEngine(audio,new Random(1));
        combat=new Track("combat-song","file","Combat",false);ambient=new Track("ambient-song","file","Ambient",false);library.tracks.addAll(List.of(combat,ambient));
        library.playlist("combat").tracks.put(combat.id,true);library.playlist("day_outside").tracks.put(ambient.id,true);
        library.settings.silenceMinSeconds=library.settings.silenceMaxSeconds=10;library.settings.bufferSeconds=3;library.settings.fadeInSeconds=library.settings.fadeOutSeconds=3;
        tick(0,true,false);tick(10,true,false);tick(13,true,false);assertSame(combat,engine.current());assertEquals(1,audio.gain);
    }
    @Test void deathStartsShortFadeImmediatelyDespiteBufferAndFinishToggle(){
        library.playlist("day_outside").finishCurrentSong=true;library.playlist("combat").finishCurrentSongOnExit=true;library.settings.bufferSeconds=60;
        tick(14,false,true);assertEquals(PlaybackEngine.Phase.FADING,engine.phase());tick(14.5,false,true);assertEquals(0.5,audio.gain,0.001);
        tick(15,false,true);assertNull(engine.current());assertEquals(10,engine.remainingSilence(15));
    }
    @Test void instantCutAndZeroFadeStopOnTheDeathTick(){
        library.playlist("day_outside").finishCurrentSong=true;library.playlist("combat").finishCurrentSongOnExit=true;
        library.settings.combatDeathMusic=DeathMusic.CUT;tick(14,false,true);assertNull(audio.current);assertEquals(PlaybackEngine.Phase.SILENCE,engine.phase());
    }
    @Test void zeroSecondFadeCutsWithoutStartingAmbientOnDeath(){
        library.settings.combatDeathFadeSeconds=0;library.settings.silenceMinSeconds=library.settings.silenceMaxSeconds=0;
        tick(14,false,true);assertNull(audio.current);tick(100,false,true);assertNull(audio.current);
    }
    @Test void configuredSilenceRemainsAfterDeathAndAutomaticMusicWaitsForRespawn(){
        tick(14,false,true);tick(15,false,true);tick(100,false,true);assertNull(engine.current());
        tick(101,false,false);assertSame(ambient,engine.current());
    }
    @Test void allowMusicWhileDeadStillKeepsTheNormalFullGap(){
        library.settings.preventMusicWhileDead=false;tick(14,false,true);tick(15,false,true);tick(24.9,false,true);assertNull(engine.current());
        tick(25,false,true);assertSame(ambient,engine.current());
    }
    @Test void finishOptionLetsCombatEndAndPreventsNewMusicWhileDead(){
        library.settings.combatDeathMusic=DeathMusic.KEEP_PLAYING;tick(14,false,true);tick(20,false,true);assertSame(combat,engine.current());
        audio.current=null;tick(21,false,true);tick(100,false,true);assertNull(engine.current());
    }
    @Test void customCombatPlaylistUsesTheSameDeathPolicy(){
        var p=library.createPlaylist("Custom fight");p.conditions.combat=State.YES;p.tracks.put(combat.id,true);library.movePlaylist(p.id,0);
        engine.reset();tick(0,true,false);tick(10,true,false);tick(13,true,false);assertSame(p,engine.selected());
        library.settings.combatDeathMusic=DeathMusic.CUT;tick(14,false,true);assertNull(engine.current());
    }
    @Test void manualPreviewsAreNotTreatedAsAutomaticCombatMusic(){
        engine.preview(combat,13,false);tick(14,false,true);tick(20,false,true);assertSame(combat,engine.current());assertTrue(engine.previewing());
    }
    @Test void anyModeSongStartedByWeatherWhilePeacefulDoesNotReceiveCombatDeathCut(){
        var p=library.createPlaylist("Fight or clear");p.conditions.matchAny=true;p.conditions.combat=State.YES;
        p.conditions.weatherEnabled=true;p.conditions.weather=Weather.CLEAR;
        p.tracks.put(combat.id,true);library.movePlaylist(p.id,0);
        engine.reset();tick(0,false,false);tick(10,false,false);tick(13,false,false);assertSame(p,engine.selected());
        library.settings.combatDeathMusic=DeathMusic.CUT;tick(14,false,true);
        assertSame(combat,engine.current());assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
    }
    @Test void anyModeSongStartedDuringCombatStillUsesCombatDeathPolicy(){
        var p=library.createPlaylist("Fight or rain");p.conditions.matchAny=true;p.conditions.combat=State.YES;
        p.conditions.weatherEnabled=true;p.conditions.weather=Weather.RAIN;
        p.tracks.put(combat.id,true);library.movePlaylist(p.id,0);
        engine.reset();tick(0,true,false);tick(10,true,false);tick(13,true,false);assertSame(p,engine.selected());
        library.settings.combatDeathMusic=DeathMusic.CUT;tick(14,false,true);assertNull(engine.current());
    }
    @Test void ambientSongDoesNotReceiveTheCombatDeathFade(){
        engine.reset();tick(0,false,false);tick(10,false,false);tick(13,false,false);assertSame(ambient,engine.current());
        tick(14,false,true);tick(20,false,true);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());assertSame(ambient,engine.current());
    }
    @Test void deathDuringAnotherFadeStartsFromItsCurrentVolume(){
        tick(14,false,false);tick(17,false,false);tick(18,false,false);assertEquals(2.0/3,audio.gain,0.001);
        tick(18.5,false,true);assertEquals(2.0/3,audio.gain,0.001);tick(19,false,true);assertEquals(1.0/3,audio.gain,0.001);
        tick(19.5,false,true);assertNull(engine.current());
    }
}

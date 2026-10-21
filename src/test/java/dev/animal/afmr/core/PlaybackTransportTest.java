package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackTransportTest {
    private Library lib;
    private PlaybackEngineTest.FakeAudio audio;
    private PlaybackEngine engine;
    private Playlist manual;
    private Context context(boolean day,boolean dead){return new Context("minecraft:overworld","minecraft:plains",false,false,day?6000:18000,false,false,false,15,"world",0,80,0,0,0.8,dead);}
    private void tick(double time,boolean day){engine.tick(time,lib,context(day,false),Set.of());}
    private void next(double time,boolean day){assertTrue(engine.nextSong(time,lib,context(day,false),Set.of()));tick(time,day);}
    private void combat(){assertTrue(engine.overridePlaylist("combat",OverrideMode.ONE_SONG,15,lib,Set.of()));tick(19,true);tick(23,true);assertEquals("combat",engine.sourcePlaylistId());}
    @BeforeEach void setup(){
        lib=Library.defaults();audio=new PlaybackEngineTest.FakeAudio();engine=new PlaybackEngine(audio,new Random(8));manual=lib.createPlaylist("Manual");manual.manualOnly=true;
        for(var p:List.of(lib.playlist("day_outside"),lib.playlist("night_outside"),lib.playlist("combat"),manual))for(int i=0;i<3;i++){
            var t=new Track(p.id+"-"+i,"file",p.name+" "+i,false);lib.tracks.add(t);p.tracks.put(t.id,true);}
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.fadeSeconds=4;lib.settings.bufferSeconds=2;
        tick(0,true);tick(10,true);tick(14,true);assertEquals(1,audio.gain);
    }
    @Test void pauseHoldsCurrentSongAndResumesWithoutRestartingIt(){
        var song=engine.current();engine.setPaused(true,15);tick(100,true);assertTrue(engine.paused());assertTrue(audio.paused);assertSame(song,engine.current());assertEquals(1,audio.gain);
        engine.setPaused(false,100);tick(101,true);assertFalse(audio.paused);assertSame(song,engine.current());assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
    }
    @Test void silentGapKeepsItsRemainingTimeDuringPause(){
        engine.reset();tick(0,true);tick(3,true);engine.setPaused(true,3);tick(100,true);assertEquals(7,engine.remainingSilence(100));assertNull(engine.current());
        engine.setPaused(false,100);tick(106.9,true);assertNull(engine.current());tick(107,true);assertNotNull(engine.current());
    }
    @Test void fadeOutResumesAtItsPreviousGainAndStillWaitsFullGap(){
        tick(15,false);tick(17,false);tick(18,false);assertEquals(0.75,audio.gain,0.001);engine.setPaused(true,18);tick(100,false);assertEquals(0.75,audio.gain,0.001);
        engine.setPaused(false,100);tick(101,false);assertEquals(0.5,audio.gain,0.001);tick(103,false);assertNull(engine.current());assertEquals(10,engine.remainingSilence(103));
        tick(113,false);assertEquals("night_outside",engine.sourcePlaylistId());
    }
    @Test void fadeInDoesNotJumpAheadDuringPause(){
        engine.reset();tick(0,true);tick(10,true);tick(11,true);assertEquals(0.25,audio.gain,0.001);
        engine.setPaused(true,11);tick(100,true);assertEquals(0.25,audio.gain,0.001);engine.setPaused(false,100);tick(101,true);assertEquals(0.5,audio.gain,0.001);
    }
    @Test void contextBufferIsFrozenAndEnvironmentChangesRemainVisible(){
        tick(15,false);tick(16,false);engine.setPaused(true,16);tick(100,false);assertEquals("night_outside",engine.automaticSelected().id);assertEquals(1,engine.remainingBuffer(100,lib.settings));
        engine.setPaused(false,100);tick(100.9,false);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());tick(101,false);assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
    }
    @Test void deathCutStillAppliesWhilePausedAndTheGapStaysFrozen(){
        combat();lib.settings.combatDeathMusic=DeathMusic.CUT;engine.setPaused(true,24);engine.tick(30,lib,context(true,true),Set.of());
        assertNull(engine.current());assertNull(engine.overrideId());assertTrue(engine.paused());assertEquals(10,engine.remainingSilence(100));
        engine.setPaused(false,100);tick(109.9,true);assertNull(engine.current());tick(110,true);assertEquals("day_outside",engine.sourcePlaylistId());
    }
    @Test void aDeathFadeRequestedWhilePausedBeginsWhenResumed(){
        combat();lib.settings.combatDeathFadeSeconds=1;engine.setPaused(true,24);engine.tick(30,lib,context(true,true),Set.of());
        assertEquals(PlaybackEngine.Phase.FADING,engine.phase());assertEquals(1,audio.gain);engine.setPaused(false,100);engine.tick(100,lib,context(true,true),Set.of());assertEquals(1,audio.gain);
        engine.tick(101,lib,context(true,true),Set.of());assertNull(engine.current());assertNull(engine.overrideId());
    }
    @Test void overrideRequestedWhilePausedWaitsForResume(){
        var before=engine.current();engine.setPaused(true,15);assertTrue(engine.overridePlaylist(manual.id,OverrideMode.ONE_SONG,100,lib,Set.of()));tick(200,true);
        assertSame(before,engine.current());assertTrue(engine.paused());engine.setPaused(false,200);tick(204,true);assertEquals(manual.id,engine.sourcePlaylistId());
    }
    @Test void aPreviewSelectedWhilePausedRemainsPaused(){
        engine.setPaused(true,15);var preview=lib.track(manual.tracks.keySet().iterator().next());engine.preview(preview,100);tick(200,true);
        assertSame(preview,engine.current());assertTrue(engine.previewing());assertTrue(audio.paused);engine.setPaused(false,200);tick(201,true);assertSame(preview,engine.current());assertEquals(0.25,audio.gain,0.001);
    }
    @Test void ordinarySkipWhilePausedKeepsTheNormalFrozenGap(){
        engine.setPaused(true,15);engine.skip(100,lib.settings);tick(200,true);assertNull(engine.current());assertEquals(10,engine.remainingSilence(200));
        engine.setPaused(false,200);tick(209.9,true);assertNull(engine.current());tick(210,true);assertNotNull(engine.current());
    }
    @Test void decoderFailureWhilePausedPreservesPauseAndRetryTiming(){
        String bad=engine.current().id;engine.setPaused(true,15);engine.failed(bad,"bad decoder",100,lib.settings);tick(200,true);assertTrue(engine.paused());assertNull(engine.current());assertEquals(1,engine.remainingSilence(200));
        engine.setPaused(false,200);tick(201,true);assertNotEquals(bad,engine.current().id);
    }
    @Test void repeatedPauseDoesNotResetItsTimeAndResetClearsIt(){
        engine.reset();tick(0,true);engine.setPaused(true,3);engine.setPaused(true,100);assertEquals(7,engine.remainingSilence(100));engine.setPaused(false,200);assertEquals(7,engine.remainingSilence(200));
        engine.setPaused(true,201);engine.reset();assertFalse(engine.paused());assertFalse(audio.paused);assertNull(engine.current());
    }
    @Test void nextBypassesSilenceBufferFadeOutAndFinishProtection(){
        var previous=engine.current();lib.playlist("day_outside").finishCurrentSong=true;lib.playlist("day_outside").finishCurrentSongOnExit=true;
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=600;lib.settings.bufferSeconds=60;next(15,true);
        assertNotSame(previous,engine.current());assertEquals("day_outside",engine.sourcePlaylistId());assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
    }
    @Test void nextCanBypassAnInitialSilentGap(){
        engine.reset();tick(0,true);next(1,true);assertNotNull(engine.current());assertEquals("day_outside",engine.sourcePlaylistId());
    }
    @Test void nextUsesTheCurrentAutomaticChoiceInsteadOfAProtectedOldSource(){
        lib.playlist("day_outside").finishCurrentSongOnExit=true;tick(15,false);tick(17,false);assertEquals("day_outside",engine.sourcePlaylistId());
        next(18,false);assertEquals("night_outside",engine.sourcePlaylistId());
    }
    @TestFactory List<DynamicTest> nextPreservesEitherOverrideModeIncludingAutomaticOff(){
        return List.of(OverrideMode.ONE_SONG,OverrideMode.UNTIL_RELEASED).stream().map(mode->DynamicTest.dynamicTest(mode.name(),()->{
            engine.reset();manual.enabled=false;assertTrue(engine.overridePlaylist(manual.id,mode,15,lib,Set.of()));tick(15,true);String first=engine.current().id;
            next(16,true);assertEquals(manual.id,engine.sourcePlaylistId());assertEquals(mode,engine.overrideMode());assertNotEquals(first,engine.current().id);assertFalse(manual.enabled);
            if(mode==OverrideMode.ONE_SONG){audio.current=null;tick(17,true);tick(27,true);assertNull(engine.overrideId());assertEquals("day_outside",engine.sourcePlaylistId());}
        })).toList();
    }
    @Test void nextWhilePausedQueuesTheNextDrawWithoutStartingIt(){
        String previous=engine.current().id;engine.setPaused(true,15);next(100,true);assertTrue(engine.paused());assertTrue(audio.paused);assertNull(engine.current());
        tick(200,true);assertNull(engine.current());engine.setPaused(false,200);tick(200,true);assertNotEquals(previous,engine.current().id);
    }
    @Test void nextEndsAnExplicitPreviewAndReturnsToCurrentSelection(){
        engine.preview(lib.track(manual.tracks.keySet().iterator().next()),15);next(16,false);assertFalse(engine.previewing());assertEquals("night_outside",engine.sourcePlaylistId());
    }
    @Test void nextWithNoEligiblePlaylistLeavesTheCurrentSongAlone(){
        var before=engine.current();lib.playlist("day_outside").enabled=false;assertFalse(engine.nextSong(15,lib,context(true,false),Set.of()));assertSame(before,engine.current());
    }
    @Test void repeatedNextDrawsStillRespectFisherYatesCycles(){
        String first=engine.current().id;var cycle=new HashSet<String>();cycle.add(first);next(15,true);assertTrue(cycle.add(engine.current().id));next(16,true);assertTrue(cycle.add(engine.current().id));
        String last=engine.current().id;next(17,true);assertNotEquals(last,engine.current().id);assertEquals(3,cycle.size());
    }
    @Test void pauseAndNextUseIndexedTracksWithALargeLibrary(){
        var tracks=new LibraryIndexTest.GuardedTracks();tracks.addAll(lib.tracks);lib.tracks=tracks;for(int i=0;i<70000;i++)tracks.add(new Track("unused"+i,"file","Unused",false));lib.rebuildTrackIndex();tracks.locked=true;
        engine.setPaused(true,15);next(100,true);tick(200,true);engine.setPaused(false,200);tick(200,true);assertEquals("day_outside",engine.sourcePlaylistId());
        assertTrue(PlaybackInspector.waiting(engine,lib,context(true,false),200).contains("Playing"));engine.setPaused(true,201);assertTrue(PlaybackInspector.waiting(engine,lib,context(true,false),300).contains("Music paused"));
    }
}

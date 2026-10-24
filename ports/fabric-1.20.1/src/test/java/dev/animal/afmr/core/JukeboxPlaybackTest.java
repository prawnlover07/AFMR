package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JukeboxPlaybackTest {
    @TempDir Path directory;
    private static Context context(boolean record,boolean dead){return new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"world",0,70,0,0,0.8,dead,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,record,dev.animal.afmr.core.NearbyPlayers.EMPTY);}
    private static final class Scenario {
        final Library lib=new Library();
        final Playlist p=lib.createPlaylist("Music");
        final Track track=new Track("song","file","Song",false);
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        Scenario(JukeboxMode mode){
            lib.tracks.add(track);p.tracks.put(track.id,true);lib.settings.jukeboxMode=mode;
            lib.settings.fadeInSeconds=4;lib.settings.fadeOutSeconds=2;lib.settings.bufferSeconds=0;
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;
            tick(0,false);tick(4,false);assertEquals(1,audio.gain);
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;
        }
        void tick(double now,boolean record){engine.tick(now,lib,context(record,false),Set.of());}
        void hold(){tick(5,true);tick(6,true);assertEquals(0.5,audio.gain,0.001);tick(7,true);assertTrue(engine.jukeboxHolding());}
    }
    @Test void offLeavesPlaybackAndGainAlone(){
        var s=new Scenario(JukeboxMode.OFF);s.tick(5,true);s.tick(20,true);
        assertFalse(s.engine.jukeboxActive());assertFalse(s.audio.paused);assertSame(s.track,s.engine.current());assertEquals(1,s.audio.gain);
    }
    @Test void resumeFadesPausesAndResumesTheSamePlaybackSequence(){
        var s=new Scenario(JukeboxMode.RESUME);long sequence=s.engine.playbackSequence();s.hold();
        assertTrue(s.audio.paused);assertFalse(s.engine.paused());assertSame(s.track,s.engine.current());assertEquals(0,s.audio.gain);
        s.tick(15,true);assertEquals(sequence,s.engine.playbackSequence());
        s.tick(20,false);assertFalse(s.audio.paused);assertSame(s.track,s.engine.current());assertEquals(sequence,s.engine.playbackSequence());assertEquals(0,s.audio.gain);
        s.tick(22,false);assertEquals(0.5,s.audio.gain,0.001);s.tick(24,false);assertEquals(1,s.audio.gain);
    }
    @Test void afterSilenceEndsTheSongAndStartsAFullGapAfterTheRecord(){
        var s=new Scenario(JukeboxMode.AFTER_SILENCE);s.hold();assertNull(s.engine.current());
        s.tick(19,true);assertNull(s.engine.current());s.tick(20,false);assertEquals(10,s.engine.remainingSilence(20));
        s.tick(29.99,false);assertNull(s.engine.current());s.tick(30,false);assertSame(s.track,s.engine.current());assertEquals(2,s.engine.playbackSequence());
    }
    @Test void immediateEndsTheSongAndSkipsBothGapAndBufferAfterTheRecord(){
        var s=new Scenario(JukeboxMode.IMMEDIATE);s.lib.settings.bufferSeconds=60;s.hold();assertNull(s.engine.current());
        s.tick(20,false);assertSame(s.track,s.engine.current());assertEquals(2,s.engine.playbackSequence());
        s.tick(22,false);assertEquals(0.5,s.audio.gain,0.001);
    }
    @Test void afterSilenceIsNotBypassedByAPlayOnTriggerPlaylist(){
        var s=new Scenario(JukeboxMode.AFTER_SILENCE);s.p.playOnTrigger=true;s.hold();s.tick(20,false);
        assertNull(s.engine.current());assertEquals(10,s.engine.remainingSilence(20));
    }
    @Test void stoppingTheRecordDuringFadeDoesNotJumpTheGain(){
        var s=new Scenario(JukeboxMode.RESUME);s.tick(5,true);s.tick(6,true);assertEquals(0.5,s.audio.gain,0.001);
        s.tick(6,false);assertEquals(0.5,s.audio.gain,0.001);s.tick(7,false);assertEquals(0.75,s.audio.gain,0.001);assertFalse(s.audio.paused);
    }
    @Test void manualPauseIsNeverClearedWhenTheRecordEnds(){
        var s=new Scenario(JukeboxMode.RESUME);s.hold();s.engine.setPaused(true,8);s.tick(20,false);
        assertTrue(s.engine.paused());assertTrue(s.audio.paused);s.engine.setPaused(false,30);s.tick(30,false);assertFalse(s.audio.paused);assertEquals(0,s.audio.gain);
        s.tick(32,false);assertEquals(0.5,s.audio.gain,0.001);
    }
    @Test void aPreviouslyPausedSongStaysPausedThroughoutARecord(){
        var s=new Scenario(JukeboxMode.RESUME);s.engine.setPaused(true,5);s.tick(6,true);s.tick(10,false);
        assertTrue(s.audio.paused);assertTrue(s.engine.paused());s.engine.setPaused(false,12);s.tick(12,false);assertEquals(1,s.audio.gain);
    }
    @Test void noMusicStartsWhileTheRecordIsActive(){
        for(var mode:List.of(JukeboxMode.RESUME,JukeboxMode.AFTER_SILENCE,JukeboxMode.IMMEDIATE)){
            var s=new Scenario(mode);s.engine.reset();s.tick(5,true);s.tick(100,true);assertNull(s.engine.current());
            if(mode==JukeboxMode.IMMEDIATE){s.tick(101,false);assertNotNull(s.engine.current());}
        }
    }
    @Test void disablingTheOptionReleasesTheHoldAndFadesBackIn(){
        var s=new Scenario(JukeboxMode.RESUME);s.hold();s.lib.settings.jukeboxMode=JukeboxMode.OFF;s.tick(20,true);
        assertFalse(s.engine.jukeboxActive());assertFalse(s.audio.paused);s.tick(22,true);assertEquals(0.5,s.audio.gain,0.001);
    }
    @Test void finishProtectionDoesNotPreventJukeboxFading(){
        var s=new Scenario(JukeboxMode.RESUME);s.p.finishCurrentSong=s.p.finishCurrentSongOnExit=true;s.hold();assertEquals(0,s.audio.gain);assertSame(s.track,s.engine.current());
    }
    @Test void resetReleasesAudioAndJukeboxOwnership(){
        var s=new Scenario(JukeboxMode.RESUME);s.hold();s.engine.reset();assertFalse(s.audio.paused);assertFalse(s.engine.jukeboxActive());assertNull(s.engine.current());
    }
    @Test void nextAndPreviewCanBeQueuedWithoutPlayingOverTheRecord(){
        var s=new Scenario(JukeboxMode.RESUME);s.hold();assertTrue(s.engine.nextSong(10,s.lib,context(true,false),Set.of()));s.tick(11,true);assertNull(s.engine.current());
        s.tick(20,false);assertNotNull(s.engine.current());
        var preview=new Scenario(JukeboxMode.RESUME);preview.hold();var track=new Track("preview","file","Preview",false);preview.engine.preview(track,10);preview.tick(11,true);
        assertTrue(preview.audio.paused);preview.tick(20,false);assertSame(track,preview.engine.current());assertTrue(preview.engine.previewing());
    }
    @Test void immediateDoesNotBypassTheDeathGuard(){
        var s=new Scenario(JukeboxMode.IMMEDIATE);s.hold();s.engine.tick(20,s.lib,context(false,true),Set.of());assertNull(s.engine.current());
        s.tick(21,false);assertNotNull(s.engine.current());
    }
    @Test void settingSurvivesSnapshotsAndDiskIndependentlyOfFades() throws Exception {
        var lib=Library.defaults();assertEquals(JukeboxMode.OFF,lib.settings.jukeboxMode);lib.settings.jukeboxMode=JukeboxMode.AFTER_SILENCE;
        lib.settings.fadeInSeconds=3;lib.settings.fadeOutSeconds=7;var snapshot=lib.snapshot();lib.settings.jukeboxMode=JukeboxMode.OFF;
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(snapshot);var settings=store.load().settings;assertEquals(JukeboxMode.AFTER_SILENCE,settings.jukeboxMode);assertEquals(3,settings.fadeInSeconds);assertEquals(7,settings.fadeOutSeconds);}
        lib.settings.jukeboxMode=null;lib.normalize();assertEquals(JukeboxMode.OFF,lib.settings.jukeboxMode);
    }
}

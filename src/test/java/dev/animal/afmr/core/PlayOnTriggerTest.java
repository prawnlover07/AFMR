package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayOnTriggerTest {
    @TempDir Path directory;
    private static Context context(int time){return new Context("minecraft:overworld","minecraft:plains",false,false,time,false,15,"world",0,70,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);}
    private static final class Scenario {
        final Library lib=new Library();
        final Playlist a=lib.createPlaylist("Fallback"),b=lib.createPlaylist("8pm"),c=lib.createPlaylist("Later");
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        Scenario(){
            for(var p:List.of(a,b,c)){var t=new Track(p.id,"file",p.name,false);t.durationSeconds=5;lib.tracks.add(t);p.tracks.put(t.id,true);}
            b.conditions.timeEnabled=true;b.conditions.startTime=14000;b.conditions.endTime=15000;b.playOnTrigger=true;
            c.conditions.timeEnabled=true;c.conditions.startTime=16000;c.conditions.endTime=17000;
            lib.movePlaylist(b.id,0);lib.movePlaylist(c.id,0);
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.bufferSeconds=3;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=2;
            tick(0,6000);tick(10,6000);tick(12,6000);assertEquals(a.id,engine.sourcePlaylistId());
        }
        void tick(double now,int time){engine.tick(now,lib,context(time),Set.of());}
    }
    @Test void entryStartsTheNormalFadeImmediatelyAndSkipsTheSilentGap(){
        var s=new Scenario();s.tick(14,14000);
        assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());assertEquals(s.a.id,s.engine.sourcePlaylistId());
        assertEquals(0,s.engine.remainingBuffer(14,s.lib.settings));
        assertTrue(PlaybackInspector.waiting(s.engine,s.lib,context(14000),Set.of(),14).contains("no gap"));
        s.tick(15,14000);assertEquals(0.5,s.audio.gain,0.001);
        s.tick(16,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());assertEquals(PlaybackEngine.Phase.PLAYING,s.engine.phase());
        s.tick(17,14000);assertEquals(0.5,s.audio.gain,0.001);s.tick(18,14000);assertEquals(1,s.audio.gain);
    }
    @Test void enteringDuringSilenceSkipsTheRemainingGapAndBuffer(){
        var s=new Scenario();s.engine.skip(13,s.lib);s.tick(14,14000);
        assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void initialActivationCanStartImmediatelyDespiteLongGlobalDelays(){
        var s=new Scenario();s.engine.reset();s.lib.settings.bufferSeconds=999;s.lib.settings.silenceMinSeconds=s.lib.settings.silenceMaxSeconds=600;
        s.tick(0,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void laterSongsKeepNormalSilenceAndMatchingTicksDoNotRetrigger(){
        var s=new Scenario();s.tick(14,14000);s.tick(16,14000);long sequence=s.engine.playbackSequence();
        s.tick(17,14000);s.tick(18,14000);assertEquals(sequence,s.engine.playbackSequence());
        s.audio.current=null;s.tick(20,14000);s.tick(29.99,14000);assertNull(s.engine.current());
        s.tick(30,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void offKeepsTheExistingBufferFadeAndGap(){
        var s=new Scenario();s.b.playOnTrigger=false;s.tick(14,14000);s.tick(16.99,14000);assertEquals(PlaybackEngine.Phase.PLAYING,s.engine.phase());
        s.tick(17,14000);assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());s.tick(19,14000);assertNull(s.engine.current());
        s.tick(28.99,14000);assertNull(s.engine.current());s.tick(29,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void sourceExitAndIncomingEntryProtectionRemainIndependent(){
        for(boolean outgoing:List.of(false,true)){
            var s=new Scenario();s.a.finishCurrentSongOnExit=outgoing;s.b.finishCurrentSong=!outgoing;
            s.tick(14,14000);s.tick(18,14000);assertEquals(s.a.id,s.engine.sourcePlaylistId());
            s.audio.current=null;s.tick(20,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
        }
    }
    @Test void leavingBeforeTheProtectedSongEndsCancelsThePendingEntryGapSkip(){
        var s=new Scenario();s.a.finishCurrentSongOnExit=true;s.tick(14,14000);s.tick(16,16000);s.tick(19,16000);
        s.audio.current=null;s.tick(20,16000);assertNull(s.engine.current());s.tick(30,16000);assertEquals(s.c.id,s.engine.sourcePlaylistId());
    }
    @Test void manualSkipCancelsAPendingTriggerAndUsesNormalSilence(){
        var s=new Scenario();s.tick(14,14000);s.engine.skip(15,s.lib);s.tick(24.99,14000);assertNull(s.engine.current());
        s.tick(25,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void previewsAndHeldOverridesRetainPriority(){
        var s=new Scenario();var t=new Track("preview","file","Preview",false);s.engine.preview(t,13,false);s.tick(14,14000);s.tick(18,14000);assertSame(t,s.engine.current());
        s.audio.current=null;s.tick(20,14000);s.tick(29.99,14000);assertNull(s.engine.current());s.tick(30,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
        var held=new Scenario();assertTrue(held.engine.overridePlaylist(held.a.id,OverrideMode.UNTIL_RELEASED,13,held.lib,Set.of()));
        held.tick(14,14000);held.tick(16,14000);assertEquals(held.a.id,held.engine.sourcePlaylistId());assertEquals(held.a.id,held.engine.overrideId());
    }
    @Test void pauseDefersTheTriggerUntilPlaybackResumes(){
        var s=new Scenario();s.engine.setPaused(true,13);s.tick(14,14000);assertEquals(s.a.id,s.engine.sourcePlaylistId());
        s.engine.setPaused(false,20);s.tick(20,14000);assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
        s.tick(22,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void enteringDuringAnotherFadeDoesNotRestartThatFade(){
        var s=new Scenario();s.tick(14,16000);s.tick(17,16000);s.tick(18,14000);s.tick(19,14000);
        assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void zeroFadeStillHasNoGap(){
        var s=new Scenario();s.lib.settings.fadeInSeconds=s.lib.settings.fadeOutSeconds=0;s.tick(14,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void fractionalOrChangedBufferCannotDelayAnAcceptedTrigger(){
        var s=new Scenario();s.lib.settings.bufferSeconds=0.1;s.tick(14.2,14000);
        assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
        s.lib.settings.bufferSeconds=600;s.tick(16.2,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void returnToAutomaticDoesNotCancelAnAlreadyAutomaticTrigger(){
        var s=new Scenario();s.tick(14,14000);s.engine.returnAutomatic();s.tick(16,14000);
        assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void returningToAnAlreadyPlayingProtectedSourceDoesNotSkipItsNextNormalGap(){
        var s=new Scenario();s.tick(14,14000);s.tick(16,14000);s.b.finishCurrentSongOnExit=true;
        s.tick(17,16000);s.tick(20,16000);s.tick(21,14000);
        s.audio.current=null;s.tick(22,14000);s.tick(31.99,14000);assertNull(s.engine.current());s.tick(32,14000);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void triggerDoesNotBypassTimeWindowFitting(){
        var s=new Scenario();s.b.conditions.fitSongsToTimeWindow=true;s.b.conditions.endTime=14001;
        s.tick(14,14000);s.tick(16,14000);assertNull(s.engine.current());assertTrue(s.engine.waitingForTimeFit());assertSame(s.b,s.engine.selected());
    }
    @Test void disabledManualOnlyAndEmptyPlaylistsCannotStealPlayback(){
        for(int kind=0;kind<3;kind++){
            var s=new Scenario();if(kind==0)s.b.enabled=false;else if(kind==1)s.b.manualOnly=true;else s.b.tracks.clear();
            s.tick(14,14000);s.tick(20,14000);assertEquals(s.a.id,s.engine.sourcePlaylistId());
        }
    }
    @Test void defaultsClonesSnapshotsAndDiskPreserveTheIndependentToggle() throws Exception {
        var lib=Library.defaults();assertTrue(lib.playlists.stream().noneMatch(p->p.playOnTrigger));
        var p=lib.createPlaylist("8pm");p.playOnTrigger=true;var clone=lib.clonePlaylist(p.id,"Copy");assertTrue(clone.playOnTrigger);clone.playOnTrigger=false;
        var snapshot=lib.snapshot();p.playOnTrigger=false;
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(snapshot);var loaded=store.load();assertTrue(loaded.playlist(p.id).playOnTrigger);assertFalse(loaded.playlist(clone.id).playOnTrigger);}
    }
}

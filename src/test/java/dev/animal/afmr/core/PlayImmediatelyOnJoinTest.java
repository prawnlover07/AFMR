package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayImmediatelyOnJoinTest {
    @TempDir Path directory;
    private static Context context(String dimension,boolean dead,boolean record){
        return new Context(dimension,"minecraft:plains",false,false,6000,false,15,"world",0,70,0,0,0.8,dead,
            StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,record,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    }
    private static final class Scenario {
        final Library lib=new Library();
        final Playlist fallback=lib.createPlaylist("Fallback"),specific=lib.createPlaylist("Overworld");
        final Track a=new Track("a","generated-a.wav","Fallback",false),b=new Track("b","generated-b.wav","Overworld",false);
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        Scenario(){
            lib.tracks.addAll(List.of(a,b));fallback.tracks.put(a.id,true);specific.tracks.put(b.id,true);
            a.durationSeconds=b.durationSeconds=10;
            specific.conditions.dimensionEnabled=true;specific.conditions.dimension="minecraft:overworld";lib.movePlaylist(specific.id,0);
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=600;lib.settings.bufferSeconds=60;
            lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=2;
        }
        void join(){engine.joinedWorld(lib.settings);}
        void tick(double now){engine.tick(now,lib,context("minecraft:overworld",false,false),Set.of());}
        void enable(){lib.settings.playImmediatelyOnJoin=true;join();}
    }
    @Test void offByDefaultKeepsTheInitialGap(){
        var s=new Scenario();assertFalse(s.lib.settings.playImmediatelyOnJoin);s.join();s.tick(0);s.tick(599.99);
        assertNull(s.engine.current());s.tick(600);assertSame(s.b,s.engine.current());
    }
    @Test void enabledUsesTheFreshWinningContextAndNormalFadeIn(){
        var s=new Scenario();s.enable();s.tick(7);
        assertSame(s.b,s.engine.current());assertSame(s.specific,s.engine.automaticSelected());assertNull(s.engine.overrideId());
        assertEquals(0,s.engine.remainingSilence(7));assertEquals(0,s.engine.remainingBuffer(7,s.lib.settings));
        s.tick(8);assertEquals(0.5,s.audio.gain,0.001);s.tick(9);assertEquals(1,s.audio.gain);
    }
    @Test void laterSongsUseNormalSilenceWithoutRepeatedStarts(){
        var s=new Scenario();s.enable();s.tick(0);long sequence=s.engine.playbackSequence();s.tick(1);s.tick(2);
        assertEquals(sequence,s.engine.playbackSequence());s.audio.current=null;s.tick(10);s.tick(609.99);assertNull(s.engine.current());
        s.tick(610);assertSame(s.b,s.engine.current());assertEquals(sequence+1,s.engine.playbackSequence());
    }
    @Test void resetDoesNotInferAJoinButTheNextExplicitJoinRearms(){
        var s=new Scenario();s.enable();s.tick(0);s.engine.reset();s.tick(10);assertNull(s.engine.current());assertEquals(600,s.engine.remainingSilence(10));
        s.engine.reset();s.join();s.tick(20);assertSame(s.b,s.engine.current());
    }
    @Test void contextChangesRetainTheNormalBufferFadeAndGap(){
        var s=new Scenario();s.enable();s.tick(0);s.tick(2);
        var nether=context("minecraft:the_nether",false,false);
        s.engine.tick(10,s.lib,nether,Set.of());s.engine.tick(69.99,s.lib,nether,Set.of());assertSame(s.b,s.engine.current());
        s.engine.tick(70,s.lib,nether,Set.of());assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
        s.engine.tick(72,s.lib,nether,Set.of());assertNull(s.engine.current());assertEquals(600,s.engine.remainingSilence(72));
    }
    @Test void waitsForTheFirstEligiblePlaylistAndSkipsDisabledManualOnlyAndEmptyEntries(){
        var s=new Scenario();s.specific.enabled=false;s.fallback.manualOnly=true;s.enable();s.tick(0);assertNull(s.engine.current());
        var empty=s.lib.createPlaylist("Empty");s.lib.movePlaylist(empty.id,0);
        s.fallback.manualOnly=false;s.tick(100);assertSame(s.a,s.engine.current());assertSame(s.fallback,s.engine.selected());
    }
    @Test void pauseDefersTheFirstStartAndUsesCurrentContextOnResume(){
        var s=new Scenario();s.enable();s.engine.setPaused(true,0);s.tick(1);assertNull(s.engine.current());
        s.engine.setPaused(false,100);s.engine.tick(100,s.lib,context("minecraft:the_nether",false,false),Set.of());assertSame(s.a,s.engine.current());
    }
    @Test void deathGuardStillPreventsAJoinStart(){
        var s=new Scenario();s.enable();s.engine.tick(0,s.lib,context("minecraft:overworld",true,false),Set.of());assertNull(s.engine.current());
        s.tick(10);assertSame(s.b,s.engine.current());
    }
    @Test void jukeboxPoliciesKeepTheirOwnReleaseTiming(){
        for(var mode:List.of(JukeboxMode.RESUME,JukeboxMode.AFTER_SILENCE,JukeboxMode.IMMEDIATE)){
            var s=new Scenario();s.lib.settings.jukeboxMode=mode;s.enable();
            s.engine.tick(0,s.lib,context("minecraft:overworld",false,true),Set.of());assertNull(s.engine.current());assertTrue(s.engine.jukeboxHolding());
            s.tick(20);
            if(mode==JukeboxMode.IMMEDIATE)assertSame(s.b,s.engine.current());else{assertNull(s.engine.current());assertEquals(600,s.engine.remainingSilence(20));}
        }
    }
    @Test void manualSkipCancelsAPendingJoinStart(){
        var s=new Scenario();s.enable();s.engine.skip(0,s.lib);s.tick(0);s.tick(599.99);assertNull(s.engine.current());
        s.tick(600);assertSame(s.b,s.engine.current());
    }
    @Test void queuedRequestsRetainPriorityAndTheirNormalGap(){
        var s=new Scenario();s.enable();assertTrue(s.engine.enqueue(s.a.id,s.fallback.id,s.lib));s.tick(0);s.tick(599.99);assertNull(s.engine.current());
        s.tick(600);assertSame(s.a,s.engine.current());assertTrue(s.engine.queuedPlaying());
    }
    @Test void previewsAndOverridesRetainTheirUsualPriorityAndLaterGap(){
        var s=new Scenario();s.enable();s.engine.preview(s.a,0);s.tick(2);assertSame(s.a,s.engine.current());
        s.audio.current=null;s.tick(10);assertEquals(600,s.engine.remainingSilence(10));
        s.engine.reset();s.join();assertTrue(s.engine.overridePlaylist(s.fallback.id,OverrideMode.UNTIL_RELEASED,20,s.lib,Set.of()));
        s.tick(20);assertSame(s.a,s.engine.current());assertEquals(s.fallback.id,s.engine.overrideId());
    }
    @Test void manualNextAndFrequencyReschedulingConsumeThePendingJoin(){
        var s=new Scenario();s.enable();assertTrue(s.engine.nextSong(0,s.lib,context("minecraft:overworld",false,false),Set.of()));s.tick(0);assertSame(s.b,s.engine.current());
        s.audio.current=null;s.tick(10);assertEquals(600,s.engine.remainingSilence(10));
        s.engine.reset();s.join();s.engine.rescheduleSilence(20,s.lib);s.tick(20);assertNull(s.engine.current());assertEquals(600,s.engine.remainingSilence(20));
    }
    @Test void doesNotBypassTimeWindowFittingOrDecoderFailureBackoff(){
        var s=new Scenario();s.specific.conditions.timeEnabled=true;s.specific.conditions.startTime=6000;s.specific.conditions.endTime=6001;
        s.specific.conditions.fitSongsToTimeWindow=true;s.enable();s.tick(0);assertNull(s.engine.current());assertTrue(s.engine.waitingForTimeFit());
        var bad=new Scenario();bad.audio.fail=bad.b.id;bad.enable();bad.tick(0);assertNull(bad.engine.current());assertEquals(1,bad.engine.remainingSilence(0));
        bad.tick(0.1);assertNull(bad.engine.current());assertTrue(bad.engine.failedTracks().contains(bad.b.id));
    }
    @Test void togglingInWorldDoesNotCreateAJoinAndTurningOffCancelsAPendingOne(){
        var s=new Scenario();s.tick(0);s.lib.settings.playImmediatelyOnJoin=true;s.tick(1);assertNull(s.engine.current());assertEquals(599,s.engine.remainingSilence(1));
        s.engine.reset();s.join();s.lib.settings.playImmediatelyOnJoin=false;s.tick(10);assertNull(s.engine.current());assertEquals(600,s.engine.remainingSilence(10));
    }
    @Test void snapshotsDiskProfilesAndNumericDefaultsPreserveTheIndependentSetting()throws Exception{
        var lib=Library.defaults();lib.settings.playImmediatelyOnJoin=true;
        var snapshot=lib.snapshot();lib.settings.playImmediatelyOnJoin=false;assertTrue(snapshot.settings.playImmediatelyOnJoin);
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(snapshot);assertTrue(store.load().settings.playImmediatelyOnJoin);}
        lib.settings.playImmediatelyOnJoin=true;String copy=lib.profiles.create(lib,"Copy","default"),fresh=lib.profiles.create(lib,"Fresh",null);
        lib.settings.playImmediatelyOnJoin=false;lib.profiles.activate(lib,copy);assertTrue(lib.settings.playImmediatelyOnJoin);
        lib.settings.restoreMinecraftPlaybackDefaults();assertTrue(lib.settings.playImmediatelyOnJoin);
        lib.profiles.activate(lib,fresh);assertFalse(lib.settings.playImmediatelyOnJoin);
        lib.profiles.activate(lib,copy);assertTrue(lib.settings.playImmediatelyOnJoin);
    }
}

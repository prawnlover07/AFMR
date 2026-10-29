package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import dev.animal.afmr.library.PlaylistPresets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistSilenceTest {
    @TempDir Path directory;
    private static Context context(boolean incoming,boolean record){
        return new Context("minecraft:overworld",incoming?"test:b":"test:a",true,false,6000,false,15,"world",0,70,0,0,0.8,false,
            StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,record,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    }
    private static void range(Playlist p,double min,double max){p.customSilence=true;p.silenceMinSeconds=min;p.silenceMaxSeconds=max;}
    private static final class Scenario {
        final Library lib=new Library();
        final Playlist a=lib.createPlaylist("Dark"),b=lib.createPlaylist("Combat");
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        Scenario(){
            lib.settings.bufferSeconds=lib.settings.fadeOutSeconds=0;
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=20;
            range(a,600,600);range(b,3,3);
            b.conditions.biomeEnabled=true;b.conditions.biomes.add("test:b");lib.movePlaylist(b.id,0);
            for(var p:List.of(a,b)){var t=new Track(p.id,"generated.wav",p.name,false);t.durationSeconds=10;lib.tracks.add(t);p.tracks.put(t.id,true);}
        }
        void tick(double now,boolean incoming){engine.tick(now,lib,context(incoming,false),Set.of());}
        void record(double now,boolean incoming){engine.tick(now,lib,context(incoming,true),Set.of());}
        void start(){tick(0,false);engine.playNow(0);tick(0,false);assertNotNull(engine.current());}
    }
    @Test void defaultsInheritGlobalInitialAndLaterGaps(){
        var s=new Scenario();s.a.customSilence=false;s.tick(0,false);assertEquals(20,s.engine.remainingSilence(0));s.tick(20,false);
        assertEquals(s.a.id,s.engine.sourcePlaylistId());s.audio.current=null;s.tick(22,false);assertEquals(20,s.engine.remainingSilence(22));
    }
    @Test void fixedTenMinuteGapAndNormalSkipUseCustomTiming(){
        var s=new Scenario();s.tick(10,false);assertEquals(600,s.engine.remainingSilence(10));s.tick(609.99,false);assertNull(s.engine.current());
        s.tick(610,false);assertNotNull(s.engine.current());s.engine.skip(611,s.lib);assertEquals(600,s.engine.remainingSilence(611));
    }
    @Test void shorterIncomingRangeCountsElapsedAndStartsImmediatelyIfAlreadySatisfied(){
        var s=new Scenario();s.tick(0,false);s.tick(100,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void shorterIncomingRangeWaitsOnlyItsUnspentPart(){
        var s=new Scenario();s.tick(10,false);s.tick(11,true);assertEquals(2,s.engine.remainingSilence(11));assertNull(s.engine.current());
        s.tick(12.99,true);assertNull(s.engine.current());s.tick(13,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void longerIncomingRangeAlsoCountsElapsed(){
        var s=new Scenario();s.tick(10,true);s.tick(12,false);assertEquals(598,s.engine.remainingSilence(12));s.tick(610,false);assertEquals(s.a.id,s.engine.sourcePlaylistId());
    }
    @Test void repeatedChangesKeepTheOriginalAnchor(){
        var s=new Scenario();s.tick(0,false);s.tick(1,true);s.tick(2,false);assertEquals(598,s.engine.remainingSilence(2));s.tick(4,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void incomingRangeStillRespectsContextBuffer(){
        var s=new Scenario();s.lib.settings.bufferSeconds=5;s.tick(0,false);s.tick(100,true);assertNull(s.engine.current());s.tick(104.99,true);assertNull(s.engine.current());
        s.tick(105,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void randomGapIsSampledOnceUntilTheRangeChanges(){
        var s=new Scenario();range(s.a,600,1200);s.tick(0,false);double end=s.engine.remainingSilence(0);assertTrue(end>=600&&end<1200);
        for(int i=1;i<100;i++){s.tick(i,false);assertEquals(end-i,s.engine.remainingSilence(i),0.00001);}
        range(s.b,600,1200);s.tick(100,true);assertEquals(end-100,s.engine.remainingSilence(100),0.00001);
    }
    @Test void liveEnableDisableAndRangeEditsReuseElapsedTime(){
        var s=new Scenario();s.tick(0,false);s.a.customSilence=false;s.tick(5,false);assertEquals(15,s.engine.remainingSilence(5));
        s.a.customSilence=true;s.tick(6,false);assertEquals(594,s.engine.remainingSilence(6));range(s.a,10,10);s.tick(7,false);assertEquals(3,s.engine.remainingSilence(7));
        s.tick(10,false);assertNotNull(s.engine.current());
    }
    @Test void pauseDoesNotCountAsElapsedSilenceOnRangeChange(){
        var s=new Scenario();s.tick(0,false);s.engine.setPaused(true,1);s.tick(90,true);s.engine.setPaused(false,100);s.tick(100,true);
        assertEquals(2,s.engine.remainingSilence(100));s.tick(102,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void ordinaryTransitionWaitStartsAfterFadeOut(){
        var s=new Scenario();s.lib.settings.fadeOutSeconds=2;s.start();s.tick(1,false);s.tick(2,true);assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
        s.tick(4,true);assertNull(s.engine.current());assertEquals(3,s.engine.remainingSilence(4));s.tick(7,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void retainedSongEndsThenUsesTheIncomingRange(){
        var s=new Scenario();s.a.finishCurrentSongOnExit=true;s.start();s.tick(10,true);assertEquals(s.a.id,s.engine.sourcePlaylistId());
        s.audio.current=null;s.tick(20,true);assertEquals(3,s.engine.remainingSilence(20));s.tick(23,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void playOnTriggerSkipsCustomGapButLaterSongsUseIt(){
        var s=new Scenario();s.b.playOnTrigger=true;s.tick(0,false);s.tick(1,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
        s.audio.current=null;s.tick(2,true);assertEquals(3,s.engine.remainingSilence(2));
    }
    @Test void joinBypassesOnlyInitialCustomGap(){
        var s=new Scenario();s.lib.settings.playImmediatelyOnJoin=true;s.engine.joinedWorld(s.lib.settings);s.tick(0,false);assertNotNull(s.engine.current());
        s.audio.current=null;s.tick(2,false);assertEquals(600,s.engine.remainingSilence(2));
    }
    @Test void playNowAndNextRemainExplicitBypassesAcrossASelectionChange(){
        var s=new Scenario();s.tick(0,false);s.engine.playNow(1);s.tick(1,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
        assertTrue(s.engine.nextSong(2,s.lib,context(false,false),Set.of()));s.tick(2,false);assertEquals(s.a.id,s.engine.sourcePlaylistId());
    }
    @Test void heldOverrideSkipsFirstGapAndThenUsesItsOwnRange(){
        var s=new Scenario();s.tick(0,false);assertTrue(s.engine.overridePlaylist(s.b.id,OverrideMode.UNTIL_RELEASED,1,s.lib,Set.of()));s.tick(1,false);
        assertEquals(s.b.id,s.engine.sourcePlaylistId());s.audio.current=null;s.tick(5,false);assertEquals(3,s.engine.remainingSilence(5));s.tick(8,false);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void oneSongOverrideReturnsToAutomaticRange(){
        var s=new Scenario();s.tick(0,false);s.engine.overridePlaylist(s.b.id,OverrideMode.ONE_SONG,1,s.lib,Set.of());s.tick(1,false);
        s.audio.current=null;s.tick(5,false);assertNull(s.engine.overrideId());assertEquals(600,s.engine.remainingSilence(5));
    }
    @Test void queuedPlaylistUsesItsRangeAndQueueEditsCountElapsed(){
        var s=new Scenario();s.tick(0,false);assertTrue(s.engine.enqueue(s.b.id,s.b.id,s.lib));s.tick(1,false);assertEquals(2,s.engine.remainingSilence(1));
        s.tick(3,false);assertTrue(s.engine.queuedPlaying());assertEquals(s.b.id,s.engine.sourcePlaylistId());s.audio.current=null;s.tick(4,false);assertEquals(600,s.engine.remainingSilence(4));
    }
    @Test void invalidQueueHeadCannotImposeItsLongRangeOnTheNextRequest(){
        var s=new Scenario();s.engine.enqueue(s.a.id,s.a.id,s.lib);s.engine.enqueue(s.b.id,s.b.id,s.lib);s.tick(0,false);
        s.a.tracks.put(s.a.id,false);s.tick(1,false);assertEquals(2,s.engine.remainingSilence(1));assertEquals(1,s.engine.queue().size());
        s.tick(3,false);assertEquals(s.b.id,s.engine.sourcePlaylistId());assertTrue(s.engine.queuedPlaying());
    }
    @Test void reorderingAndClearingQueueRecalculateFromTheSameAnchor(){
        var s=new Scenario();s.engine.enqueue(s.a.id,s.a.id,s.lib);s.engine.enqueue(s.b.id,s.b.id,s.lib);s.tick(0,false);
        var second=s.engine.queue().snapshot().get(1);s.engine.queue().move(second.id(),0);s.tick(1,false);assertEquals(2,s.engine.remainingSilence(1));
        s.engine.queue().clear();s.tick(2,false);assertEquals(598,s.engine.remainingSilence(2));
    }
    @Test void customSilenceDoesNotBypassTimeWindowFitting(){
        var s=new Scenario();range(s.a,3,3);s.a.conditions.timeEnabled=true;s.a.conditions.fitSongsToTimeWindow=true;s.a.conditions.startTime=0;s.a.conditions.endTime=6001;
        s.tick(0,false);s.tick(3,false);assertNull(s.engine.current());assertTrue(s.engine.waitingForTimeFit());
        range(s.b,3,3);s.tick(4,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void queueWithoutSourceUsesGlobalTiming(){
        var s=new Scenario();s.engine.enqueue(s.b.id,"",s.lib);s.tick(0,false);assertEquals(20,s.engine.remainingSilence(0));s.tick(20,false);assertTrue(s.engine.queuedPlaying());assertNull(s.engine.sourcePlaylistId());
    }
    @Test void failureBackoffIsNotReplacedByIncomingCustomRange(){
        var s=new Scenario();s.start();s.engine.failed(s.a.id,"fixture decode failure",1,s.lib);s.tick(1.5,true);assertEquals(0.5,s.engine.remainingSilence(1.5));
        s.tick(2,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void jukeboxAfterSilenceUsesFullCustomRangeOnRelease(){
        var s=new Scenario();s.lib.settings.jukeboxMode=JukeboxMode.AFTER_SILENCE;s.start();s.record(1,false);assertTrue(s.engine.jukeboxHolding());
        s.tick(100,true);assertEquals(3,s.engine.remainingSilence(100));s.tick(103,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void jukeboxImmediateBypassesCustomRange(){
        var s=new Scenario();s.lib.settings.jukeboxMode=JukeboxMode.IMMEDIATE;s.tick(0,false);s.record(1,false);s.tick(100,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void jukeboxResumeFreezesTheGapAnchor(){
        var s=new Scenario();s.lib.settings.jukeboxMode=JukeboxMode.RESUME;s.tick(0,false);s.record(1,false);s.tick(100,true);
        assertEquals(2,s.engine.remainingSilence(100));s.tick(102,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void globalFrequencyChangeDoesNotRestartCustomGap(){
        var s=new Scenario();s.tick(0,false);s.lib.settings.silenceMinSeconds=s.lib.settings.silenceMaxSeconds=30;s.engine.rescheduleSilence(100,s.lib);
        assertEquals(500,s.engine.remainingSilence(100));s.a.customSilence=false;s.engine.rescheduleSilence(100,s.lib);assertEquals(30,s.engine.remainingSilence(100));
    }
    @Test void resetDiscardsTheOldAnchor(){
        var s=new Scenario();s.tick(0,false);s.engine.reset();s.tick(100,true);assertEquals(3,s.engine.remainingSilence(100));
    }
    @Test void normalizationBoundsActiveAndInactiveRanges(){
        var s=new Scenario();s.a.silenceMinSeconds=Double.NaN;s.a.silenceMaxSeconds=-2;s.b.silenceMinSeconds=90000;s.b.silenceMaxSeconds=Double.POSITIVE_INFINITY;
        var profile=s.lib.profiles.create(s.lib,"Copy",s.lib.profiles.activeId);s.lib.normalize();
        for(var p:s.lib.playlists){assertTrue(p.silenceMinSeconds>=0&&p.silenceMaxSeconds>=p.silenceMinSeconds&&p.silenceMaxSeconds<=86400);}
        for(var p:s.lib.profiles.playlists(s.lib,profile)){assertTrue(Double.isFinite(p.silenceMinSeconds)&&p.silenceMaxSeconds>=p.silenceMinSeconds);}
    }
    @Test void cloneSnapshotProfileAndStoreRetainIndependentValues()throws Exception{
        var s=new Scenario();var clone=s.lib.clonePlaylist(s.a.id,"Branch");assertTrue(clone.customSilence);assertEquals(600,clone.silenceMinSeconds);
        var profile=s.lib.profiles.create(s.lib,"Copy",s.lib.profiles.activeId);var snapshot=s.lib.snapshot();
        range(s.a,5,6);assertEquals(600,snapshot.playlist(s.a.id).silenceMinSeconds);assertEquals(600,s.lib.profiles.playlists(s.lib,profile).stream().filter(p->p.id.equals(s.a.id)).findFirst().orElseThrow().silenceMinSeconds);
        var store=new LibraryStore(directory.resolve("library.json"));store.save(s.lib);var loaded=store.load();assertTrue(loaded.playlist(s.a.id).customSilence);assertEquals(5,loaded.playlist(s.a.id).silenceMinSeconds);assertEquals(6,loaded.playlist(s.a.id).silenceMaxSeconds);
    }
    @Test void playlistExportRetainsRangesAndRejectsInvalidHiddenValues()throws Exception{
        var s=new Scenario();var data=PlaylistPresets.capture(s.lib,Set.of(s.a.id),false);var file=directory.resolve("silence.afmrp");PlaylistPresets.write(file,data);
        var loaded=PlaylistPresets.read(file);assertTrue(loaded.playlists.get(0).customSilence);assertEquals(600,loaded.playlists.get(0).silenceMinSeconds);
        var p=data.playlists.get(0);p.customSilence=false;
        for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,86401}){p.silenceMinSeconds=bad;assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(data));}
        p.silenceMinSeconds=600;p.silenceMaxSeconds=3;assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(data));
    }
}

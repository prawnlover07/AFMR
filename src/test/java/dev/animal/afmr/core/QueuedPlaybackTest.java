package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.ListeningHistory.Origin;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueuedPlaybackTest {
    private static Context context(boolean dead,boolean record){return new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"world",0,70,0,0,0.8,dead,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,record,dev.animal.afmr.core.NearbyPlayers.EMPTY);}
    private static final class Scenario {
        final Library lib=new Library();
        final Playlist automatic=lib.createPlaylist("Automatic"),requests=lib.createPlaylist("Requests");
        final Track a=new Track("a","file","A",false),b=new Track("b","file","B",false),c=new Track("c","file","C",false);
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        Scenario(){lib.tracks.addAll(List.of(a,b,c));automatic.tracks.put(a.id,true);requests.manualOnly=true;requests.tracks.put(b.id,true);requests.tracks.put(c.id,true);lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.bufferSeconds=2;}
        void tick(double now){engine.tick(now,lib,context(false,false),Set.of());}
        void start(){tick(0);tick(10);assertSame(a,engine.current());}
        void enqueue(Track track){assertTrue(engine.enqueue(track.id,requests.id,lib));}
        void end(double now){audio.current=null;tick(now);}
    }
    @Test void queueWaitsForCurrentSongAndKeepsNormalGapsThenReturnsToAutomatic(){
        var s=new Scenario();s.start();s.enqueue(s.b);s.enqueue(s.c);s.tick(11);assertSame(s.a,s.engine.current());
        s.end(12);s.tick(21.99);assertNull(s.engine.current());s.tick(22);assertSame(s.b,s.engine.current());assertTrue(s.engine.queuedPlaying());
        s.end(23);s.tick(33);assertSame(s.c,s.engine.current());s.end(34);s.tick(44);assertSame(s.a,s.engine.current());assertFalse(s.engine.queuedPlaying());
        assertEquals(List.of(Origin.AUTOMATIC,Origin.QUEUE,Origin.QUEUE,Origin.AUTOMATIC),s.engine.history().snapshot().reversed().stream().map(ListeningHistory.Entry::origin).toList());
    }
    @Test void pendingQueueKeepsExistingSilenceButBypassesAutomaticContextBuffer(){
        var s=new Scenario();s.lib.settings.bufferSeconds=60;s.enqueue(s.b);s.tick(0);s.tick(9.99);assertNull(s.engine.current());s.tick(10);assertSame(s.b,s.engine.current());
    }
    @Test void queuedTrackIgnoresAutomaticPlaylistChangesUntilItEnds(){
        var s=new Scenario();s.enqueue(s.b);s.tick(0);s.tick(10);s.automatic.enabled=false;s.tick(11);
        assertSame(s.b,s.engine.current());assertTrue(s.engine.queuedPlaying());s.end(12);s.tick(22);assertNull(s.engine.current());
    }
    @Test void automaticOffAllowsQueueButSongOffDoesNot(){
        var s=new Scenario();s.requests.enabled=false;s.enqueue(s.b);s.requests.tracks.put(s.c.id,false);
        assertFalse(s.engine.enqueue(s.c.id,s.requests.id,s.lib));s.tick(0);s.tick(10);assertSame(s.b,s.engine.current());
    }
    @Test void disablingOrRemovingAnAssignmentBeforeItsTurnSkipsIt(){
        for(boolean disable:List.of(true,false)){
            var s=new Scenario();s.enqueue(s.b);s.enqueue(s.c);
            if(disable)s.requests.tracks.put(s.b.id,false);else s.requests.tracks.remove(s.b.id);
            s.tick(0);s.tick(10);assertSame(s.c,s.engine.current());assertTrue(s.engine.queueMessage().contains("Skipped B"));assertTrue(s.engine.queue().empty());
        }
    }
    @Test void removedSongsAndDeletedSourcePlaylistsDoNotBlockLaterRequests(){
        var s=new Scenario();s.enqueue(s.b);assertTrue(s.engine.enqueue(s.c.id,"",s.lib));s.lib.removePlaylist(s.requests.id);
        s.tick(0);s.tick(10);assertSame(s.c,s.engine.current());
        s.engine.reset();assertTrue(s.engine.enqueue(s.b.id,"",s.lib));assertTrue(s.engine.enqueue(s.c.id,"",s.lib));
        s.lib.tracks.removeIf(t->t.id.equals(s.b.id));s.lib.rebuildTrackIndex();s.tick(20);s.tick(30);assertSame(s.c,s.engine.current());
    }
    @Test void failedQueueStartHasNoHistoryAndNextRequestRetriesWithoutBlockingForever(){
        var s=new Scenario();s.enqueue(s.b);s.enqueue(s.c);s.audio.fail=s.b.id;s.tick(0);s.tick(10);
        assertNull(s.engine.current());assertEquals(0,s.engine.history().size());assertTrue(s.engine.failedTracks().contains(s.b.id));
        assertFalse(s.engine.enqueue(s.b.id,s.requests.id,s.lib));s.tick(11);assertSame(s.c,s.engine.current());assertEquals(1,s.engine.history().size());
    }
    @Test void vanillaQueueUsesExplicitIntentEvenWhenItsAutomaticPoolChanges(){
        var s=new Scenario();var vanilla=new Track("vanilla:test","minecraft:music/test","Vanilla",true);s.lib.tracks.add(vanilla);s.requests.includeVanilla=true;
        assertTrue(s.engine.enqueue(vanilla.id,s.requests.id,s.lib));s.tick(0);s.tick(10);assertSame(vanilla,s.engine.current());
    }
    @Test void queueCapacityIsBoundedAndDuplicateRequestsAreDeliberate(){
        var s=new Scenario();for(int i=0;i<PlaybackQueue.LIMIT;i++)s.enqueue(s.b);
        assertFalse(s.engine.enqueue(s.b.id,s.requests.id,s.lib));assertEquals(PlaybackQueue.LIMIT,s.engine.queue().size());
        s.tick(0);s.tick(10);s.end(11);s.tick(21);assertSame(s.b,s.engine.current());assertEquals(2,s.engine.history().size());
    }
    @Test void editingQueueOrderAndRemovingOneDuplicateUsesStableRequestIdentity(){
        var s=new Scenario();s.enqueue(s.b);s.enqueue(s.c);s.enqueue(s.b);var before=s.engine.queue().snapshot();
        assertTrue(s.engine.queue().move(before.get(2).id(),0));assertTrue(s.engine.queue().remove(before.get(0).id()));
        assertEquals(List.of(before.get(2).id(),before.get(1).id()),s.engine.queue().snapshot().stream().map(PlaybackQueue.Entry::id).toList());
    }
    @Test void nextUsesQueueBeforeAutomaticAndWorksWithoutAnAutomaticWinner(){
        var s=new Scenario();s.start();s.enqueue(s.b);s.automatic.enabled=false;
        assertTrue(s.engine.nextSong(11,s.lib,context(false,false),Set.of()));s.tick(11);assertSame(s.b,s.engine.current());
    }
    @Test void ordinarySkipKeepsNormalGapBeforeTheNextQueuedSong(){
        var s=new Scenario();s.enqueue(s.b);s.enqueue(s.c);s.tick(0);s.tick(10);s.engine.skip(11,s.lib);
        s.tick(20.99);assertNull(s.engine.current());s.tick(21);assertSame(s.c,s.engine.current());
    }
    @Test void pausedQueueAndPendingGapStayFrozenIncludingNext(){
        var s=new Scenario();s.enqueue(s.b);s.tick(0);s.engine.setPaused(true,3);s.tick(50);assertEquals(7,s.engine.remainingSilence(50));
        assertTrue(s.engine.nextSong(51,s.lib,context(false,false),Set.of()));s.tick(51);assertNull(s.engine.current());assertEquals(1,s.engine.queue().size());
        s.engine.setPaused(false,60);s.tick(60);assertSame(s.b,s.engine.current());
    }
    @Test void previewInterruptsWithoutDiscardingPendingQueue(){
        var s=new Scenario();s.enqueue(s.b);s.engine.preview(s.c,0);s.tick(2);assertSame(s.c,s.engine.current());assertTrue(s.engine.previewing());
        s.end(3);s.tick(13);assertSame(s.b,s.engine.current());
    }
    @Test void queuedRequestsTemporarilyTakePrecedenceAndThenResumeAHeldOverride(){
        var s=new Scenario();assertTrue(s.engine.overridePlaylist(s.automatic.id,OverrideMode.UNTIL_RELEASED,0,s.lib,Set.of()));s.tick(0);s.enqueue(s.b);
        s.end(1);s.tick(11);assertSame(s.b,s.engine.current());assertEquals(s.automatic.id,s.engine.overrideId());
        s.end(12);s.tick(22);assertSame(s.a,s.engine.current());assertEquals(s.automatic.id,s.engine.overrideId());
    }
    @Test void aNewHeldOverrideTakesPrecedenceWithoutDiscardingTheQueue(){
        var s=new Scenario();s.enqueue(s.b);assertTrue(s.engine.overridePlaylist(s.automatic.id,OverrideMode.UNTIL_RELEASED,0,s.lib,Set.of()));
        s.tick(0);assertSame(s.a,s.engine.current());assertTrue(s.engine.queueWaitingForOverride());s.end(1);s.tick(11);assertSame(s.a,s.engine.current());
        s.engine.useQueueNext();s.engine.nextSong(12,s.lib,context(false,false),Set.of());s.tick(12);assertSame(s.b,s.engine.current());
    }
    @Test void aNewOneSongOverrideCompletesBeforeResumingTheQueue(){
        var s=new Scenario();s.enqueue(s.b);s.engine.overridePlaylist(s.automatic.id,OverrideMode.ONE_SONG,0,s.lib,Set.of());s.tick(0);
        assertSame(s.a,s.engine.current());s.end(1);assertNull(s.engine.overrideId());s.tick(11);assertSame(s.b,s.engine.current());
    }
    @Test void playOnTriggerCannotRemoveTheNormalGapBeforeAQueuedSong(){
        var s=new Scenario();s.automatic.playOnTrigger=true;s.enqueue(s.b);s.tick(0);assertNull(s.engine.current());s.tick(9);assertNull(s.engine.current());s.tick(10);assertSame(s.b,s.engine.current());
    }
    @Test void clearingPendingQueueDoesNotInterruptTheCurrentQueuedSong(){
        var s=new Scenario();s.enqueue(s.b);s.enqueue(s.c);s.tick(0);s.tick(10);s.engine.queue().clear();s.tick(11);assertSame(s.b,s.engine.current());
        s.end(12);s.tick(22);assertSame(s.a,s.engine.current());
    }
    @Test void resetClearsRequestsButPreservesListeningHistory(){
        var s=new Scenario();s.start();s.enqueue(s.b);s.engine.reset();assertTrue(s.engine.queue().empty());assertEquals(1,s.engine.history().size());
    }
    @Test void deathGuardDoesNotConsumePendingQueueAndCombatQueueUsesDeathPolicy(){
        var s=new Scenario();s.requests.conditions.combat=PlaylistConditions.State.YES;s.enqueue(s.b);s.tick(0);
        s.engine.tick(10,s.lib,context(true,false),Set.of());assertNull(s.engine.current());assertEquals(1,s.engine.queue().size());
        s.tick(11);assertSame(s.b,s.engine.current());s.lib.settings.combatDeathMusic=DeathMusic.CUT;
        s.engine.tick(12,s.lib,context(true,false),Set.of());assertNull(s.engine.current());
    }
    @Test void jukeboxHoldPreservesQueueAndDoesNotRecordAResumeAsAnotherPlay(){
        var s=new Scenario();s.lib.settings.jukeboxMode=JukeboxMode.RESUME;s.enqueue(s.b);s.enqueue(s.c);s.tick(0);s.tick(10);
        s.engine.tick(11,s.lib,context(false,true),Set.of());assertTrue(s.engine.jukeboxHolding());assertEquals(1,s.engine.queue().size());
        s.engine.tick(30,s.lib,context(false,false),Set.of());assertSame(s.b,s.engine.current());assertEquals(1,s.engine.history().size());
    }
    @Test void historyRecordsSourceMetadataBeforePlaylistOrTrackEdits(){
        var s=new Scenario();s.start();var entry=s.engine.history().snapshot().getFirst();s.a.title="Renamed";s.automatic.name="Renamed playlist";
        assertEquals("A",entry.title());assertEquals("Automatic",entry.playlistName());assertTrue(entry.reason().startsWith("Automatic winner:"));
    }
    @Test void inspectorExplainsQueuePlaybackAndWaiting(){
        var s=new Scenario();s.enqueue(s.b);s.tick(0);assertTrue(PlaybackInspector.waiting(s.engine,s.lib,context(false,false),Set.of(),0).startsWith("Queued song waiting"));
        s.tick(10);assertTrue(PlaybackInspector.waiting(s.engine,s.lib,context(false,false),Set.of(),10).startsWith("Manual queued song"));
    }
    @Test void queueBypassesAutomaticTimeFittingAndDoesNotRequestDurations(){
        var s=new Scenario();s.automatic.conditions.timeEnabled=true;s.automatic.conditions.startTime=6000;s.automatic.conditions.endTime=6001;s.automatic.conditions.fitSongsToTimeWindow=true;
        s.b.durationSeconds=9999;s.enqueue(s.b);s.tick(0);s.tick(10);assertSame(s.b,s.engine.current());assertFalse(s.engine.waitingForTimeFit());assertNull(s.engine.takeDurationRequest());
    }
    @Test void pendingQueueSurvivesAJukeboxHoldAndWaitsAfterTheRecord(){
        var s=new Scenario();s.lib.settings.jukeboxMode=JukeboxMode.AFTER_SILENCE;s.enqueue(s.b);
        s.engine.tick(0,s.lib,context(false,true),Set.of());s.engine.tick(20,s.lib,context(false,true),Set.of());
        assertEquals(1,s.engine.queue().size());assertNull(s.engine.current());s.tick(21);assertEquals(10,s.engine.remainingSilence(21));s.tick(31);assertSame(s.b,s.engine.current());
    }
    @Test void returnAutomaticReleasesAnOverrideWithoutDiscardingRequests(){
        var s=new Scenario();s.enqueue(s.b);s.engine.overridePlaylist(s.automatic.id,OverrideMode.UNTIL_RELEASED,0,s.lib,Set.of());s.tick(0);
        s.engine.returnAutomatic();assertFalse(s.engine.queueWaitingForOverride());assertEquals(1,s.engine.queue().size());s.end(1);s.tick(11);assertSame(s.b,s.engine.current());
    }
    @Test void historyDistinguishesPreviewsOverridesAndQueuedStarts(){
        var s=new Scenario();s.engine.preview(s.c,0);assertEquals(Origin.PREVIEW,s.engine.history().snapshot().getFirst().origin());
        s.engine.overridePlaylist(s.automatic.id,OverrideMode.UNTIL_RELEASED,1,s.lib,Set.of());s.tick(1);
        assertEquals(Origin.OVERRIDE,s.engine.history().snapshot().getFirst().origin());s.enqueue(s.b);s.engine.nextSong(2,s.lib,context(false,false),Set.of());s.tick(2);
        var entry=s.engine.history().snapshot().getFirst();assertEquals(Origin.QUEUE,entry.origin());assertEquals(s.requests.name,entry.playlistName());assertTrue(entry.reason().contains("Queued manually"));
    }
    @Test void queueAndHistoryUseTheIndexWithoutWalkingSeventyThousandLibraryEntries(){
        var s=new Scenario();var tracks=new LibraryIndexTest.GuardedTracks();tracks.addAll(s.lib.tracks);
        for(int i=0;i<70000;i++)tracks.add(new Track("unused:"+i,"unused","Unused",false));
        s.lib.tracks=tracks;s.lib.rebuildTrackIndex();tracks.locked=true;s.enqueue(s.b);s.tick(0);s.tick(10);
        for(int i=0;i<2000;i++)s.tick(10+i/20.0);
        assertSame(s.b,s.engine.current());assertEquals(1,s.engine.history().size());assertEquals(s.b.id,s.engine.history().snapshot().getFirst().trackId());
    }
}

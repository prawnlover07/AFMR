package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackEngineTest {
    private final FakeAudio audio=new FakeAudio();
    private PlaybackEngine engine;
    private Library library;
    private final Context forest=new Context("minecraft:overworld","minecraft:forest",false,false,6000,false,15,"",0,64,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    private final Context desert=new Context("minecraft:overworld","minecraft:desert",false,false,6000,false,15,"",0,64,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    private Playlist general,forestPlaylist,desertPlaylist;
    private Track a,b,c;
    @BeforeEach void setup(){
        library=new Library();
        var combat=new Playlist("combat","Combat");combat.conditions.combat=PlaylistConditions.State.YES;library.playlists.add(combat);
        var water=new Playlist("underwater","Underwater");water.conditions.water=PlaylistConditions.State.YES;water.includeVanilla=false;library.playlists.add(water);
        Playlist night=new Playlist("night","Night");night.enabled=false;library.playlists.add(night);
        Playlist day=new Playlist("day","Day");day.enabled=false;library.playlists.add(day);
        library.playlists.add(new Playlist("general","General"));
        library.settings.silenceMinSeconds=10;library.settings.silenceMaxSeconds=10;
        library.settings.fadeInSeconds=library.settings.fadeOutSeconds=3;library.settings.bufferSeconds=2;
        general=library.playlist("general");general.includeVanilla=false;
        a=track("a");b=track("b");c=track("c");
        general.tracks.put(a.id,true);
        forestPlaylist=new Playlist("forest","Forest");forestPlaylist.conditions.biomeEnabled=true;forestPlaylist.conditions.biomes.add("minecraft:forest");forestPlaylist.includeVanilla=false;forestPlaylist.tracks.put(b.id,true);
        desertPlaylist=new Playlist("desert","Desert");desertPlaylist.conditions.biomeEnabled=true;desertPlaylist.conditions.biomes.add("minecraft:desert");desertPlaylist.includeVanilla=false;desertPlaylist.tracks.put(c.id,true);
        library.playlists.add(4,forestPlaylist);library.playlists.add(5,desertPlaylist);
        engine=new PlaybackEngine(audio,new Random(7));
    }
    private Track track(String id){Track t=new Track(id,id,id,false);library.tracks.add(t);return t;}
    private void tick(double time,Context context){engine.tick(time,library,context,Set.of());}
    private void startForest(){tick(0,forest);tick(10,forest);tick(13,forest);assertEquals(b,audio.current);}

    @Test void startsAfterConfiguredSilence(){
        tick(0,forest);tick(9,forest);assertNull(audio.current);tick(10,forest);assertEquals(b,audio.current);
    }
    @Test void biomeTransitionBuffersFadesAndWaitsFullSilence(){
        startForest();tick(14,desert);tick(15,desert);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
        tick(16,desert);assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
        tick(17.5,desert);assertEquals(0.5,audio.gain,0.001);
        tick(19,desert);assertEquals(PlaybackEngine.Phase.SILENCE,engine.phase());assertNull(audio.current);
        tick(28.9,desert);assertNull(audio.current);tick(29,desert);assertEquals(c,audio.current);
    }
    @Test void briefBoundaryCrossingsDoNotInterrupt(){
        startForest();tick(14,desert);tick(15,forest);tick(19,forest);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());assertEquals(b,audio.current);
    }
    @Test void finishToggleKeepsSongThenNormalSilence(){
        startForest();desertPlaylist.finishCurrentSong=true;tick(14,desert);tick(16,desert);
        assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());assertEquals(b,audio.current);
        audio.current=null;tick(30,desert);tick(39.9,desert);assertNull(audio.current);tick(40,desert);assertEquals(c,audio.current);
    }
    @Test void rechecksCurrentConditionDuringSilence(){
        startForest();tick(14,desert);tick(16,desert);tick(19,desert);
        tick(20,forest);tick(29,forest);assertEquals(b,audio.current);
    }
    @Test void priorityAndCombatToggle(){
        Playlist combat=library.playlist("combat");combat.includeVanilla=false;combat.tracks.put(a.id,true);
        Context busy=new Context("minecraft:overworld","minecraft:forest",true,true,18000,false,15,"",0,64,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
        assertEquals(combat,library.selectPlayable(busy,Set.of(),Set.of()));
        library.settings.combatEnabled=false;
        assertEquals(forestPlaylist,library.selectPlayable(busy,Set.of(),Set.of()));
    }
    @Test void emptyContextPlaylistsFallThroughToGeneral(){
        forestPlaylist.tracks.clear();assertEquals(general,library.selectPlayable(forest,Set.of(),Set.of()));
    }
    @Test void disabledSongStaysAssignedAndDoesNotPlay(){
        forestPlaylist.tracks.put(b.id,false);assertEquals(Boolean.FALSE,forestPlaylist.tracks.get(b.id));
        tick(0,forest);tick(10,forest);assertEquals(a,audio.current);
    }
    @Test void vanillaUsesCurrentGamePoolAndIndividualOverrides(){
        Track vanilla=track("vanilla:test");vanilla.vanilla=true;
        general.includeVanilla=true;
        assertTrue(library.pool(general,Set.of(vanilla.id),false).contains(vanilla));
        assertFalse(library.pool(general,Set.of(),false).contains(vanilla));
        general.tracks.put(vanilla.id,false);assertFalse(library.pool(general,Set.of(vanilla.id),false).contains(vanilla));
        general.tracks.put(vanilla.id,true);assertTrue(library.pool(general,Set.of(),false).contains(vanilla));
    }
    @Test void badTrackDoesNotLoopOrBlockFallback(){
        audio.fail=b.id;tick(0,forest);tick(10,forest);assertNull(audio.current);assertFalse(engine.error().isBlank());
        tick(11,forest);tick(13,forest);assertEquals(a,audio.current);
    }
    @Test void asyncDecoderFailureQuarantinesSong(){
        startForest();engine.failed(b.id,"decoder failed",14,library);tick(15,forest);tick(17,forest);
        assertEquals(a,audio.current);
    }
    @Test void skipRetainsNormalSilence(){
        startForest();engine.skip(14,library);tick(23.9,forest);assertNull(audio.current);tick(24,forest);assertEquals(b,audio.current);
    }
    @Test void resetStopsAudioAndClearsWorldState(){
        startForest();engine.reset();assertNull(audio.current);assertNull(engine.current());assertEquals(PlaybackEngine.Phase.SILENCE,engine.phase());
    }
    @Test void invalidSettingsCannotBreakTimer(){
        library.settings.silenceMinSeconds=Double.NaN;library.settings.silenceMaxSeconds=-20;library.settings.fadeInSeconds=library.settings.fadeOutSeconds=Double.POSITIVE_INFINITY;
        library.normalize();assertTrue(Double.isFinite(library.settings.silenceMinSeconds));
        assertTrue(library.settings.silenceMaxSeconds>=library.settings.silenceMinSeconds);assertEquals(0,library.settings.fadeInSeconds);
    }
    @Test void dayNightAndDimensionRules(){
        var night=library.playlist("night");night.enabled=true;night.tracks.put(a.id,true);
        Context context=new Context("minecraft:the_nether","minecraft:nether_wastes",false,false,18000,false,15,"",0,64,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
        assertEquals(night,library.select(context));night.enabled=false;
        var dimension=new Playlist("nether","Nether");dimension.conditions.dimensionEnabled=true;dimension.conditions.dimension="minecraft:the_nether";dimension.tracks.put(c.id,true);
        library.playlists.add(0,dimension);assertEquals(dimension,library.select(context));
    }
    @Test void rewindRestartsAfterThreeSecondsWithoutRecording(){
        engine.preview(a,0,false);b.durationSeconds=120;engine.preview(b,1,false);
        var history=engine.history().snapshot();long sequence=engine.playbackSequence();
        assertTrue(engine.rewind(4.01,library));assertSame(b,engine.current());assertEquals(0,engine.trackElapsed(4.01));
        tick(4.01,forest);assertEquals(0,audio.position);assertEquals(sequence,engine.playbackSequence());
        assertEquals(history,engine.history().snapshot());
        assertTrue(engine.rewind(4.02,library));assertSame(a,engine.current());
    }
    @Test void rewindAtExactlyThreeSecondsPlaysPreviousHistoryTrack(){
        engine.preview(a,0,false);engine.preview(b,1,false);var history=engine.history().snapshot();
        assertTrue(engine.rewind(4,library));assertSame(a,engine.current());assertTrue(engine.previewing());
        assertEquals(history,engine.history().snapshot());
    }
    @Test void repeatedRewindWalksThreeDistinctEntriesWithoutPingPong(){
        var d=track("d");engine.preview(a,0,false);engine.preview(b,1,false);engine.preview(c,2,false);engine.preview(d,3,false);
        var history=engine.history().snapshot();
        assertTrue(engine.rewind(3.1,library));assertSame(c,engine.current());
        assertTrue(engine.rewind(3.2,library));assertSame(b,engine.current());
        assertTrue(engine.rewind(3.3,library));assertSame(a,engine.current());
        assertFalse(engine.rewind(3.4,library));assertSame(a,engine.current());assertEquals(history,engine.history().snapshot());
    }
    @Test void normalNextStartResetsRewindWalk(){
        engine.preview(a,0,false);engine.preview(b,1,false);engine.preview(c,2,false);
        assertTrue(engine.rewind(2.1,library));assertSame(b,engine.current());
        assertTrue(engine.rewind(2.2,library));assertSame(a,engine.current());
        assertTrue(engine.nextSong(3,library,forest,Set.of()));tick(3,forest);assertSame(b,engine.current());
        assertEquals(4,engine.history().size());assertTrue(engine.rewind(3.1,library));assertSame(c,engine.current());
    }
    @Test void normalPreviewResetsRewindWalk(){
        engine.preview(a,0,false);engine.preview(b,1,false);engine.preview(c,2,false);
        assertTrue(engine.rewind(2.1,library));engine.preview(a,3,false);
        assertTrue(engine.rewind(3.1,library));assertSame(c,engine.current());assertEquals(4,engine.history().size());
    }
    @Test void rewindSkipsMissingAndFailedHistoryTracks(){
        engine.preview(a,0,false);engine.preview(b,1,false);engine.failed(b.id,"bad audio",1,library);
        var missing=track("missing");engine.preview(missing,2,false);engine.preview(c,3,false);
        library.tracks.remove(missing);library.rebuildTrackIndex();var history=engine.history().snapshot();
        assertTrue(engine.rewind(3.1,library));assertSame(a,engine.current());assertEquals(history,engine.history().snapshot());
    }
    @Test void rewindPreservesOneSongOverrideAndQueueThroughReplayEnd(){
        engine.preview(a,0,false);library.settings.fadeOutSeconds=0;
        assertTrue(engine.overridePlaylist(forestPlaylist.id,OverrideMode.ONE_SONG,1,library,Set.of()));
        tick(1,forest);assertSame(b,engine.current());assertTrue(engine.enqueue(c.id,"",library));
        var queue=engine.queue().snapshot();var history=engine.history().snapshot();
        assertTrue(engine.rewind(2,library));assertSame(a,engine.current());tick(3,desert);
        assertEquals(forestPlaylist.id,engine.overrideId());assertEquals(OverrideMode.ONE_SONG,engine.overrideMode());
        assertEquals(queue,engine.queue().snapshot());assertEquals(history,engine.history().snapshot());
        audio.current=null;tick(4,desert);assertEquals(forestPlaylist.id,engine.overrideId());
        tick(14,desert);assertSame(c,engine.current());assertEquals(forestPlaylist.id,engine.overrideId());
        audio.current=null;tick(15,desert);tick(25,desert);assertSame(b,engine.current());
    }
    @Test void rewindWithNoEarlierHistoryDoesNotStopAnything(){
        assertFalse(engine.rewind(0,library));engine.preview(a,1,false);int stops=audio.stops,starts=audio.starts;
        assertFalse(engine.rewind(2,library));assertSame(a,audio.current);
        assertEquals(stops,audio.stops);assertEquals(starts,audio.starts);assertEquals(1,engine.history().size());
    }
    @Test void rewindDuringSilenceStartsNewestHistoryEntry(){
        engine.preview(a,0,false);engine.preview(b,1,false);engine.skip(2,library);
        assertTrue(engine.rewind(3,library));assertSame(b,engine.current());
        assertTrue(engine.rewind(3.1,library));assertSame(a,engine.current());
    }
    @Test void rewindRestartsUnknownDurationWithoutRecording(){
        engine.preview(a,0,false);b.durationSeconds=Double.NaN;engine.preview(b,1,false);
        var history=engine.history().snapshot();int starts=audio.starts;
        assertTrue(engine.rewind(5,library));assertSame(b,audio.current);assertEquals(starts+1,audio.starts);
        assertEquals(0,audio.position);assertEquals(0,engine.trackPlayed(5));assertEquals(history,engine.history().snapshot());
        assertTrue(engine.rewind(5.1,library));assertSame(a,engine.current());
    }
    @Test void rewindThresholdUsesSongPosition(){
        engine.preview(a,0,false);b.durationSeconds=120;engine.preview(b,1,false);
        assertTrue(engine.seek(90,2));tick(2,forest);assertTrue(engine.rewind(3,library));assertSame(b,engine.current());
        tick(3,forest);assertEquals(0,audio.position);assertTrue(engine.rewind(3.5,library));assertSame(a,engine.current());
    }
    @Test void pausedRewindKeepsPauseAndDoesNotCountPausedTime(){
        engine.preview(a,0,false);engine.preview(b,1,false);engine.setPaused(true,2);
        assertTrue(engine.rewind(100,library));assertSame(a,engine.current());assertTrue(engine.paused());assertTrue(audio.paused);
        assertEquals(0,engine.trackPlayed(200));
    }
    @Test void pausedKnownLengthRestartKeepsPause(){
        engine.preview(a,0,false);b.durationSeconds=120;engine.preview(b,1,false);engine.setPaused(true,5);
        assertTrue(engine.rewind(100,library));tick(100,forest);assertSame(b,engine.current());
        assertTrue(engine.paused());assertTrue(audio.paused);assertEquals(0,audio.position);assertEquals(0,engine.trackElapsed(200));
        assertTrue(engine.rewind(101,library));assertSame(a,engine.current());assertTrue(audio.paused);
    }
    @Test void historyMutationsAndResetInvalidateRewindCursor(){
        engine.preview(a,0,false);engine.preview(b,1,false);engine.preview(c,2,false);
        var saved=engine.history().snapshot();assertTrue(engine.rewind(2.1,library));assertSame(b,engine.current());
        engine.history().restore(saved);assertTrue(engine.rewind(2.2,library));assertSame(c,engine.current());
        engine.history().relink(Map.of(c.id,a.id));assertTrue(engine.rewind(2.3,library));assertSame(a,engine.current());
        engine.history().clear();assertFalse(engine.rewind(2.4,library));assertSame(a,engine.current());
        engine.history().restore(saved);engine.reset();assertTrue(engine.rewind(3,library));assertSame(c,engine.current());
    }
    @Test void rewoundSongFinishesBeforeNormalSchedulingResumes(){
        engine.preview(a,0,false);engine.preview(b,1,false);assertTrue(engine.rewind(2,library));
        tick(3,desert);tick(6,desert);assertSame(a,engine.current());assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
        audio.current=null;tick(7,desert);assertNull(engine.current());tick(17,desert);assertSame(c,engine.current());
        assertEquals(3,engine.history().size());
    }
    static class FakeAudio implements PlaybackEngine.Audio{
        Track current;float gain;String fail;boolean paused;double position;int starts,stops;
        public void start(Track t,double position)throws Exception{if(t.id.equals(fail))throw new Exception("bad audio");current=t;this.position=position;starts++;}
        public boolean started(){return playing();}
        public boolean playing(){return current!=null;}
        public void gain(float value){gain=value;}
        public void stop(){current=null;stops++;}
        public void setPaused(boolean value){paused=value;}
    }
}

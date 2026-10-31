package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackSeekTest {
    static final class Audio extends PlaybackEngineTest.FakeAudio {
        double offset;int starts;boolean ready=true;
        @Override public void start(Track track,double position)throws Exception{super.start(track,position);offset=position;starts++;}
        @Override public boolean started(){return ready&&playing();}
    }
    private final Audio audio=new Audio();
    private final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
    private final Track track=new Track("a","fixture.wav","Song",false);
    private final Library lib=Library.defaults();
    private final Context context=new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"world",0,70,0,0,.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,NearbyPlayers.EMPTY,false);
    private void tick(double now){engine.tick(now,lib,context,java.util.Set.of());}
    private void start(){track.durationSeconds=120;engine.preview(track,10,false);}
    @Test void seekRetainsPlaybackIdentityAndDoesNotReplayNotifications(){
        start();long sequence=engine.playbackSequence();
        assertTrue(engine.seek(80,15));assertSame(track,engine.current());assertTrue(engine.previewing());
        tick(15);assertEquals(sequence,engine.playbackSequence());assertEquals(80,audio.offset);assertEquals(80,engine.trackElapsed(15));
        assertEquals(5,engine.trackPlayed(15));assertEquals(8,engine.trackPlayed(18));
        assertTrue(engine.seek(3,18));assertEquals(8,engine.trackPlayed(18));assertEquals(3,engine.trackElapsed(18));
    }
    @Test void seekWhilePausedKeepsPauseAndFreezesBothClocks(){
        start();engine.setPaused(true,15);assertTrue(engine.seek(60,100));tick(100);
        assertTrue(engine.paused());assertTrue(audio.paused);assertEquals(60,engine.trackElapsed(120));assertEquals(5,engine.trackPlayed(120));
        engine.setPaused(false,125);assertEquals(61,engine.trackElapsed(126));assertEquals(6,engine.trackPlayed(126));
    }
    @Test void pendingDecoderPublishesTargetWithoutCountingLoadingAsListening(){
        start();audio.ready=false;assertTrue(engine.seek(50,15));assertEquals(50,engine.trackElapsed(30));assertEquals(5,engine.trackPlayed(30));
    }
    @Test void unknownLengthsAndInvalidRequestsLeaveAudioUntouched(){
        assertFalse(engine.seek(1,0));start();int starts=audio.starts;
        assertFalse(engine.seek(Double.NaN,11));assertFalse(engine.seek(Double.POSITIVE_INFINITY,11));
        track.durationSeconds=0;assertFalse(engine.seek(1,11));assertEquals(starts,audio.starts);
    }
    @Test void repeatedRequestsInOneTickOpenOnlyTheLatestTarget(){
        start();int starts=audio.starts;for(int i=0;i<100;i++)assertTrue(engine.seek(i,15));
        assertEquals(starts,audio.starts);tick(15);assertEquals(starts+1,audio.starts);assertEquals(99,audio.offset);
    }
    @Test void seekingAQueuedSongKeepsItsSourceAndTheRestOfTheQueue(){
        track.durationSeconds=120;var source=lib.createPlaylist("Manual queue");source.manualOnly=true;source.tracks.put(track.id,true);lib.tracks.add(track);lib.rebuildTrackIndex();
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;engine.enqueue(track.id,source.id,lib);tick(0);tick(1);
        assertTrue(engine.queuedPlaying());engine.enqueue(track.id,source.id,lib);var history=engine.history().snapshot();long sequence=engine.playbackSequence();
        assertTrue(engine.seek(20,2));tick(2);assertTrue(engine.queuedPlaying());assertEquals(source.id,engine.sourcePlaylistId());assertEquals(1,engine.queue().size());assertEquals(history,engine.history().snapshot());assertEquals(sequence,engine.playbackSequence());
    }
    @Test void seekClampsToPlayableBounds(){
        start();assertTrue(engine.seek(-100,11));tick(11);assertEquals(0,audio.offset);
        assertTrue(engine.seek(999,12));tick(12);assertEquals(119.95,audio.offset,0.001);
    }
}

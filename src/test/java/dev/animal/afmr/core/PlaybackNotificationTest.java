package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackNotificationTest {
    private final Library library=Library.defaults();
    private final Track track=new Track("a","file","A",false);
    private final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
    private final PlaybackEngine engine=new PlaybackEngine(audio,new Random(7));
    private final Context outside=new Context("minecraft:overworld","minecraft:forest",false,false,6000,false,15,"world",0,80,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);

    private void tick(double now){engine.tick(now,library,outside,Set.of());}
    private void prepare(){
        library.tracks.add(track);library.playlist("day_outside").tracks.put(track.id,true);
        library.settings.silenceMinSeconds=library.settings.silenceMaxSeconds=5;
        tick(0);
    }

    @Test void eachStartNotifiesEvenWhenTheSameTrackIsChosenAgain(){
        prepare();tick(5);long first=engine.playbackSequence();assertTrue(first>0);
        tick(6);engine.setPaused(true,6);tick(20);assertEquals(first,engine.playbackSequence());
        engine.setPaused(false,20);engine.nextSong(21,library,outside,Set.of());tick(21);
        assertSame(track,engine.current());assertEquals(first+1,engine.playbackSequence());
        engine.preview(track,22,false);assertEquals(first+2,engine.playbackSequence());
        engine.reset();engine.preview(track,23,false);assertEquals(first+3,engine.playbackSequence());
    }

    @Test void failedStartDoesNotNotify(){
        var broken=new PlaybackEngine(new PlaybackEngine.Audio(){
            public void start(Track track,double position)throws Exception{throw new Exception("fixture failure");}
            public boolean started(){return playing();}
        public boolean playing(){return false;}
            public void gain(float gain){}
            public void stop(){}
            public void setPaused(boolean paused){}
        },new Random(1));
        broken.preview(track,0,false);assertNull(broken.current());assertEquals(0,broken.playbackSequence());
    }

    @Test void frequencyChangeRestartsPendingGapAndKeepsAPlayingSong(){
        prepare();library.settings.silenceMinSeconds=library.settings.silenceMaxSeconds=10;
        engine.rescheduleSilence(3,library);assertEquals(10,engine.remainingSilence(3));
        tick(12);assertNull(engine.current());tick(13);assertSame(track,engine.current());
        long sequence=engine.playbackSequence();library.settings.silenceMinSeconds=library.settings.silenceMaxSeconds=5;
        engine.rescheduleSilence(14,library);tick(15);
        assertSame(track,engine.current());assertEquals(sequence,engine.playbackSequence());
        audio.current=null;tick(16);assertEquals(5,engine.remainingSilence(16));
    }

    @Test void changingFrequencyDuringManualPausePreservesTheNewGapOnResume(){
        prepare();engine.setPaused(true,2);
        library.settings.silenceMinSeconds=library.settings.silenceMaxSeconds=10;
        engine.rescheduleSilence(100,library);assertEquals(10,engine.remainingSilence(100));
        engine.setPaused(false,102);assertEquals(10,engine.remainingSilence(102));
        tick(111);assertNull(engine.current());tick(112);assertSame(track,engine.current());
    }
}

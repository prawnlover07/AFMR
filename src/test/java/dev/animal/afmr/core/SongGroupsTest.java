package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SongGroupsTest {
    private static final Context CONTEXT=new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,
        "test",0,70,0,0,.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,NearbyPlayers.EMPTY,false);
    private static class Scenario {
        final Library lib=new Library();
        final Playlist a=new Playlist("a","A"),b=new Playlist("b","B");
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(7));
        Scenario(){
            lib.settings.bufferSeconds=lib.settings.fadeOutSeconds=0;
            lib.settings.songsBeforeSilence=3;lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;
            for(var p:List.of(a,b)){
                p.includeVanilla=false;lib.playlists.add(p);
                for(int i=0;i<3;i++){var t=new Track(p.id+i,"generated.wav",p.name+i,false);lib.tracks.add(t);p.tracks.put(t.id,true);}
            }
            b.enabled=false;
        }
        void tick(double now){engine.tick(now,lib,CONTEXT,Set.of());}
        void start(){tick(0);engine.playNow(0);tick(0);assertNotNull(engine.current());}
        void end(double now){audio.current=null;tick(now);}
    }
    @Test void threeNaturalSongsThenOneFullGap(){
        var s=new Scenario();s.start();var heard=new HashSet<String>();heard.add(s.engine.current().id);
        s.end(2);assertEquals(2,s.engine.groupStarted());heard.add(s.engine.current().id);
        s.end(4);assertEquals(3,s.engine.groupStarted());heard.add(s.engine.current().id);assertEquals(3,heard.size());
        s.end(6);assertNull(s.engine.current());assertEquals(10,s.engine.remainingSilence(6));
        s.tick(16);assertNotNull(s.engine.current());assertEquals(1,s.engine.groupStarted());
    }
    @Test void groupSettingDoesNotBypassInitialSilence(){var s=new Scenario();s.tick(0);assertNull(s.engine.current());assertEquals(10,s.engine.remainingSilence(0));}
    @Test void playlistOverrideCanUseOneSongDespiteGlobalThree(){var s=new Scenario();s.a.customSongCount=true;s.a.songsBeforeSilence=1;s.start();s.end(2);assertNull(s.engine.current());assertEquals(10,s.engine.remainingSilence(2));}
    @Test void changingSelectionResetsEvenWhenReturning(){
        var s=new Scenario();s.start();s.end(1);assertEquals(2,s.engine.groupStarted());
        s.a.enabled=false;s.b.enabled=true;s.tick(2);s.tick(12);assertEquals("b",s.engine.sourcePlaylistId());assertEquals(1,s.engine.groupStarted());
        s.a.enabled=true;s.b.enabled=false;s.tick(13);s.tick(23);assertEquals("a",s.engine.sourcePlaylistId());assertEquals(1,s.engine.groupStarted());
    }
    @Test void retainedOutgoingSongDoesNotCountForIncomingGroup(){
        var s=new Scenario();s.a.finishCurrentSongOnExit=true;s.start();s.a.enabled=false;s.b.enabled=true;s.tick(1);
        assertEquals("a",s.engine.sourcePlaylistId());s.end(2);assertNull(s.engine.current());s.tick(12);
        assertEquals("b",s.engine.sourcePlaylistId());assertEquals(1,s.engine.groupStarted());
    }
    @Test void skipKeepsTheNormalGapAndResetsGroup(){var s=new Scenario();s.start();s.engine.skip(1,s.lib);assertEquals(10,s.engine.remainingSilence(1));s.tick(11);assertEquals(1,s.engine.groupStarted());}
    @Test void nextBypassesGapButBeginsANewGroup(){var s=new Scenario();s.start();s.end(1);s.engine.nextSong(2,s.lib,CONTEXT,Set.of());s.tick(2);assertEquals(1,s.engine.groupStarted());}
    @Test void previewDoesNotCountAndKeepsItsNormalGap(){var s=new Scenario();s.start();s.engine.preview(s.lib.tracks.get(0),1);s.end(2);assertNull(s.engine.current());assertEquals(10,s.engine.remainingSilence(2));}
    @Test void manualQueueRetainsNormalSilence(){
        var s=new Scenario();s.start();assertTrue(s.engine.enqueue("a1","a",s.lib));s.end(1);assertNull(s.engine.current());assertEquals(10,s.engine.remainingSilence(1));
        s.tick(11);assertTrue(s.engine.queuedPlaying());s.end(12);assertNull(s.engine.current());assertEquals(10,s.engine.remainingSilence(12));
    }
    @Test void heldOverrideGroupsButOneSongOverrideStillEndsAfterOne(){
        var s=new Scenario();s.engine.overridePlaylist("b",OverrideMode.UNTIL_RELEASED,0,s.lib,Set.of());s.tick(0);s.end(1);assertEquals(2,s.engine.groupStarted());
        s.engine.overridePlaylist("b",OverrideMode.ONE_SONG,2,s.lib,Set.of());s.tick(2);s.end(3);assertNull(s.engine.overrideId());assertNull(s.engine.current());
    }
    @Test void failureDoesNotTriggerConsecutivePlayback(){var s=new Scenario();s.start();s.engine.failed(s.engine.current().id,"test error",1,s.lib);assertNull(s.engine.current());assertEquals(1,s.engine.remainingSilence(1));assertEquals(0,s.engine.groupStarted());}
    @Test void pausedPlaybackTimeDoesNotAdvanceNotificationDelay(){
        var s=new Scenario();s.start();s.engine.setPaused(true,1);assertEquals(1,s.engine.trackElapsed(20));s.engine.setPaused(false,21);assertEquals(3,s.engine.trackElapsed(23));
    }
    @Test void decodingTimeDoesNotAdvancePlaybackClock(){
        var s=new Scenario();var audio=new PlaybackEngineTest.FakeAudio(){boolean ready;@Override public boolean started(){return ready;}};
        var engine=new PlaybackEngine(audio,new Random(1));engine.tick(0,s.lib,CONTEXT,Set.of());engine.playNow(0);engine.tick(0,s.lib,CONTEXT,Set.of());
        assertEquals(0,engine.trackElapsed(10));audio.ready=true;engine.tick(10,s.lib,CONTEXT,Set.of());assertEquals(3,engine.trackElapsed(13));
    }
    @Test void interruptedPlaylistCannotRepeatItsLastDrawAtCycleBoundary(){
        var pool=List.of(new Track("x","","X",false),new Track("y","","Y",false));var bag=new ShuffleBag();var random=new Random(3);String last=null;
        for(int i=0;i<200;i++){var next=bag.next(pool,"unrelated-other-playlist-song",random);assertNotEquals(last,next.id);last=next.id;}
    }
    @Test void returningPreservesTheUnspentShuffleOrder(){
        var pool=List.of(new Track("x","","X",false),new Track("y","","Y",false),new Track("z","","Z",false));
        var a=new ShuffleBag();var reference=new ShuffleBag();var ra=new Random(4);var rb=new Random(4);
        assertEquals(reference.next(pool,null,rb).id,a.next(pool,null,ra).id);
        for(int i=0;i<2;i++)assertEquals(reference.next(pool,"elsewhere",rb).id,a.next(pool,"elsewhere",ra).id);
    }
}

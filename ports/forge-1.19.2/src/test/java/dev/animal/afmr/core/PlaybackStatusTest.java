package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackStatusTest {
    private final Library library=new Library();
    private final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
    private final PlaybackEngine engine=new PlaybackEngine(audio,new Random(7));
    private final Context context=new Context("minecraft:overworld","minecraft:forest",false,false,6000,false,15,"world",0,80,0,0,.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,NearbyPlayers.EMPTY,false);
    private String label(double now){return PlaybackStatus.label(engine,library.settings,true,false,"",now);}
    private Playlist prepare(double silence,double buffer){
        var p=new Playlist("one","One");p.includeVanilla=false;library.playlists.add(p);var track=new Track("song","generated.wav","Song",false);library.tracks.add(track);p.tracks.put(track.id,true);
        library.settings.silenceMinSeconds=library.settings.silenceMaxSeconds=silence;library.settings.bufferSeconds=buffer;library.settings.fadeInSeconds=0;engine.tick(0,library,context,Set.of());return p;
    }
    @Test void countdownUsesTheLongerOfTheSilentGapAndContextBuffer(){prepare(10,20);assertEquals("Next song in 0:20",label(0));assertEquals("Next song in 0:01",label(19.1));}
    @Test void countdownRoundsUpAndDoesNotExposeNegativeOrInvalidTimes(){assertEquals("2:14",PlaybackStatus.countdown(133.1));assertEquals("0:01",PlaybackStatus.countdown(.01));assertEquals("0:00",PlaybackStatus.countdown(-1));assertEquals("0:00",PlaybackStatus.countdown(Double.NaN));}
    @Test void unavailableReasonsTakePrecedenceOverAnUnrelatedGap(){engine.tick(0,library,context,Set.of());assertEquals("No matching playlist",PlaybackStatus.label(engine,library.settings,true,false,"No matching playlist",0));assertEquals("No enabled songs",PlaybackStatus.label(engine,library.settings,true,false,"No enabled songs",0));}
    @Test void pauseAndWorldStateNeverPretendThatADeadlineIsAdvancing(){prepare(10,0);engine.setPaused(true,0);assertEquals("Paused",label(5));assertEquals("Join a world to play music",PlaybackStatus.label(engine,library.settings,false,false,"",5));}
    @Test void respawnAndErrorsRemainExplicit(){prepare(0,0);engine.failed("song","fixture error",0,library);assertEquals("Playback error",label(0));assertEquals("Waiting for respawn",PlaybackStatus.label(engine,library.settings,true,true,"",0));}
    @Test void playingAndSourceRemainDistinctFromTheSelectedPlaylist(){var first=prepare(0,0);first.finishCurrentSongOnExit=true;var other=new Playlist("two","Two");other.includeVanilla=false;other.tracks.put("song",true);library.playlists.add(0,other);engine.tick(1,library,context,Set.of());assertEquals("Playing",label(1));assertEquals("one",engine.sourcePlaylistId());assertEquals(other,engine.selected());}
}

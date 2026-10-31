package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.State;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistVolumeTest {
    private static Context context(boolean incoming,boolean record,boolean dead){
        return new Context("minecraft:overworld",incoming?"test:b":"test:a",true,false,6000,false,15,"world",0,70,0,0,0.8,dead,
            StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,record,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    }
    private static final class Scenario {
        final Library lib=new Library();
        final Playlist a=lib.createPlaylist("A"),b=lib.createPlaylist("B");
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        Scenario(){
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=lib.settings.bufferSeconds=lib.settings.fadeOutSeconds=0;
            a.volume=0.4;b.volume=0.8;b.conditions.biomeEnabled=true;b.conditions.biomes.add("test:b");lib.movePlaylist(b.id,0);
            for(var p:List.of(a,b)){var track=new Track(p.id,"file",p.name,false);lib.tracks.add(track);p.tracks.put(track.id,true);}
        }
        void tick(double now,boolean incoming){engine.tick(now,lib,context(incoming,false,false),Set.of());}
        void start(){tick(0,false);tick(1,false);assertEquals(0.4,audio.gain,0.001);}
    }
    @Test void normalVolumeAndLiveAdjustmentDoNotRestartTheSong(){
        var s=new Scenario();s.start();long seq=s.engine.playbackSequence();
        s.a.volume=0.15;s.tick(2,false);assertEquals(0.15,s.audio.gain,0.001);assertEquals(seq,s.engine.playbackSequence());
    }
    @Test void zeroVolumeIsSilentButStillPlaysAndDoesNotFallThrough(){
        var s=new Scenario();s.a.volume=0;s.tick(0,false);s.tick(1,false);
        assertEquals(s.a.id,s.engine.sourcePlaylistId());assertNotNull(s.engine.current());assertEquals(0,s.audio.gain);
    }
    @Test void fadeEnvelopeMultipliesVolumeExactlyOnce(){
        var s=new Scenario();s.lib.settings.fadeInSeconds=4;s.lib.settings.fadeOutSeconds=2;
        s.tick(0,false);s.tick(2,false);assertEquals(0.2,s.audio.gain,0.001);
        s.tick(4,false);assertEquals(0.4,s.audio.gain,0.001);
        s.tick(5,true);s.tick(6,true);assertEquals(0.2,s.audio.gain,0.001);
        s.tick(7,true);s.tick(9,true);assertEquals(0.4,s.audio.gain,0.001);
        s.tick(11,true);assertEquals(0.8,s.audio.gain,0.001);
    }
    @Test void finishProtectionKeepsOutgoingVolumeAcrossContextChanges(){
        var s=new Scenario();s.a.finishCurrentSongOnExit=true;s.start();s.tick(2,true);
        assertEquals(s.a.id,s.engine.sourcePlaylistId());assertEquals(0.4,s.audio.gain,0.001);
        s.b.volume=0.1;s.tick(3,true);assertEquals(0.4,s.audio.gain,0.001);
        s.a.volume=0.3;s.tick(4,true);assertEquals(0.3,s.audio.gain,0.001);
    }
    @Test void continueOnMatchKeepsSourceVolume(){
        var s=new Scenario();s.b.tracks.put(s.a.id,true);s.lib.settings.continuePlayingOnMatch=true;
        s.start();s.tick(2,true);assertEquals(0.4,s.audio.gain,0.001);assertEquals(s.a.id,s.engine.sourcePlaylistId());
    }
    @Test void manualOverrideUsesSourceVolumeEvenWithAutomaticOff(){
        var s=new Scenario();s.b.enabled=false;s.start();
        assertTrue(s.engine.overridePlaylist(s.b.id,OverrideMode.UNTIL_RELEASED,2,s.lib,Set.of()));
        s.tick(2,false);s.tick(3,false);assertEquals(0.8,s.audio.gain,0.001);
    }
    @Test void queueUsesSourceVolumeAndUnownedQueueUsesNormalVolume(){
        var s=new Scenario();assertTrue(s.engine.enqueue(s.b.id,s.b.id,s.lib));s.tick(0,false);s.tick(1,false);assertEquals(0.8,s.audio.gain,0.001);
        assertTrue(s.engine.enqueue(s.a.id,"",s.lib));s.engine.nextSong(2,s.lib,context(false,false,false),Set.of());
        s.tick(2,false);s.tick(3,false);assertEquals(1,s.audio.gain,0.001);
    }
    @Test void previewDoesNotInheritPreviousPlaylistVolume(){
        var s=new Scenario();s.start();s.engine.preview(s.lib.track(s.b.id),2,false);s.tick(3,false);assertEquals(1,s.audio.gain,0.001);
    }
    @Test void jukeboxResumeUsesTheSameVolumeWithoutSquaringIt(){
        var s=new Scenario();s.start();s.lib.settings.jukeboxMode=JukeboxMode.RESUME;s.lib.settings.fadeOutSeconds=2;s.lib.settings.fadeInSeconds=4;
        s.engine.tick(2,s.lib,context(false,true,false),Set.of());s.engine.tick(3,s.lib,context(false,true,false),Set.of());assertEquals(0.2,s.audio.gain,0.001);
        s.engine.tick(4,s.lib,context(false,true,false),Set.of());assertEquals(0,s.audio.gain);
        s.tick(10,false);s.tick(12,false);assertEquals(0.2,s.audio.gain,0.001);s.tick(14,false);assertEquals(0.4,s.audio.gain,0.001);
    }
    @Test void deathFadeUsesPlaylistVolume(){
        var s=new Scenario();s.a.conditions.combat=State.YES;s.lib.settings.combatDeathFadeSeconds=2;s.start();
        s.engine.tick(2,s.lib,context(false,false,true),Set.of());s.engine.tick(3,s.lib,context(false,false,true),Set.of());
        assertEquals(0.2,s.audio.gain,0.001);s.engine.tick(4,s.lib,context(false,false,true),Set.of());assertNull(s.engine.current());
    }
    @Test void pauseRetainsVolumeUntilPlaybackResumes(){
        var s=new Scenario();s.start();s.engine.setPaused(true,2);s.a.volume=0.2;s.tick(50,false);
        assertEquals(0.4,s.audio.gain,0.001);s.engine.setPaused(false,51);s.tick(52,false);assertEquals(0.2,s.audio.gain,0.001);
    }
    @Test void defaultsBoundsClonesAndSnapshotsOwnVolume(){
        var l=Library.defaults();var p=l.playlist("day_outside");assertEquals(1,p.volume);
        p.volume=0.37;var clone=l.clonePlaylist(p.id,"Copy");var saved=l.snapshot();p.volume=0.6;
        assertEquals(0.37,clone.volume);assertEquals(0.37,saved.playlist(p.id).volume);
        p.volume=Double.NaN;l.normalize();assertEquals(1,p.volume);p.volume=-1;l.normalize();assertEquals(0,p.volume);p.volume=3;l.normalize();assertEquals(1,p.volume);
    }
}

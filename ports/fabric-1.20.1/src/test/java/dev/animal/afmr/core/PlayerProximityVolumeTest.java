package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import dev.animal.afmr.library.PlaylistPresets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerProximityVolumeTest {
    @TempDir Path directory;
    private static final String UUID="12345678-1234-1234-1234-123456789abc";
    private static Context context(double distance,boolean incoming,boolean record){
        var players=Double.isFinite(distance)?new NearbyPlayers(Map.of("Friend",distance*distance)):NearbyPlayers.EMPTY;
        return new Context("minecraft:overworld",incoming?"test:b":"test:a",false,false,6000,false,15,"world",0,70,0,0,.8,false,
            StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,record,players,false);
    }
    private static void configure(PlaylistConditions c){c.nearbyPlayerEnabled=true;c.nearbyPlayerName="Friend";c.nearbyPlayerRadius=128;c.proximityVolume.enabled=true;}
    private static final class Scenario {
        final Library lib=new Library();final Playlist a=lib.createPlaylist("A");
        final Track song=new Track("file:a","generated.wav","Generated",false);
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        Scenario(){lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=lib.settings.bufferSeconds=lib.settings.fadeOutSeconds=0;configure(a.conditions);a.volume=.4;lib.tracks.add(song);a.tracks.put(song.id,true);}
        void tick(double now,double distance){engine.tick(now,lib,context(distance,false,false),Set.of());}
        void start(double distance){tick(0,distance);tick(.01,distance);assertSame(song,engine.current());}
    }
    @Test void offByDefaultKeepsNormalVolume(){var v=new PlayerProximityVolume();assertFalse(v.enabled);assertEquals(1,v.gain(100));assertEquals(1,v.gain(Double.POSITIVE_INFINITY));}
    @Test void closerLouderUsesLinearBlockDistancesAndClampedEndpoints(){var v=new PlayerProximityVolume();v.enabled=true;assertEquals(1,v.gain(0));assertEquals(1,v.gain(4));assertEquals(.5,v.gain(18));assertEquals(0,v.gain(32));assertEquals(0,v.gain(1000));}
    @Test void reversedEndpointsGetQuieterWhenCloser(){var v=new PlayerProximityVolume();v.enabled=true;v.nearVolume=.1;v.farVolume=.9;assertEquals(.1,v.gain(0),.00001);assertEquals(.5,v.gain(18),.00001);assertEquals(.9,v.gain(40),.00001);}
    @Test void equalVolumesRemainConstant(){var v=new PlayerProximityVolume();v.enabled=true;v.nearVolume=v.farVolume=.3;assertEquals(.3,v.gain(18),.00001);assertEquals(.3,v.gain(Double.NaN),.00001);}
    @Test void untrackedOfflineAndInvalidObservationsUseFarEndpoint(){var c=new PlaylistConditions();configure(c);c.proximityVolume.farVolume=.2;for(var p:List.of(NearbyPlayers.EMPTY,new NearbyPlayers(Map.of("Friend",Double.NaN)),new NearbyPlayers(Map.of("Friend",-1.0))))assertEquals(.2,c.proximityGain(p),.00001);}
    @Test void uuidMatchingDoesNotFallBackToAnotherPlayersName(){var c=new PlaylistConditions();configure(c);c.nearbyPlayerUuid=UUID;assertEquals(0,c.proximityGain(new NearbyPlayers(Map.of("Friend",0.0))));assertEquals(1,c.proximityGain(new NearbyPlayers(Map.of(UUID,0.0,"Renamed",0.0))));}
    @Test void triggerRadiusRemainsIndependentOfVolumeEndpoints(){var c=new PlaylistConditions();configure(c);c.nearbyPlayerRadius=100;var ctx=context(60,false,false);assertTrue(c.matches(ctx,new Settings()));assertEquals(0,c.proximityGain(ctx.nearbyPlayers()));c.nearbyPlayerRadius=10;assertFalse(c.matches(context(18,false,false),new Settings()));assertEquals(.5,c.proximityGain(context(18,false,false).nearbyPlayers()));}
    @Test void disabledFilterOrGroupDoesNotApplyItsCurve(){var c=new PlaylistConditions();configure(c);c.nearbyPlayerEnabled=false;assertEquals(1,c.proximityGain(NearbyPlayers.EMPTY));c.nearbyPlayerEnabled=true;c.enabled=false;assertEquals(1,c.proximityGain(NearbyPlayers.EMPTY));}
    @Test void multipleEnabledNestedRulesUseQuietestRatherThanMultiplying(){var c=new PlaylistConditions();configure(c);c.proximityVolume.nearVolume=c.proximityVolume.farVolume=.8;var child=new PlaylistConditions();configure(child);child.proximityVolume.nearVolume=child.proximityVolume.farVolume=.4;c.groups.add(child);assertEquals(.4,c.proximityGain(NearbyPlayers.EMPTY),.00001);child.enabled=false;assertEquals(.8,c.proximityGain(NearbyPlayers.EMPTY),.00001);}
    @Test void initialGainMultipliesPlaylistVolumeWithoutAStartSpike(){var s=new Scenario();s.start(18);assertEquals(.2,s.audio.gain,.00001);assertEquals(.5,s.engine.proximityGain());}
    @Test void movingPlayerChangesGainSmoothlyWithoutRestarting(){var s=new Scenario();s.start(32);long seq=s.engine.playbackSequence();s.tick(1,4);assertEquals(0,s.audio.gain);s.tick(1.125,4);assertEquals(.2,s.audio.gain,.00001);s.tick(1.25,4);assertEquals(.4,s.audio.gain,.00001);assertEquals(seq,s.engine.playbackSequence());}
    @Test void disappearingPlayerUsesTheFarEndpointWhileRetainedSongFinishes(){var s=new Scenario();s.a.finishCurrentSongOnExit=true;s.start(4);s.tick(1,Double.POSITIVE_INFINITY);s.tick(1.25,Double.POSITIVE_INFINITY);assertEquals(0,s.audio.gain);assertSame(s.song,s.engine.current());}
    @Test void zeroGainDoesNotStopOrAdvanceTheSong(){var s=new Scenario();s.start(32);long seq=s.engine.playbackSequence();s.tick(20,32);assertEquals(0,s.audio.gain);assertSame(s.song,s.engine.current());assertEquals(seq,s.engine.playbackSequence());}
    @Test void fadeEnvelopeAndLivePlaylistVolumeMultiplyExactlyOnce(){var s=new Scenario();s.lib.settings.fadeInSeconds=4;s.start(18);s.tick(2,18);assertEquals(.1,s.audio.gain,.00001);s.tick(4,18);assertEquals(.2,s.audio.gain,.00001);s.a.volume=.2;s.tick(5,18);assertEquals(.1,s.audio.gain,.00001);}
    @Test void pausedMusicFreezesSmoothingUntilResume(){var s=new Scenario();s.start(4);s.engine.setPaused(true,1);s.tick(5,32);assertEquals(.4,s.audio.gain,.00001);s.engine.setPaused(false,10);s.tick(10,32);s.tick(10.125,32);assertEquals(.2,s.audio.gain,.00001);s.tick(10.25,32);assertEquals(0,s.audio.gain);}
    @Test void finishProtectionUsesTheActualSourceRatherThanIncomingPlaylist(){var s=new Scenario();s.a.finishCurrentSongOnExit=true;var b=s.lib.createPlaylist("B");b.volume=.9;b.tracks.put(s.song.id,true);b.conditions.biomeEnabled=true;b.conditions.biomes.add("test:b");s.lib.movePlaylist(b.id,0);s.start(18);s.engine.tick(2,s.lib,context(18,true,false),Set.of());assertEquals(s.a.id,s.engine.sourcePlaylistId());assertEquals(.2,s.audio.gain,.00001);}
    @Test void heldOverrideKeepsProximityVolumeWithAutomaticOff(){var s=new Scenario();s.a.enabled=false;assertTrue(s.engine.overridePlaylist(s.a.id,OverrideMode.UNTIL_RELEASED,0,s.lib,Set.of()));s.tick(0,18);s.tick(1,18);assertEquals(.2,s.audio.gain,.00001);}
    @Test void queuedSongUsesItsSourceCurveAndUnownedQueueUsesNormalVolume(){var s=new Scenario();s.a.enabled=false;s.engine.enqueue(s.song.id,s.a.id,s.lib);s.tick(0,18);s.tick(1,18);assertEquals(.2,s.audio.gain,.00001);s.engine.enqueue(s.song.id,"",s.lib);s.engine.nextSong(2,s.lib,context(18,false,false),Set.of());s.tick(2,18);s.tick(3,18);assertEquals(1,s.audio.gain,.00001);}
    @Test void libraryPreviewIgnoresThePreviousSourceCurve(){var s=new Scenario();s.start(32);s.engine.preview(s.song,1);s.tick(2,32);assertEquals(1,s.audio.gain,.00001);assertEquals(1,s.engine.proximityGain());}
    @Test void jukeboxFadeAndResumeKeepTheMultiplier(){var s=new Scenario();s.lib.settings.jukeboxMode=JukeboxMode.RESUME;s.lib.settings.fadeOutSeconds=2;s.start(18);s.engine.tick(1,s.lib,context(18,false,true),Set.of());s.engine.tick(2,s.lib,context(18,false,true),Set.of());assertEquals(.1,s.audio.gain,.00001);s.engine.tick(3,s.lib,context(18,false,true),Set.of());assertTrue(s.engine.jukeboxHolding());s.tick(10,18);s.tick(11,18);assertEquals(.2,s.audio.gain,.00001);}
    @Test void resetClearsSourceGain(){var s=new Scenario();s.start(32);s.engine.reset();assertEquals(1,s.engine.proximityGain());}
    @Test void normalizationRepairsNullAndInvalidNestedCurves(){var c=new PlaylistConditions();var child=new PlaylistConditions();child.proximityVolume=null;child.nearbyPlayerName=null;child.nearbyPlayerRadius=Double.NaN;c.groups.add(child);c.proximityVolume.nearDistance=Double.NaN;c.proximityVolume.farDistance=-1;c.proximityVolume.nearVolume=Double.POSITIVE_INFINITY;c.proximityVolume.farVolume=-2;c.normalize();assertTrue(c.proximityVolume.valid());assertTrue(child.proximityVolume.valid());assertEquals("",child.nearbyPlayerName);assertEquals(32,child.nearbyPlayerRadius);}
    @Test void invalidAndDegenerateCurvesAreRejected(){var v=new PlayerProximityVolume();for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,1024}){v.nearDistance=bad;assertFalse(v.valid());}v.nearDistance=4;v.farDistance=4;assertFalse(v.valid());v.farDistance=32;v.nearVolume=1.01;assertFalse(v.valid());v.nearVolume=1;v.farVolume=-.1;assertFalse(v.valid());}
    @Test void ownedCopiesProfilesClonesAndStorageKeepAllEndpoints()throws Exception{
        var s=new Scenario();var v=s.a.conditions.proximityVolume;v.nearDistance=6;v.farDistance=60;v.nearVolume=.15;v.farVolume=.85;
        var child=s.a.conditions.copy();s.a.conditions.groups.add(child);var clone=s.lib.clonePlaylist(s.a.id,"Branch");var snapshot=s.lib.snapshot();var profile=s.lib.profiles.create(s.lib,"Copy",s.lib.profiles.activeId);
        v.nearVolume=.9;assertEquals(.15,clone.conditions.proximityVolume.nearVolume);assertEquals(.15,snapshot.playlist(s.a.id).conditions.proximityVolume.nearVolume);assertEquals(.15,s.lib.profiles.playlists(s.lib,profile).get(0).conditions.proximityVolume.nearVolume);
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(snapshot);var saved=store.load().playlist(s.a.id).conditions;assertEquals(6,saved.proximityVolume.nearDistance);assertEquals(60,saved.proximityVolume.farDistance);assertEquals(.85,saved.proximityVolume.farVolume);assertTrue(saved.proximityVolume.enabled);assertEquals(.15,saved.groups.get(0).proximityVolume.nearVolume);}
    }
    @Test void portablePlaylistRoundTripKeepsCurvesAndRejectsBadTypesAndUnknownFields()throws Exception{
        var s=new Scenario();s.a.conditions.proximityVolume.nearVolume=.37;var data=PlaylistPresets.capture(s.lib,Set.of(s.a.id),false);var file=directory.resolve("proximity.afmrp");PlaylistPresets.write(file,data);assertEquals(.37,PlaylistPresets.read(file).playlists.get(0).conditions.proximityVolume.nearVolume);
        String good=Files.readString(file);Files.writeString(file,good.replace("\"nearVolume\": 0.37","\"nearVolume\": \"0.37\""));assertThrows(java.io.IOException.class,()->PlaylistPresets.read(file));
        Files.writeString(file,good.replace("\"nearVolume\": 0.37","\"unknown\": 1, \"nearVolume\": 0.37"));assertThrows(java.io.IOException.class,()->PlaylistPresets.read(file));
        data.playlists.get(0).conditions.proximityVolume.farDistance=0;assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(data));
    }
}

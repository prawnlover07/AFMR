package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackEngineTest {
    private final FakeAudio audio=new FakeAudio();
    private PlaybackEngine engine;
    private Library library;
    private final Context forest=new Context("minecraft:overworld","minecraft:forest",false,false,6000,false,false,false,15,"",0,64,0,0,0.8,false);
    private final Context desert=new Context("minecraft:overworld","minecraft:desert",false,false,6000,false,false,false,15,"",0,64,0,0,0.8,false);
    private Playlist general,forestPlaylist,desertPlaylist;
    private Track a,b,c;
    @BeforeEach void setup(){
        library=new Library();
        library.playlists.add(new Playlist("combat","Combat",Rule.COMBAT,""));
        library.playlists.add(new Playlist("underwater","Underwater",Rule.UNDERWATER,""));
        Playlist night=new Playlist("night","Night",Rule.NIGHT,"");night.enabled=false;library.playlists.add(night);
        Playlist day=new Playlist("day","Day",Rule.DAY,"");day.enabled=false;library.playlists.add(day);
        library.playlists.add(new Playlist("general","General",Rule.GENERAL,""));
        library.settings.silenceMinSeconds=10;library.settings.silenceMaxSeconds=10;
        library.settings.fadeSeconds=3;library.settings.bufferSeconds=2;
        general=library.playlist("general");general.includeVanilla=false;
        a=track("a");b=track("b");c=track("c");
        general.tracks.put(a.id,true);
        forestPlaylist=new Playlist("forest","Forest",Rule.BIOME,"minecraft:forest");forestPlaylist.includeVanilla=false;forestPlaylist.tracks.put(b.id,true);
        desertPlaylist=new Playlist("desert","Desert",Rule.BIOME,"minecraft:desert");desertPlaylist.includeVanilla=false;desertPlaylist.tracks.put(c.id,true);
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
        Context busy=new Context("minecraft:overworld","minecraft:forest",true,true,18000,false,false,false,15,"",0,64,0,0,0.8,false);
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
        startForest();engine.failed(b.id,"decoder failed",14,library.settings);tick(15,forest);tick(17,forest);
        assertEquals(a,audio.current);
    }
    @Test void skipRetainsNormalSilence(){
        startForest();engine.skip(14,library.settings);tick(23.9,forest);assertNull(audio.current);tick(24,forest);assertEquals(b,audio.current);
    }
    @Test void resetStopsAudioAndClearsWorldState(){
        startForest();engine.reset();assertNull(audio.current);assertNull(engine.current());assertEquals(PlaybackEngine.Phase.SILENCE,engine.phase());
    }
    @Test void invalidSettingsCannotBreakTimer(){
        library.settings.silenceMinSeconds=Double.NaN;library.settings.silenceMaxSeconds=-20;library.settings.fadeSeconds=Double.POSITIVE_INFINITY;
        library.normalize();assertTrue(Double.isFinite(library.settings.silenceMinSeconds));
        assertTrue(library.settings.silenceMaxSeconds>=library.settings.silenceMinSeconds);assertEquals(0,library.settings.fadeSeconds);
    }
    @Test void dayNightAndDimensionRules(){
        var night=library.playlist("night");night.enabled=true;night.tracks.put(a.id,true);
        Context context=new Context("minecraft:the_nether","minecraft:nether_wastes",false,false,18000,false,false,false,15,"",0,64,0,0,0.8,false);
        assertEquals(night,library.select(context));night.enabled=false;
        var dimension=new Playlist("nether","Nether",Rule.DIMENSION,"minecraft:the_nether");dimension.tracks.put(c.id,true);
        library.playlists.add(0,dimension);assertEquals(dimension,library.select(context));
    }
    static class FakeAudio implements PlaybackEngine.Audio{
        Track current;float gain;String fail;boolean paused;
        public void start(Track t)throws Exception{if(t.id.equals(fail))throw new Exception("bad audio");current=t;}
        public boolean playing(){return current!=null;}
        public void gain(float value){gain=value;}
        public void stop(){current=null;}
        public void setPaused(boolean value){paused=value;}
    }
}

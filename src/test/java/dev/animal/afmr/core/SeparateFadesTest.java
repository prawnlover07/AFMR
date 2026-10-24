package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.State;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SeparateFadesTest {
    @TempDir Path directory;
    private static Context context(boolean incoming,boolean combat,boolean dead){return new Context("minecraft:overworld",incoming?"test:b":"test:a",combat,false,6000,false,15,"world",0,70,0,0,0.8,dead,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY);}
    private static final class Scenario {
        final Library lib=new Library();
        final Playlist a=lib.createPlaylist("A"),b=lib.createPlaylist("B");
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        Scenario(double in,double out){
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=lib.settings.bufferSeconds=0;
            lib.settings.fadeInSeconds=in;lib.settings.fadeOutSeconds=out;
            b.conditions.biomeEnabled=true;b.conditions.biomes.add("test:b");lib.movePlaylist(b.id,0);
            for(var p:List.of(a,b)){var t=new Track(p.id,"file",p.name,false);lib.tracks.add(t);p.tracks.put(t.id,true);}
        }
        void tick(double now,boolean incoming){engine.tick(now,lib,context(incoming,false,false),Set.of());}
    }
    @Test void automaticTransitionUsesDifferentOutAndInDurations(){
        var s=new Scenario(4,2);s.tick(0,false);s.tick(1,false);assertEquals(0.25,s.audio.gain,0.001);s.tick(4,false);assertEquals(1,s.audio.gain);
        s.tick(5,true);assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());s.tick(6,true);assertEquals(0.5,s.audio.gain,0.001);
        s.tick(7,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());s.tick(9,true);assertEquals(0.5,s.audio.gain,0.001);s.tick(11,true);assertEquals(1,s.audio.gain);
    }
    @Test void overrideUsesFadeOutBeforeTheNewSongsFadeIn(){
        var s=new Scenario(6,2);s.tick(0,false);s.tick(6,false);assertTrue(s.engine.overridePlaylist(s.b.id,OverrideMode.UNTIL_RELEASED,8,s.lib,Set.of()));
        s.tick(9,false);assertEquals(0.5,s.audio.gain,0.001);s.tick(10,false);assertEquals(s.b.id,s.engine.sourcePlaylistId());
        s.tick(13,false);assertEquals(0.5,s.audio.gain,0.001);s.tick(16,false);assertEquals(1,s.audio.gain);
    }
    @Test void previewUsesFadeInIndependently(){
        var s=new Scenario(4,20);s.engine.preview(s.lib.track(s.b.id),0);s.tick(1,false);assertEquals(0.25,s.audio.gain,0.001);
        s.tick(4,false);assertEquals(1,s.audio.gain);assertTrue(s.engine.previewing());
    }
    @Test void eitherFadeCanBeZeroWithoutDisablingTheOther(){
        var s=new Scenario(0,3);s.tick(0,false);s.tick(0.1,false);assertEquals(1,s.audio.gain);
        s.tick(1,true);s.tick(2,true);assertEquals(2.0/3,s.audio.gain,0.001);s.tick(4,true);s.tick(4.1,true);assertEquals(1,s.audio.gain);
        var t=new Scenario(4,0);t.tick(0,false);t.tick(4,false);t.tick(5,true);assertEquals(t.b.id,t.engine.sourcePlaylistId());t.tick(6,true);assertEquals(0.25,t.audio.gain,0.001);
    }
    @Test void playOnTriggerKeepsBothDistinctFadesWithoutSilence(){
        var s=new Scenario(4,2);s.tick(0,false);s.tick(4,false);s.b.playOnTrigger=true;
        s.lib.settings.bufferSeconds=20;s.lib.settings.silenceMinSeconds=s.lib.settings.silenceMaxSeconds=30;
        s.tick(5,true);s.tick(6,true);assertEquals(0.5,s.audio.gain,0.001);s.tick(7,true);assertEquals(s.b.id,s.engine.sourcePlaylistId());
        s.tick(9,true);assertEquals(0.5,s.audio.gain,0.001);
    }
    @Test void combatDeathRetainsItsDedicatedFade(){
        var s=new Scenario(4,7);s.a.conditions.combat=State.YES;s.lib.settings.combatDeathFadeSeconds=1;
        s.engine.tick(0,s.lib,context(false,true,false),Set.of());s.engine.tick(4,s.lib,context(false,true,false),Set.of());
        s.engine.tick(5,s.lib,context(true,false,true),Set.of());s.engine.tick(5.5,s.lib,context(true,false,true),Set.of());assertEquals(0.5,s.audio.gain,0.001);
        s.engine.tick(6,s.lib,context(true,false,true),Set.of());assertNull(s.engine.current());
    }
    @Test void finishingAfterDeathRecoversUsingFadeInRatherThanFadeOut(){
        var s=new Scenario(4,10);s.a.conditions.combat=State.YES;s.lib.settings.combatDeathMusic=DeathMusic.KEEP_PLAYING;
        s.engine.tick(0,s.lib,context(false,true,false),Set.of());s.engine.tick(4,s.lib,context(false,true,false),Set.of());
        s.engine.tick(5,s.lib,context(true,false,false),Set.of());s.engine.tick(7,s.lib,context(true,false,false),Set.of());assertEquals(0.8,s.audio.gain,0.001);
        s.engine.tick(8,s.lib,context(true,false,true),Set.of());assertEquals(0.8,s.audio.gain,0.001);
        s.engine.tick(8.4,s.lib,context(true,false,true),Set.of());assertEquals(0.9,s.audio.gain,0.001);
    }
    @Test void bothValuesHaveIndependentBoundsDefaultsAndOwnedSaveSnapshots() throws Exception {
        var lib=Library.defaults();lib.settings.fadeInSeconds=2;lib.settings.fadeOutSeconds=7;var snapshot=lib.snapshot();
        lib.settings.fadeInSeconds=9;lib.settings.fadeOutSeconds=12;
        Path path=directory.resolve("library.json");try(var store=new LibraryStore(path)){store.save(snapshot);var loaded=store.load().settings;assertEquals(2,loaded.fadeInSeconds);assertEquals(7,loaded.fadeOutSeconds);}
        assertFalse(Files.readString(path).contains("\"fadeSeconds\""));
        lib.settings.fadeInSeconds=Double.NaN;lib.settings.fadeOutSeconds=80;lib.normalize();assertEquals(0,lib.settings.fadeInSeconds);assertEquals(60,lib.settings.fadeOutSeconds);
        lib.settings.fadeInSeconds=3;lib.settings.restoreMinecraftPlaybackDefaults();assertEquals(0,lib.settings.fadeInSeconds);assertEquals(0,lib.settings.fadeOutSeconds);
    }
}

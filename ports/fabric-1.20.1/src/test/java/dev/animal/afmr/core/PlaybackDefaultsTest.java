package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackDefaultsTest {
    @TempDir Path directory;
    private final Context outside=new Context("minecraft:overworld","minecraft:forest",false,false,6000,false,15,"world",0,80,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY);
    @Test void newSettingsUseNormalMinecraftGameTiming(){
        var s=new Settings();assertEquals(600,s.silenceMinSeconds);assertEquals(1200,s.silenceMaxSeconds);assertEquals(0,s.fadeInSeconds);assertEquals(0,s.bufferSeconds);
    }
    @Test void resetOnlyChangesTheFourPlaybackTimingValues(){
        var lib=Library.defaults();var p=lib.createPlaylist("Keep me");p.tracks.put("a",false);p.finishCurrentSong=true;p.finishCurrentSongOnExit=true;p.conditions.biomeEnabled=true;p.conditions.biomes.add("minecraft:forest");
        lib.movePlaylist(p.id,0);var beforeOrder=lib.playlists.stream().map(q->q.id).toList();
        var s=lib.settings;s.silenceMinSeconds=1;s.silenceMaxSeconds=2;s.fadeInSeconds=s.fadeOutSeconds=3;s.bufferSeconds=4;
        s.combatEnabled=false;s.combatDeathMusic=DeathMusic.CUT;s.combatDeathFadeSeconds=0.2;s.rescanOnLaunch=true;s.nowPlaying=false;s.popupSeconds=15;s.replayGainEnabled=false;s.continuePlayingOnMatch=true;
        s.restoreMinecraftPlaybackDefaults();
        assertEquals(600,s.silenceMinSeconds);assertEquals(1200,s.silenceMaxSeconds);assertEquals(0,s.fadeInSeconds);assertEquals(0,s.bufferSeconds);
        assertFalse(s.combatEnabled);assertEquals(DeathMusic.CUT,s.combatDeathMusic);assertEquals(0.2,s.combatDeathFadeSeconds);assertTrue(s.rescanOnLaunch);assertFalse(s.nowPlaying);assertEquals(15,s.popupSeconds);
        assertFalse(s.replayGainEnabled);
        assertTrue(s.continuePlayingOnMatch);
        assertEquals(beforeOrder,lib.playlists.stream().map(q->q.id).toList());assertEquals(Boolean.FALSE,p.tracks.get("a"));assertTrue(p.finishCurrentSong);assertTrue(p.conditions.biomeEnabled);assertEquals(Set.of("minecraft:forest"),p.conditions.biomes);
        assertTrue(p.finishCurrentSongOnExit);
    }
    @Test void loadingExistingTimingDoesNotAutomaticallyResetIt() throws Exception {
        var lib=Library.defaults();lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=3;lib.settings.bufferSeconds=3;lib.settings.silenceMinSeconds=5;lib.settings.silenceMaxSeconds=10;
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(lib);var restored=store.load();assertEquals(3,restored.settings.fadeInSeconds);assertEquals(3,restored.settings.bufferSeconds);assertEquals(5,restored.settings.silenceMinSeconds);assertEquals(10,restored.settings.silenceMaxSeconds);}
    }
    @Test void replayGainDefaultsOnAndItsSavedSwitchIsIndependent() throws Exception {
        var lib=Library.defaults();assertTrue(lib.settings.replayGainEnabled);lib.settings.replayGainEnabled=false;var copy=lib.snapshot();lib.settings.replayGainEnabled=true;
        assertFalse(copy.settings.replayGainEnabled);
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(copy);assertFalse(store.load().settings.replayGainEnabled);}
    }
    @Test void widgetStylePersistsIndependentlyOfVisibilityDurationAndTiming() throws Exception {
        var lib=Library.defaults();
        lib.settings.nowPlaying=false;lib.settings.popupSeconds=12;
        var copy=lib.snapshot();

        copy.settings.restoreMinecraftPlaybackDefaults();
        try(var store=new LibraryStore(directory.resolve("widget.json"))){
            store.save(copy);var restored=store.load();

            assertFalse(restored.settings.nowPlaying);assertEquals(12,restored.settings.popupSeconds);
            restored.normalize();
        }
    }
    @Test void resetKeepsCurrentGapAndUsesMinecraftTimingAfterSongEnds(){
        var lib=Library.defaults();var track=new Track("a","file","A",false);lib.tracks.add(track);lib.playlist("day_outside").tracks.put(track.id,true);
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;
        var audio=new PlaybackEngineTest.FakeAudio();var engine=new PlaybackEngine(audio,new Random(1));engine.tick(0,lib,outside,Set.of());
        lib.settings.restoreMinecraftPlaybackDefaults();engine.tick(9,lib,outside,Set.of());assertNull(audio.current);
        engine.tick(10,lib,outside,Set.of());assertSame(track,audio.current);engine.tick(10.05,lib,outside,Set.of());assertEquals(1,audio.gain);
        audio.current=null;engine.tick(20,lib,outside,Set.of());double gap=engine.remainingSilence(20);assertTrue(gap>=600&&gap<=1200);
        engine.tick(20+gap-0.01,lib,outside,Set.of());assertNull(audio.current);engine.tick(20+gap,lib,outside,Set.of());assertSame(track,audio.current);
    }
}

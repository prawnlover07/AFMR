package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CurrentPlaybackTest {
    private final Library library = Library.defaults();
    private final PlaybackEngineTest.FakeAudio audio = new PlaybackEngineTest.FakeAudio();
    private final PlaybackEngine engine = new PlaybackEngine(audio, new Random(7));
    private final Track song = new Track("song", "song", "A song", false);
    private Context context(String biome, String dimension, boolean day, boolean underground, boolean combat) {
        return new Context(dimension,biome,combat,true,(day?6000:18000),underground,false,underground,15,"world",0,100,0,(underground?8:0),0.8,false);
    }
    private Context outside() { return context("minecraft:forest", "minecraft:overworld", true, false, false); }
    private void setup() {
        library.tracks.add(song);
        library.settings.silenceMinSeconds = library.settings.silenceMaxSeconds = 10;
        library.settings.bufferSeconds = 3;
        for (var p : library.playlists) { p.includeVanilla = false; p.tracks.put(song.id, true); }
    }
    @Test void centralCustomPlaylistsIgnoreBiomeDimensionAndWater() {
        setup();
        for (String biome : List.of("minecraft:forest", "minecraft:desert", "another_mod:custom_biome"))
            for (String dimension : List.of("minecraft:overworld", "minecraft:the_nether", "another_mod:world")) {
                assertEquals("day_outside", library.selectPlayable(context(biome, dimension, true, false, false), Set.of(), Set.of()).id);
                assertEquals("night_outside", library.selectPlayable(context(biome, dimension, false, false, false), Set.of(), Set.of()).id);
                assertEquals("underground", library.selectPlayable(context(biome, dimension, true, true, false), Set.of(), Set.of()).id);
                assertEquals("combat", library.selectPlayable(context(biome, dimension, true, false, true), Set.of(), Set.of()).id);
            }
    }
    @Test void disabledDayPlaylistExplainsWhyOutsideHasNoMusic() {
        setup(); library.playlist("day_outside").enabled = false;
        assertNull(library.selectPlayable(outside(), Set.of(), Set.of()));
        assertTrue(library.playbackUnavailable(outside(), Set.of()).contains("Day outside playlist is off"));
        library.playlist("day_outside").enabled = true;
        assertEquals("", library.playbackUnavailable(outside(), Set.of()));
    }
    @Test void emptyPlaylistExplainsMissingSongs() {
        setup(); library.playlist("day_outside").tracks.clear();
        assertTrue(library.playbackUnavailable(outside(), Set.of()).contains("No enabled songs for Day outside"));
    }
    @Test void manualPreviewBypassesDisabledPlaylistsAndContextChanges() {
        setup(); library.playlists.forEach(p -> p.enabled = false);
        engine.tick(0, library, outside(), Set.of());
        engine.preview(song, 1);
        engine.tick(5, library, context("any", "any", false, true, true), Set.of());
        assertEquals(song, engine.current()); assertTrue(engine.previewing());
        assertEquals(PlaybackEngine.Phase.PLAYING, engine.phase());
    }
    @Test void previewFinishesThenResumesNormalContextAndSilence() {
        setup(); engine.preview(song, 1);
        engine.tick(2, library, outside(), Set.of());
        engine.tick(5, library, outside(), Set.of());
        assertTrue(engine.previewing());
        audio.current = null;
        engine.tick(8, library, outside(), Set.of());
        assertFalse(engine.previewing()); assertNull(engine.current());
        engine.tick(17.9, library, outside(), Set.of()); assertNull(engine.current());
        engine.tick(18, library, outside(), Set.of()); assertEquals(song, engine.current());
        assertFalse(engine.previewing());
    }
    @Test void failedPreviewShowsErrorAndCanBeRetriedExplicitly() {
        setup(); audio.fail = song.id; engine.preview(song, 1);
        assertFalse(engine.error().isBlank()); assertNull(engine.current()); assertFalse(engine.previewing());
        audio.fail = null; engine.preview(song, 2);
        assertEquals(song, engine.current()); assertTrue(engine.error().isBlank());
        engine.reset(); assertFalse(engine.previewing());
    }
}

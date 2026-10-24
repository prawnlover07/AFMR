package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackInspectorTest {
    private final Library lib=Library.defaults();
    private final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
    private final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
    private Context context(boolean day,boolean dead){return new Context("minecraft:overworld","minecraft:forest",false,false,day?6000:18000,false,15,"world",0,80,0,0,0.8,dead,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false);}
    private void song(Playlist p,String id){lib.tracks.add(new Track(id,"file",id,false));p.tracks.put(id,true);}
    private PlaybackInspector.Entry row(String id,Context c){return PlaybackInspector.inspect(lib,c,Set.of(),engine).stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();}
    @Test void disabledManualOnlyAndUnmatchedStatesHaveSeparateExplanations(){
        var p=lib.createPlaylist("Manual");p.manualOnly=true;song(p,"manual");
        assertTrue(row(p.id,context(true,false)).status().contains("Manual only"));p.enabled=false;assertTrue(row(p.id,context(true,false)).status().contains("disabled"));
        assertTrue(row("combat",context(true,false)).status().contains("Not currently in combat"));lib.settings.combatEnabled=false;assertTrue(row("combat",context(true,false)).status().contains("globally"));
    }
    @Test void winnerShadowedAndEmptyPlaylistsAreDistinguished(){
        song(lib.playlist("day_outside"),"day");var p=lib.createPlaylist("Any");song(p,"custom");
        assertEquals("Automatic winner",row(p.id,context(true,false)).status());assertTrue(row("day_outside",context(true,false)).status().contains("Lower priority than Any"));
        p.tracks.clear();assertTrue(row(p.id,context(true,false)).status().contains("No playable songs"));assertEquals("Automatic winner",row("day_outside",context(true,false)).status());
    }
    @Test void customFailureUsesTheSamePredicateAsPlayback(){
        var p=lib.createPlaylist("High forest");song(p,"custom");p.conditions.elevationEnabled=true;p.conditions.minY=100;p.conditions.maxY=200;
        assertFalse(p.matches(context(true,false),lib.settings));assertEquals(PlaylistConditions.Failure.ELEVATION,p.conditions.failure(context(true,false),lib.settings));
        var entry=row(p.id,context(true,false));assertTrue(entry.status().contains("elevation"));assertTrue(entry.details().contains("Biome: minecraft:forest"));assertTrue(entry.details().stream().anyMatch(s->s.contains("block Y: 80")));
    }
    @Test void waitingExplainsBufferSilenceRespawnAndNoSongs(){
        song(lib.playlist("day_outside"),"day");lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.bufferSeconds=3;
        engine.tick(0,lib,context(true,false),Set.of());assertTrue(PlaybackInspector.waiting(engine,lib,context(true,false),Set.of(),1).contains("Context buffer"));
        assertTrue(PlaybackInspector.waiting(engine,lib,context(true,false),Set.of(),4).contains("Silent gap"));assertTrue(PlaybackInspector.waiting(engine,lib,context(true,true),Set.of(),4).contains("respawn"));
        engine.tick(4,lib,context(false,false),Set.of());assertTrue(PlaybackInspector.waiting(engine,lib,context(false,false),Set.of(),8).contains("No available playlist"));
    }
    @Test void retainedSongsExplainExitAndEntryProtection(){
        var day=lib.playlist("day_outside");var night=lib.playlist("night_outside");song(day,"day");song(night,"night");lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;day.finishCurrentSongOnExit=true;
        engine.tick(0,lib,context(true,false),Set.of());engine.tick(1,lib,context(false,false),Set.of());
        assertTrue(PlaybackInspector.waiting(engine,lib,context(false,false),Set.of(),1).contains("exit setting"));day.finishCurrentSongOnExit=false;night.finishCurrentSong=true;
        assertTrue(PlaybackInspector.waiting(engine,lib,context(false,false),Set.of(),1).contains("entry setting"));
    }
    @Test void inspectorKeepsAutomaticWinnerVisibleDuringAnOverride(){
        var day=lib.playlist("day_outside");song(day,"day");var manual=lib.createPlaylist("Manual");manual.manualOnly=true;song(manual,"manual");
        assertTrue(engine.overridePlaylist(manual.id,OverrideMode.ONE_SONG,0,lib,Set.of()));engine.tick(0,lib,context(true,false),Set.of());
        assertEquals("day_outside",engine.automaticSelected().id);assertTrue(row(manual.id,context(true,false)).status().startsWith("OVERRIDE"));
        assertTrue(PlaybackInspector.waiting(engine,lib,context(true,false),Set.of(),0).contains("One-song override"));
    }
    @Test void offPlaylistCanBeTheOverrideWhileInspectorExplainsItsAutomaticExclusion(){
        song(lib.playlist("day_outside"),"day");var p=lib.playlist("combat");song(p,"fight");p.enabled=false;
        assertTrue(engine.overridePlaylist(p.id,OverrideMode.ONE_SONG,0,lib,Set.of()));engine.tick(0,lib,context(true,false),Set.of());
        assertEquals("combat",engine.sourcePlaylistId());assertEquals("day_outside",engine.automaticSelected().id);
        assertEquals("OVERRIDE / Automatic selection disabled",row(p.id,context(true,false)).status());assertFalse(p.enabled);
    }
    @Test void previewAndFadingAreReported(){
        var day=lib.playlist("day_outside");song(day,"day");engine.preview(lib.track("day"),0);assertTrue(PlaybackInspector.waiting(engine,lib,context(true,false),Set.of(),0).contains("Manual preview"));
        var combat=lib.playlist("combat");song(combat,"fight");lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=2;
        engine.overridePlaylist(combat.id,OverrideMode.UNTIL_RELEASED,1,lib,Set.of());assertTrue(PlaybackInspector.waiting(engine,lib,context(true,false),Set.of(),1).contains("Combat; no gap"));
    }
    @Test void songDetailsCountDisabledMissingFailedAndAutomaticVanilla(){
        var p=lib.playlist("day_outside");song(p,"ok");song(p,"disabled");p.tracks.put("disabled",false);song(p,"failed");p.tracks.put("missing",true);
        var vanilla=new Track("vanilla","resource","Vanilla",true);lib.tracks.add(vanilla);
        var lines=PlaybackInspector.songCounts(lib,p,Set.of("vanilla"),Set.of("failed"));
        assertEquals("Available songs: 2 (automatic vanilla: 1)",lines.getFirst());assertEquals("Disabled: 1; missing: 1; failed this session: 1",lines.get(1));
    }
    @Test void diagnosticsUseIndexedAssignmentsWithALargeLibrary(){
        var tracks=new LibraryIndexTest.GuardedTracks();lib.tracks=tracks;for(int i=0;i<70000;i++)tracks.add(new Track("t"+i,"file","Song",false));lib.playlist("day_outside").tracks.put("t69999",true);lib.rebuildTrackIndex();tracks.locked=true;
        assertEquals("Automatic winner",row("day_outside",context(true,false)).status());assertTrue(PlaybackInspector.songCounts(lib,lib.playlist("day_outside"),Set.of(),Set.of()).getFirst().startsWith("Available songs: 1"));
    }
}

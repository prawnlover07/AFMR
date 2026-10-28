package dev.animal.afmr.core;

import com.google.gson.Gson;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OverrideSettingsTest {
    @TempDir Path directory;
    private Context outside(){return new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"world",0,80,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);}
    @Test void manualOnlyKeepsConditionsButExcludesAutomaticSelection(){
        var lib=Library.defaults();var p=lib.createPlaylist("Manual");p.manualOnly=true;var t=new Track("a","file","A",false);lib.tracks.add(t);p.tracks.put(t.id,true);
        assertTrue(p.matches(outside(),lib.settings));assertTrue(lib.hasPlayable(p,Set.of(),Set.of()));assertNull(lib.selectPlayable(outside(),Set.of(),Set.of()));
        assertNotSame(p,lib.select(outside()));p.manualOnly=false;assertSame(p,lib.selectPlayable(outside(),Set.of(),Set.of()));
    }
    @Test void eightSlotsStartWithCombatToggleAndUnassignedRemainingSlots(){
        var lib=Library.defaults();assertEquals(8,lib.settings.overrideSlots.length);assertEquals("combat",lib.settings.overrideSlots[0].playlistId);assertEquals(OverrideMode.UNTIL_RELEASED,lib.settings.overrideSlots[0].mode);
        for(int i=1;i<8;i++){assertEquals("",lib.settings.overrideSlots[i].playlistId);assertEquals(OverrideMode.ONE_SONG,lib.settings.overrideSlots[i].mode);}
    }
    @Test void settingsAndManualOnlyAreOwnedBySnapshotAndRoundTrip() throws Exception {
        var lib=Library.defaults();var p=lib.createPlaylist("Manual");p.manualOnly=true;lib.settings.overrideSlots[7].playlistId=p.id;lib.settings.overrideSlots[7].mode=OverrideMode.UNTIL_RELEASED;
        var copy=lib.snapshot();p.manualOnly=false;lib.settings.overrideSlots[7].playlistId="changed";
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(copy);var read=store.load();assertTrue(read.playlist(p.id).manualOnly);assertEquals(p.id,read.settings.overrideSlots[7].playlistId);assertEquals(OverrideMode.UNTIL_RELEASED,read.settings.overrideSlots[7].mode);}
    }
    @Test void absentFieldsUseDefaultsWithoutChangingExistingChoices() throws Exception {
        var lib=Library.defaults();lib.playlist("day_outside").finishCurrentSong=true;var json=new Gson().toJsonTree(lib).getAsJsonObject();json.getAsJsonObject("settings").remove("overrideSlots");
        for(var p:json.getAsJsonArray("playlists"))p.getAsJsonObject().remove("manualOnly");var file=directory.resolve("library.json");Files.writeString(file,json.toString());
        try(var store=new LibraryStore(file)){var read=store.load();assertEquals(8,read.settings.overrideSlots.length);assertEquals("combat",read.settings.overrideSlots[0].playlistId);assertFalse(read.playlist("day_outside").manualOnly);assertTrue(read.playlist("day_outside").finishCurrentSong);}
    }
    @Test void invalidBindingArraysAreNormalizedBeforeUse(){
        var lib=Library.defaults();lib.settings.overrideSlots=new OverrideBinding[]{null};lib.normalize();assertEquals(8,lib.settings.overrideSlots.length);
        for(var b:lib.settings.overrideSlots){assertNotNull(b);assertNotNull(b.playlistId);assertNotNull(b.mode);}
        lib.settings.overrideSlots[0].playlistId=null;lib.settings.overrideSlots[0].mode=null;lib.normalize();assertEquals("",lib.settings.overrideSlots[0].playlistId);assertEquals(OverrideMode.ONE_SONG,lib.settings.overrideSlots[0].mode);
        lib.settings.overrideSlots=null;lib.normalize();assertEquals("combat",lib.settings.overrideSlots[0].playlistId);
    }
    @Test void savedKeybindCanOverrideAnOffPlaylistWithoutChangingItsAutomaticSwitch() throws Exception {
        var lib=Library.defaults();var p=lib.playlist("combat");p.enabled=false;var t=new Track("fight","file","Fight",false);lib.tracks.add(t);p.tracks.put(t.id,true);
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(lib);var read=store.load();var b=read.settings.overrideSlots[0];var engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));
            assertTrue(engine.overridePlaylist(b.playlistId,b.mode,0,read,Set.of()));engine.tick(0,read,outside(),Set.of());
            assertEquals("combat",engine.sourcePlaylistId());assertFalse(read.playlist("combat").enabled);assertNull(engine.automaticSelected());
        }
    }
}

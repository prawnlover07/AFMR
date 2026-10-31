package dev.animal.afmr.core;

import com.google.gson.Gson;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistTransitionsTest {
    @TempDir Path directory;
    private static final class Scenario {
        final Library lib=new Library();
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        final Playlist a=playlist("A"),b=playlist("B"),c=playlist("C");
        Scenario(){
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=2;lib.settings.bufferSeconds=2;
            tick(0,a);tick(10,a);tick(12,a);assertEquals(a.id,audio.current.id);assertEquals(1,audio.gain);
        }
        private Playlist playlist(String name){
            var p=lib.createPlaylist(name);p.conditions.biomeEnabled=true;p.conditions.biomes.add("test:"+name.toLowerCase(Locale.ROOT));
            var track=new Track(p.id,"file",name,false);lib.tracks.add(track);p.tracks.put(track.id,true);return p;
        }
        void tick(double time,Playlist where){
            var context=new Context("minecraft:overworld","test:"+where.name.toLowerCase(Locale.ROOT),false,false,6000,false,15,"world",0,80,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
            engine.tick(time,lib,context,Set.of());
        }
        void enterB(){tick(14,b);tick(15.9,b);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());tick(16,b);}
    }
    @TestFactory List<DynamicTest> entryAndExitIndependentlyPermitFinishing(){
        var tests=new ArrayList<DynamicTest>();
        for(boolean exit:List.of(false,true))for(boolean entry:List.of(false,true))tests.add(DynamicTest.dynamicTest("A exit="+exit+", B entry="+entry,()->{
            var s=new Scenario();s.a.finishCurrentSongOnExit=exit;s.b.finishCurrentSong=entry;s.enterB();
            if(exit||entry){
                assertEquals(PlaybackEngine.Phase.PLAYING,s.engine.phase());assertEquals(s.a.id,s.audio.current.id);
                s.audio.current=null;s.tick(20,s.b);s.tick(29.9,s.b);assertNull(s.audio.current);s.tick(30,s.b);assertEquals(s.b.id,s.audio.current.id);
            }else{
                assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());s.tick(17,s.b);assertEquals(0.5,s.audio.gain,0.001);
                s.tick(18,s.b);assertNull(s.audio.current);s.tick(27.9,s.b);assertNull(s.audio.current);s.tick(28,s.b);assertEquals(s.b.id,s.audio.current.id);
            }
        }));return tests;
    }
    @Test void entryOfTheOutgoingPlaylistDoesNotProtectItsOwnSong(){
        var s=new Scenario();s.a.finishCurrentSong=true;s.enterB();assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
    }
    @Test void exitOfTheIncomingPlaylistDoesNotProtectTheOutgoingSong(){
        var s=new Scenario();s.b.finishCurrentSongOnExit=true;s.enterB();assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
    }
    @Test void retainedSongKeepsItsOriginalSourceAcrossSeveralTransitions(){
        var s=new Scenario();s.b.finishCurrentSong=true;s.b.finishCurrentSongOnExit=true;s.enterB();
        s.tick(18,s.c);s.tick(20,s.c);assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
        assertEquals(s.a.id,s.audio.current.id); // B never started this song, so its exit setting cannot keep it.
    }
    @Test void originalSourceExitProtectionPersistsAcrossSeveralTransitions(){
        var s=new Scenario();s.a.finishCurrentSongOnExit=true;s.enterB();s.tick(18,s.c);s.tick(20,s.c);
        assertEquals(PlaybackEngine.Phase.PLAYING,s.engine.phase());assertEquals(s.a.id,s.audio.current.id);
        s.audio.current=null;s.tick(21,s.c);s.tick(31,s.c);assertEquals(s.c.id,s.audio.current.id);
    }
    @Test void returningToTheSongsOwnPlaylistDoesNotInterruptIt(){
        var s=new Scenario();s.b.finishCurrentSong=true;s.enterB();s.tick(18,s.a);s.tick(20,s.a);
        assertEquals(PlaybackEngine.Phase.PLAYING,s.engine.phase());assertEquals(s.a.id,s.audio.current.id);
    }
    @Test void exitCanKeepPlayingWhenNoPlaylistMatches(){
        var s=new Scenario();s.a.finishCurrentSongOnExit=true;s.b.enabled=false;s.enterB();
        assertNull(s.engine.selected());assertEquals(PlaybackEngine.Phase.PLAYING,s.engine.phase());
        s.audio.current=null;s.tick(20,s.b);s.tick(100,s.b);assertNull(s.audio.current);
    }
    @Test void noMatchingPlaylistFadesWhenExitIsOff(){
        var s=new Scenario();s.b.enabled=false;s.enterB();assertNull(s.engine.selected());assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
    }
    @Test void deletingTheSourceBeforeTransitionDoesNotLeaveStaleExitProtection(){
        var s=new Scenario();s.a.finishCurrentSongOnExit=true;assertTrue(s.lib.removePlaylist(s.a.id));s.enterB();
        assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
    }
    @Test void aSharedTrackStillBelongsToThePlaylistThatStartedIt(){
        var s=new Scenario();s.b.tracks.clear();s.b.tracks.put(s.a.id,true);s.b.finishCurrentSong=true;s.b.finishCurrentSongOnExit=true;
        s.enterB();s.tick(18,s.c);s.tick(20,s.c);assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
    }
    @Test void manualSkipOverridesBothFinishSettingsAndKeepsNormalSilence(){
        var s=new Scenario();s.a.finishCurrentSongOnExit=true;s.b.finishCurrentSong=true;s.enterB();
        s.engine.skip(17,s.lib);assertNull(s.audio.current);s.tick(26.9,s.b);assertNull(s.audio.current);s.tick(27,s.b);assertEquals(s.b.id,s.audio.current.id);
        s.tick(30,s.b);s.tick(31,s.c);s.tick(33,s.c);assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());
    }
    @Test void previewsStayIndependentOfPlaylistEntryAndExitSettings(){
        var s=new Scenario();var preview=new Track("preview","file","Preview",false);s.engine.preview(preview,13,false);
        s.enterB();assertSame(preview,s.audio.current);assertTrue(s.engine.previewing());
        s.audio.current=null;s.tick(20,s.b);s.tick(30,s.b);assertEquals(s.b.id,s.audio.current.id);assertFalse(s.engine.previewing());
    }
    @Test void priorityChangesUseTheSongsExitSetting(){
        var s=new Scenario();s.b.conditions.biomeEnabled=false;s.a.finishCurrentSongOnExit=true;s.lib.movePlaylist(s.b.id,0);
        s.tick(14,s.a);s.tick(16,s.a);assertSame(s.b,s.engine.selected());assertEquals(s.a.id,s.audio.current.id);assertEquals(PlaybackEngine.Phase.PLAYING,s.engine.phase());
    }
    @Test void bothSettingsSurviveOwnedSnapshotsAndSaving() throws Exception {
        var s=new Scenario();s.a.finishCurrentSong=true;s.a.finishCurrentSongOnExit=true;s.b.finishCurrentSongOnExit=true;
        var snapshot=s.lib.snapshot();s.a.finishCurrentSong=false;s.a.finishCurrentSongOnExit=false;s.b.finishCurrentSongOnExit=false;
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(snapshot);var restored=store.load();
            assertTrue(restored.playlist(s.a.id).finishCurrentSong);assertTrue(restored.playlist(s.a.id).finishCurrentSongOnExit);
            assertFalse(restored.playlist(s.b.id).finishCurrentSong);assertTrue(restored.playlist(s.b.id).finishCurrentSongOnExit);}
    }
    @Test void existingEntryChoiceIsRetainedAndExitDefaultsOff() throws Exception {
        var s=new Scenario();s.a.finishCurrentSong=true;var json=new Gson().toJsonTree(s.lib).getAsJsonObject();
        for(var p:json.getAsJsonArray("playlists"))p.getAsJsonObject().remove("finishCurrentSongOnExit");
        var file=directory.resolve("library.json");Files.writeString(file,json.toString());
        try(var store=new LibraryStore(file)){var restored=store.load();assertTrue(restored.playlist(s.a.id).finishCurrentSong);assertFalse(restored.playlist(s.a.id).finishCurrentSongOnExit);}
    }
    @Test void transitionsDoNotWalkTheMusicLibrary(){
        var s=new Scenario();var tracks=new LibraryIndexTest.GuardedTracks();tracks.addAll(s.lib.tracks);s.lib.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("unused"+i,"file","Unused",false));s.lib.rebuildTrackIndex();tracks.locked=true;
        s.enterB();s.tick(18,s.b);s.tick(28,s.b);assertEquals(s.b.id,s.audio.current.id);
    }
}

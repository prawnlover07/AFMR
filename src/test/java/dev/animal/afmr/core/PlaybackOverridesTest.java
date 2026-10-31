package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackOverridesTest {
    private Library lib;
    private PlaybackEngineTest.FakeAudio audio;
    private PlaybackEngine engine;
    private Playlist manual;
    private Context context(boolean day,boolean busy,boolean dead){return new Context("minecraft:overworld","minecraft:plains",busy,false,day?6000:18000,false,15,"world",0,80,0,0,0.8,dead,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);}
    private void tick(double t,boolean day){engine.tick(t,lib,context(day,false,false),Set.of());}
    private void force(double time,Playlist p,OverrideMode mode){assertTrue(engine.overridePlaylist(p.id,mode,time,lib,Set.of()));}
    @BeforeEach void setup(){
        lib=Library.defaults();audio=new PlaybackEngineTest.FakeAudio();engine=new PlaybackEngine(audio,new Random(5));manual=lib.createPlaylist("Manual");manual.manualOnly=true;
        for(var p:List.of(manual,lib.playlist("combat"),lib.playlist("day_outside"),lib.playlist("night_outside")))for(int i=0;i<2;i++){
            var t=new Track(p.id+"-"+i,"file",p.name+" "+i,false);lib.tracks.add(t);p.tracks.put(t.id,true);}
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=2;lib.settings.bufferSeconds=3;
        tick(0,true);tick(10,true);tick(12,true);assertEquals("day_outside",engine.sourcePlaylistId());
    }
    @Test void oneSongFromSilenceStartsWithoutWaitingForGapOrBuffer(){
        engine.reset();force(13,manual,OverrideMode.ONE_SONG);tick(13,true);
        assertEquals(manual.id,engine.sourcePlaylistId());assertEquals(manual.id,engine.overrideId());assertFalse(engine.previewing());
    }
    @Test void explicitOverrideFadesImmediatelyDespiteFinishFlagsAndBypassesGap(){
        lib.playlist("day_outside").finishCurrentSongOnExit=true;manual.finishCurrentSong=true;lib.settings.bufferSeconds=60;
        force(13,manual,OverrideMode.ONE_SONG);tick(13,true);assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
        tick(14,true);assertEquals(0.5,audio.gain,0.001);tick(15,true);assertEquals(manual.id,engine.sourcePlaylistId());
    }
    @Test void combatCanBeForcedWithoutAttackingEvenWithGlobalCombatOff(){
        lib.settings.combatEnabled=false;force(13,lib.playlist("combat"),OverrideMode.UNTIL_RELEASED);tick(15,true);
        assertEquals("combat",engine.sourcePlaylistId());assertEquals("day_outside",engine.automaticSelected().id);
    }
    @Test void oneSongReturnsToTheCurrentEnvironmentAfterNormalSilence(){
        force(13,manual,OverrideMode.ONE_SONG);tick(15,true);tick(20,false);assertEquals(manual.id,engine.sourcePlaylistId());
        audio.current=null;tick(21,false);assertNull(engine.overrideId());assertNull(engine.current());
        tick(30.9,false);assertNull(engine.current());tick(31,false);assertEquals("night_outside",engine.sourcePlaylistId());
    }
    @Test void heldOverrideIgnoresContextChangesAndKeepsShuffledPlayback(){
        force(13,manual,OverrideMode.UNTIL_RELEASED);tick(15,true);var first=engine.current();tick(20,false);
        assertEquals(manual.id,engine.selected().id);assertEquals("night_outside",engine.automaticSelected().id);
        audio.current=null;tick(21,false);tick(31,false);assertEquals(manual.id,engine.sourcePlaylistId());assertNotEquals(first.id,engine.current().id);assertNotNull(engine.overrideId());
    }
    @Test void oneSongSkipReturnsToAutomaticButHeldSkipStaysHeld(){
        force(13,manual,OverrideMode.ONE_SONG);tick(15,true);engine.skip(16,lib);assertNull(engine.overrideId());tick(26,true);assertEquals("day_outside",engine.sourcePlaylistId());
        force(27,manual,OverrideMode.UNTIL_RELEASED);tick(29,true);engine.skip(30,lib);assertEquals(manual.id,engine.overrideId());tick(40,true);assertEquals(manual.id,engine.sourcePlaylistId());
    }
    @Test void explicitReleaseRestoresNormalEntryAndExitRules(){
        force(13,manual,OverrideMode.UNTIL_RELEASED);tick(15,true);manual.finishCurrentSongOnExit=true;engine.returnAutomatic();tick(16,true);tick(19,true);
        assertNull(engine.overrideId());assertEquals("day_outside",engine.selected().id);assertEquals(manual.id,engine.sourcePlaylistId());assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
    }
    @Test void emptyAndMissingRequestsLeaveExistingMusicAlone(){
        var before=engine.current();manual.tracks.clear();assertFalse(engine.overridePlaylist(manual.id,OverrideMode.ONE_SONG,13,lib,Set.of()));
        assertFalse(engine.overridePlaylist("missing",OverrideMode.ONE_SONG,13,lib,Set.of()));assertSame(before,engine.current());assertNull(engine.overrideId());
    }
    @Test void switchingAutomaticOffKeepsHeldOverrideButDeletingPlaylistReleasesIt(){
        force(13,manual,OverrideMode.UNTIL_RELEASED);tick(15,true);manual.enabled=false;tick(16,true);assertEquals(manual.id,engine.overrideId());
        var first=engine.current();audio.current=null;tick(17,true);tick(27,true);assertEquals(manual.id,engine.sourcePlaylistId());assertNotEquals(first.id,engine.current().id);
        lib.removePlaylist(manual.id);tick(28,true);assertNull(engine.overrideId());assertEquals("day_outside",engine.selected().id);
    }
    @TestFactory List<DynamicTest> automaticOffPlaylistsSupportBothOverrideModes(){
        return List.of(OverrideMode.ONE_SONG,OverrideMode.UNTIL_RELEASED).stream().map(mode->DynamicTest.dynamicTest(mode.name(),()->{
            manual.enabled=false;manual.manualOnly=false;force(13,manual,mode);tick(15,true);
            assertEquals(manual.id,engine.sourcePlaylistId());assertEquals(mode,engine.overrideMode());assertFalse(manual.enabled);
            assertEquals("day_outside",engine.automaticSelected().id);engine.returnAutomatic();engine.reset();
        })).toList();
    }
    @Test void oneSongFromAnOffPlaylistReturnsToAutomaticWithoutEnablingIt(){
        manual.enabled=false;manual.manualOnly=false;force(13,manual,OverrideMode.ONE_SONG);tick(15,true);audio.current=null;tick(16,true);tick(26,true);
        assertNull(engine.overrideId());assertFalse(manual.enabled);assertEquals("day_outside",engine.sourcePlaylistId());
    }
    @Test void switchingAutomaticOffDoesNotCancelAOneSongOverride(){
        force(13,manual,OverrideMode.ONE_SONG);tick(15,true);manual.enabled=false;manual.manualOnly=false;tick(16,true);
        assertEquals(manual.id,engine.overrideId());assertEquals(manual.id,engine.sourcePlaylistId());assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
        audio.current=null;tick(17,true);tick(27,true);assertNull(engine.overrideId());assertEquals("day_outside",engine.sourcePlaylistId());assertFalse(manual.enabled);
    }
    @Test void offPlaylistOverridesStillExcludeIndividuallyDisabledSongs(){
        manual.enabled=false;String disabled=manual.tracks.keySet().iterator().next();manual.tracks.put(disabled,false);
        force(13,manual,OverrideMode.UNTIL_RELEASED);tick(15,true);assertNotEquals(disabled,engine.current().id);
        audio.current=null;tick(16,true);tick(26,true);assertNotEquals(disabled,engine.current().id);
        manual.tracks.replaceAll((id,value)->false);tick(27,true);assertNull(engine.overrideId());
        assertFalse(engine.overridePlaylist(manual.id,OverrideMode.ONE_SONG,28,lib,Set.of()));
    }
    @Test void allFailedOverrideTracksFallBackAndCanBeRetried(){
        String id=manual.tracks.keySet().iterator().next();manual.tracks.clear();manual.tracks.put(id,true);audio.fail=id;
        force(13,manual,OverrideMode.ONE_SONG);tick(15,true);assertFalse(engine.error().isBlank());assertTrue(engine.failedTracks().contains(id));
        tick(16,true);assertNull(engine.overrideId());tick(20,true);assertEquals("day_outside",engine.sourcePlaylistId());
        audio.fail=null;engine.retryFailed();assertTrue(engine.error().isBlank());force(21,manual,OverrideMode.ONE_SONG);tick(23,true);assertEquals(manual.id,engine.sourcePlaylistId());
    }
    @Test void asynchronousFailureOfOneSongReturnsAutomatic(){
        force(13,manual,OverrideMode.ONE_SONG);tick(15,true);String id=engine.current().id;engine.failed(id,"decode error",16,lib);
        assertNull(engine.overrideId());tick(20,true);assertEquals("day_outside",engine.sourcePlaylistId());
    }
    @Test void previewCancelsOverrideAndThenReturnsAutomatic(){
        force(13,manual,OverrideMode.UNTIL_RELEASED);tick(15,true);var preview=lib.track(manual.tracks.keySet().iterator().next());engine.preview(preview,16,false);
        assertNull(engine.overrideId());assertTrue(engine.previewing());audio.current=null;tick(17,true);tick(27,true);assertEquals("day_outside",engine.sourcePlaylistId());
    }
    @Test void forcedCombatRetainsConfiguredDeathPolicy(){
        force(13,lib.playlist("combat"),OverrideMode.ONE_SONG);tick(15,true);lib.settings.combatDeathMusic=DeathMusic.CUT;
        engine.tick(16,lib,context(true,false,true),Set.of());assertNull(engine.current());assertNull(engine.overrideId());
    }
    @Test void deathPreventionAlsoAppliesToAnExplicitRequest(){
        engine.reset();force(13,manual,OverrideMode.ONE_SONG);engine.tick(13,lib,context(true,false,true),Set.of());assertNull(engine.current());
        tick(14,true);assertEquals(manual.id,engine.sourcePlaylistId());
    }
    @Test void resetClearsOverrideButNotPersistedBindings(){
        force(13,manual,OverrideMode.UNTIL_RELEASED);tick(15,true);engine.reset();assertNull(engine.overrideId());assertNull(engine.current());assertEquals("combat",lib.settings.overrideSlots[0].playlistId);
    }
    @Test void zeroGapDoesNotRestartTheCompletedOneSongOverride(){
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;lib.settings.bufferSeconds=0;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0;
        force(13,manual,OverrideMode.ONE_SONG);tick(13,true);audio.current=null;tick(14,true);
        assertNull(engine.overrideId());assertEquals("day_outside",engine.sourcePlaylistId());
    }
    @Test void normalAndForcedPlaybackShareTheSamePlaylistShuffleBag(){
        String first=engine.current().id;force(13,lib.playlist("day_outside"),OverrideMode.ONE_SONG);tick(15,true);assertNotEquals(first,engine.current().id);
    }
    @Test void overrideResolutionDoesNotTraverseSeventyThousandExtraTracks(){
        var tracks=new LibraryIndexTest.GuardedTracks();tracks.addAll(lib.tracks);lib.tracks=tracks;for(int i=0;i<70000;i++)tracks.add(new Track("extra"+i,"file","Extra",false));lib.rebuildTrackIndex();tracks.locked=true;
        force(13,manual,OverrideMode.UNTIL_RELEASED);for(int i=13;i<100;i++)tick(i,true);assertEquals(manual.id,engine.sourcePlaylistId());
    }
}

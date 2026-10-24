package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ContinuePlayingOnMatchTest {
    @TempDir Path directory;
    private final Library lib=new Library();
    private final CountingAudio audio=new CountingAudio();
    private final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
    private final Playlist a=playlist("A"),b=playlist("B"),c=playlist("C");
    private Set<String> vanilla=Set.of();

    private static final class CountingAudio extends PlaybackEngineTest.FakeAudio {
        int starts,stops;
        @Override public void start(Track t)throws Exception{super.start(t);starts++;}
        @Override public void stop(){super.stop();stops++;}
    }
    private Playlist playlist(String name){
        var p=lib.createPlaylist(name);p.conditions.biomeEnabled=true;p.conditions.biomes.add("test:"+name);
        lib.tracks.add(new Track(name,"file",name,false));p.tracks.put(name,true);return p;
    }
    private Context context(Playlist where,boolean dead){
        return new Context("minecraft:overworld","test:"+where.name,false,false,6000,false,15,"world",0,80,0,0,0.8,dead,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false);
    }
    private void tick(double now,Playlist where){engine.tick(now,lib,context(where,false),vanilla);}
    private void enter(double now,Playlist where){tick(now,where);tick(now+2,where);}
    @BeforeEach void start(){
        lib.settings.continuePlayingOnMatch=true;
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;
        lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=2;lib.settings.bufferSeconds=2;
        tick(0,a);tick(10,a);tick(12,a);assertEquals("A",engine.current().id);
    }
    private void retainInB(){b.tracks.put("A",true);enter(14,b);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());}

    @Test void sharedTrackContinuesWithoutRestartOrFadeAndThenUsesNormalSilence(){
        var track=engine.current();retainInB();tick(20,b);
        assertSame(track,engine.current());assertEquals(a.id,engine.sourcePlaylistId());assertSame(b,engine.selected());
        assertEquals(1,audio.starts);assertEquals(0,audio.stops);assertEquals(1,audio.gain);
        audio.current=null;tick(21,b);assertNull(engine.current());assertEquals(10,engine.remainingSilence(21));
        tick(30.9,b);assertNull(engine.current());tick(31,b);assertEquals(b.id,engine.sourcePlaylistId());assertEquals(2,audio.starts);
    }
    @Test void disabledGlobalSettingKeepsExistingSharedTrackTransition(){
        lib.settings.continuePlayingOnMatch=false;b.tracks.put("A",true);enter(14,b);
        assertEquals(PlaybackEngine.Phase.FADING,engine.phase());tick(17,b);assertEquals(0.5,audio.gain,0.001);
        tick(18,b);assertNull(engine.current());assertEquals(10,engine.remainingSilence(18));
    }
    @Test void sameTitleWithDifferentLibraryIdDoesNotMatch(){
        lib.track("B").title=lib.track("A").title;enter(14,b);assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
    }
    @Test void disabledAssignmentDoesNotMatch(){
        b.tracks.put("A",false);enter(14,b);assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
    }
    @Test void sharedSongInEveryIncomingPlaylistCanContinueAcrossSeveralChanges(){
        retainInB();c.tracks.put("A",true);enter(18,c);
        assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());assertEquals(a.id,engine.sourcePlaylistId());assertEquals(1,audio.starts);
    }
    @Test void laterNonmatchingPlaylistResumesNormalBufferedFade(){
        retainInB();b.finishCurrentSongOnExit=true;tick(18,c);tick(19.9,c);
        assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());tick(20,c);assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
        tick(21,c);assertEquals(0.5,audio.gain,0.001);tick(22,c);assertNull(engine.current());
        tick(31.9,c);assertNull(engine.current());tick(32,c);assertEquals(c.id,engine.sourcePlaylistId());
    }
    @Test void laterNonmatchStillHonorsOriginalSourceExitProtection(){
        retainInB();a.finishCurrentSongOnExit=true;enter(18,c);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
    }
    @Test void laterNonmatchStillHonorsIncomingEntryProtection(){
        retainInB();c.finishCurrentSong=true;enter(18,c);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
    }
    @Test void noMatchingPlaylistUsesNormalTransition(){
        retainInB();c.enabled=false;enter(18,c);assertNull(engine.selected());assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
    }
    @Test void returningToTheOriginalPlaylistKeepsItsSong(){
        retainInB();enter(18,a);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());assertEquals(1,audio.starts);
    }
    @Test void nextDrawUsesTheCurrentSituationAfterTheGap(){
        retainInB();audio.current=null;tick(21,b);enter(22,c);tick(31,c);assertEquals(c.id,engine.sourcePlaylistId());
    }
    @Test void applicableAutomaticVanillaMembershipAlsoMatches(){
        lib.track("A").vanilla=true;b.includeVanilla=true;vanilla=Set.of("A");enter(14,b);
        assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());assertEquals(1,audio.starts);
        assertTrue(PlaybackInspector.waiting(engine,lib,context(b,false),vanilla,16).contains("Continue Playing on Match"));
    }
    @Test void explicitDisableOverridesAutomaticVanillaMembership(){
        lib.track("A").vanilla=true;b.includeVanilla=true;vanilla=Set.of("A");b.tracks.put("A",false);enter(14,b);
        assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
    }
    @Test void inapplicableAutomaticVanillaMembershipDoesNotMatch(){
        lib.track("A").vanilla=true;b.includeVanilla=true;enter(14,b);assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
    }
    @ParameterizedTest @EnumSource(OverrideMode.class)
    void bothOverrideModesInterruptSharedMusicAndTakePriorityOverAutomaticChanges(OverrideMode mode){
        retainInB();c.tracks.put("A",true);c.finishCurrentSong=true;a.finishCurrentSongOnExit=true;
        assertTrue(engine.overridePlaylist(c.id,mode,17,lib,vanilla));tick(17,a);assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
        tick(18,a);assertEquals(0.5,audio.gain,0.001);tick(19,a);assertEquals(c.id,engine.sourcePlaylistId());
        tick(22,b);assertSame(b,engine.automaticSelected());assertSame(c,engine.selected());assertEquals(c.id,engine.overrideId());
    }
    @Test void returningFromAHeldOverrideUsesAutomaticMatchRules(){
        a.tracks.put("C",true);assertTrue(engine.overridePlaylist(c.id,OverrideMode.UNTIL_RELEASED,13,lib,vanilla));
        tick(15,a);assertEquals("C",engine.current().id);engine.returnAutomatic();enter(16,a);
        assertNull(engine.overrideId());assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());assertEquals(c.id,engine.sourcePlaylistId());
    }
    @Test void manualSkipAndNextStillInterruptAMatchedSong(){
        retainInB();engine.skip(17,lib);assertNull(engine.current());assertEquals(10,engine.remainingSilence(17));
        assertTrue(engine.nextSong(18,lib,context(c,false),vanilla));tick(18,c);assertEquals(c.id,engine.sourcePlaylistId());
    }
    @Test void combatDeathCutTakesPrecedenceOverMatchProtection(){
        a.conditions.combat=PlaylistConditions.State.YES;a.conditions.matchAny=true;
        engine.reset();lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;
        var combatContext=new Context("minecraft:overworld","test:A",true,false,6000,false,15,"world",0,80,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false);
        engine.tick(0,lib,combatContext,vanilla);engine.tick(2,lib,combatContext,vanilla);retainInB();
        lib.settings.combatDeathMusic=DeathMusic.CUT;engine.tick(17,lib,context(b,true),vanilla);assertNull(engine.current());
    }
    @Test void inspectorExplainsSharedTrackRetention(){
        retainInB();assertEquals("Continue Playing on Match: current song is eligible in B",PlaybackInspector.waiting(engine,lib,context(b,false),vanilla,16));
        enter(18,c);assertTrue(PlaybackInspector.waiting(engine,lib,context(c,false),vanilla,20).contains("Fading out"));
    }
    @Test void matchingDoesNotWalkSeventyThousandLibraryTracks(){
        var tracks=new LibraryIndexTest.GuardedTracks();tracks.addAll(lib.tracks);lib.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("unused"+i,"file","Unused",false));lib.rebuildTrackIndex();tracks.locked=true;
        retainInB();tick(100,b);assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
    }
    @Test void settingDefaultsOffAndOwnedSnapshotPersistsItsValue()throws Exception{
        assertFalse(new Settings().continuePlayingOnMatch);var copy=lib.snapshot();lib.settings.continuePlayingOnMatch=false;
        assertTrue(copy.settings.continuePlayingOnMatch);
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(copy);assertTrue(store.load().settings.continuePlayingOnMatch);}
    }
    @Test void indexedMembershipAgreesWithPlaybackPool(){
        lib.tracks.add(new Track("vanilla","resource","Vanilla",true));b.includeVanilla=true;
        b.tracks.put("A",true);b.tracks.put("B",false);b.tracks.put("missing",true);
        var applicable=Set.of("vanilla","A","unknown");var failed=Set.of("C");
        var expected=new HashSet<>(lib.pool(b,applicable,false).stream().map(t->t.id).toList());expected.removeAll(failed);
        for(String id:List.of("A","B","C","missing","vanilla","unknown"))assertEquals(expected.contains(id),lib.isPlayable(b,id,applicable,failed),id);
        assertFalse(lib.isPlayable(null,"A",applicable,failed));
    }
}

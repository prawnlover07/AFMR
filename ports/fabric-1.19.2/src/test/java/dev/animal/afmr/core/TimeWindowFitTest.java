package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.Weather;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TimeWindowFitTest {
    @TempDir Path directory;
    private final Settings settings=new Settings();
    private Context context(int time){
        return new Context("minecraft:overworld","minecraft:plains",false,false,time,false,15,"world",0,80,0,0,0.8,false,
            StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    }
    private PlaylistConditions window(int start,int end){
        var c=new PlaylistConditions();c.timeEnabled=true;c.fitSongsToTimeWindow=true;c.startTime=start;c.endTime=end;return c;
    }
    private Library library(){var lib=new Library();lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=lib.settings.bufferSeconds=lib.settings.fadeOutSeconds=0;return lib;}
    private Playlist playlist(Library lib,PlaylistConditions c){var p=lib.createPlaylist("Timed");p.conditions=c;return p;}
    private Track song(Library lib,Playlist p,String id,double duration){var t=new Track(id,"file",id,false);t.durationSeconds=duration;lib.tracks.add(t);p.tracks.put(id,true);return t;}
    private PlaybackEngine engine(PlaybackEngineTest.FakeAudio audio){return new PlaybackEngine(audio,new Random(1));}

    @Test void inclusiveEndAndOneTickWindowsUseNaturalTwentyTicksPerSecond(){
        var c=window(0,11999);assertEquals(600,c.songTimeBudget(context(0),settings));
        assertEquals(300,c.songTimeBudget(context(6000),settings));assertEquals(0.05,c.songTimeBudget(context(11999),settings));
        assertEquals(0,c.songTimeBudget(context(12000),settings));
        c=window(6000,6000);assertEquals(0.05,c.songTimeBudget(context(6000),settings));
    }
    @Test void wrappedWindowsContinueAcrossMidnight(){
        var c=window(22000,2000);
        assertEquals(200.05,c.songTimeBudget(context(22000),settings),1e-9);
        assertEquals(50.05,c.songTimeBudget(context(1000),settings),1e-9);
        assertEquals(0,c.songTimeBudget(context(10000),settings));
    }
    @Test void offDisabledAndAllDayOptionsDoNotRestrictDuration(){
        var c=window(0,23999);assertEquals(Double.POSITIVE_INFINITY,c.songTimeBudget(context(23000),settings));
        c=window(0,11999);c.fitSongsToTimeWindow=false;assertEquals(Double.POSITIVE_INFINITY,c.songTimeBudget(context(6000),settings));
        c.fitSongsToTimeWindow=true;c.timeEnabled=false;assertEquals(Double.POSITIVE_INFINITY,c.songTimeBudget(context(15000),settings));
    }
    @Test void anyModeDoesNotRestrictAMatchingNonTimeAlternative(){
        var c=window(0,11999);c.matchAny=true;c.weatherEnabled=true;c.weather=Weather.CLEAR;
        assertEquals(Double.POSITIVE_INFINITY,c.songTimeBudget(context(6000),settings));
        c.weather=Weather.RAIN;assertEquals(300,c.songTimeBudget(context(6000),settings));
    }
    @Test void allModeUsesEarliestRequiredEndAndAnyModeUsesContinuousUnion(){
        var all=new PlaylistConditions();all.groups.add(window(0,11999));all.groups.add(window(6000,17999));
        assertEquals(300,all.songTimeBudget(context(6000),settings));
        all.matchAny=true;assertEquals(600,all.songTimeBudget(context(6000),settings));
        all.groups.get(1).startTime=12000;assertEquals(900,all.songTimeBudget(context(0),settings));
        all.groups.get(1).startTime=12001;assertEquals(600,all.songTimeBudget(context(0),settings)); // one-tick gap
    }
    @Test void complementaryAnyWindowsNeverClose(){
        var c=new PlaylistConditions();c.matchAny=true;c.groups.add(window(0,11999));c.groups.add(window(12000,23999));
        assertEquals(Double.POSITIVE_INFINITY,c.songTimeBudget(context(6000),settings));
    }
    @Test void notGroupsFitUntilTheirForbiddenWindowStarts(){
        var c=window(6000,11999);c.negated=true;
        assertEquals(300,c.songTimeBudget(context(0),settings));assertEquals(900,c.songTimeBudget(context(12000),settings));
        var parent=new PlaylistConditions();parent.groups.add(c);assertEquals(300,parent.songTimeBudget(context(0),settings));
        c.enabled=false;assertEquals(Double.POSITIVE_INFINITY,parent.songTimeBudget(context(0),settings));
    }
    @Test void picksAFittingSongWithoutDrawingTooLongOrDisabledSongs(){
        var lib=library();var p=playlist(lib,window(0,11999));song(lib,p,"long",301);var shortSong=song(lib,p,"short",300);
        song(lib,p,"disabled",1);p.tracks.put("disabled",false);
        var audio=new PlaybackEngineTest.FakeAudio();var e=engine(audio);e.tick(0,lib,context(6000),Set.of());
        assertSame(shortSong,e.current());assertFalse(e.waitingForTimeFit());
    }
    @Test void noFittingSongKeepsPriorityAndSilenceUntilTheWindowEnds(){
        var lib=library();var p=playlist(lib,window(0,11999));song(lib,p,"long",700);
        var fallback=new Playlist("fallback","Fallback");lib.playlists.add(fallback);song(lib,fallback,"ambient",100);
        var e=engine(new PlaybackEngineTest.FakeAudio());e.tick(0,lib,context(6000),Set.of());
        assertNull(e.current());assertSame(p,e.selected());assertTrue(e.waitingForTimeFit());
        e.tick(300,lib,context(11999),Set.of());assertNull(e.current());assertSame(p,e.selected());
        e.tick(301,lib,context(12000),Set.of());assertEquals("ambient",e.current().id);assertFalse(e.waitingForTimeFit());
    }
    @Test void checksRemainingTimeAfterSilenceAndBufferNotAtInitialSelection(){
        var lib=library();lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.bufferSeconds=5;
        var p=playlist(lib,window(0,11999));song(lib,p,"song",295);
        var e=engine(new PlaybackEngineTest.FakeAudio());e.tick(0,lib,context(6000),Set.of());assertNull(e.current());
        e.tick(10,lib,context(6200),Set.of());assertNull(e.current());assertTrue(e.waitingForTimeFit());
    }
    @Test void unknownDurationsAreRequestedOnceAndCanBecomeEligible(){
        var lib=library();var p=playlist(lib,window(0,11999));var unknown=song(lib,p,"unknown",0);
        var e=engine(new PlaybackEngineTest.FakeAudio());e.tick(0,lib,context(6000),Set.of());
        assertNull(e.current());assertSame(unknown,e.takeDurationRequest());
        e.tick(1,lib,context(6020),Set.of());assertNull(e.takeDurationRequest());
        unknown.durationSeconds=100;e.playNow(1);e.tick(1,lib,context(6020),Set.of());assertSame(unknown,e.current());
    }
    @Test void failedLengthDiscoveryDoesNotDisableManualPlaybackAndRetryIsExplicit(){
        var lib=library();var p=playlist(lib,window(0,11999));var unknown=song(lib,p,"unknown",Double.NaN);
        var e=engine(new PlaybackEngineTest.FakeAudio());e.tick(0,lib,context(6000),Set.of());assertSame(unknown,e.takeDurationRequest());
        e.tick(2,lib,context(6040),Set.of());assertNull(e.takeDurationRequest());assertTrue(e.failedTracks().isEmpty());
        e.retryFailed();e.tick(3,lib,context(6060),Set.of());assertSame(unknown,e.takeDurationRequest());
        assertTrue(e.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED,3,lib,Set.of()));e.tick(3,lib,context(6060),Set.of());assertSame(unknown,e.current());
    }
    @Test void overridesPreviewsAndUnrestrictedPlaylistsAllowUnknownOrLongSongs(){
        var lib=library();var p=playlist(lib,window(0,11999));var longSong=song(lib,p,"long",900);
        var e=engine(new PlaybackEngineTest.FakeAudio());assertTrue(e.overridePlaylist(p.id,OverrideMode.ONE_SONG,0,lib,Set.of()));
        e.tick(0,lib,context(11999),Set.of());assertSame(longSong,e.current());
        e.reset();e.preview(longSong,1);assertSame(longSong,e.current());
        p.conditions.fitSongsToTimeWindow=false;longSong.durationSeconds=0;e.reset();e.tick(0,lib,context(11999),Set.of());assertSame(longSong,e.current());
    }
    @Test void nextDoesNotBypassAutomaticFitting(){
        var lib=library();var p=playlist(lib,window(0,11999));song(lib,p,"long",900);
        var e=engine(new PlaybackEngineTest.FakeAudio());assertTrue(e.nextSong(0,lib,context(6000),Set.of()));
        e.tick(0,lib,context(6000),Set.of());assertNull(e.current());assertTrue(e.waitingForTimeFit());
    }
    @Test void playingSongIsNotRecheckedWhenTimeJumpsOrItsWindowShrinks(){
        var lib=library();var p=playlist(lib,window(0,11999));var song=song(lib,p,"song",300);
        var e=engine(new PlaybackEngineTest.FakeAudio());e.tick(0,lib,context(6000),Set.of());assertSame(song,e.current());
        e.tick(1,lib,context(11999),Set.of());assertSame(song,e.current());assertEquals(PlaybackEngine.Phase.PLAYING,e.phase());
    }
    @Test void pauseResumeRechecksNewSongsAgainstLiveTime(){
        var lib=library();var p=playlist(lib,window(0,11999));var song=song(lib,p,"song",300);
        var e=engine(new PlaybackEngineTest.FakeAudio());e.setPaused(true,0);e.tick(1,lib,context(6000),Set.of());assertNull(e.current());
        e.setPaused(false,10);e.playNow(10);e.tick(10,lib,context(6200),Set.of());assertNull(e.current());assertTrue(e.waitingForTimeFit());
        song.durationSeconds=200;e.playNow(11);e.tick(11,lib,context(6220),Set.of());assertSame(song,e.current());
    }
    @Test void shortSongsKeepShuffleCyclesWithoutConsumingLongCandidates(){
        var lib=library();var p=playlist(lib,window(0,11999));song(lib,p,"long",900);song(lib,p,"a",10);song(lib,p,"b",10);
        var audio=new PlaybackEngineTest.FakeAudio();var e=engine(audio);var played=new ArrayList<String>();
        for(int i=0;i<4;i++){e.tick(i,lib,context(6000+i*20),Set.of());played.add(e.current().id);audio.current=null;}
        assertEquals(Set.of("a","b"),new HashSet<>(played.subList(0,2)));assertEquals(Set.of("a","b"),new HashSet<>(played.subList(2,4)));assertNotEquals(played.get(1),played.get(2));
    }
    @Test void vanillaCandidatesWithUnknownLengthsAreCheckedToo(){
        var lib=library();var p=playlist(lib,window(0,11999));p.includeVanilla=true;
        var vanilla=new Track("vanilla:test","minecraft:music/test","Vanilla",true);lib.tracks.add(vanilla);
        var e=engine(new PlaybackEngineTest.FakeAudio());e.tick(0,lib,context(6000),Set.of(vanilla.id));assertSame(vanilla,e.takeDurationRequest());
    }
    @Test void snapshotCloneAndJsonOwnTheTimeOption()throws Exception{
        var lib=library();var p=playlist(lib,new PlaylistConditions());p.conditions.groups.add(window(22000,2000));
        var clone=lib.clonePlaylist(p.id,"Copy");clone.conditions.groups.getFirst().fitSongsToTimeWindow=false;
        var copy=lib.snapshot();p.conditions.groups.getFirst().fitSongsToTimeWindow=false;
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(copy);var restored=store.load();
            assertTrue(restored.playlist(p.id).conditions.groups.getFirst().fitSongsToTimeWindow);
            assertFalse(restored.playlist(clone.id).conditions.groups.getFirst().fitSongsToTimeWindow);
            assertTrue(restored.playlist(p.id).conditions.summary().contains("group"));
        }
        assertFalse(new PlaylistConditions().fitSongsToTimeWindow);
    }
    @Test void inspectorExplainsWhyTheSelectedPlaylistIsSilent(){
        var lib=library();var p=playlist(lib,window(0,11999));song(lib,p,"long",900);
        var e=engine(new PlaybackEngineTest.FakeAudio());e.tick(0,lib,context(6000),Set.of());
        assertTrue(PlaybackInspector.waiting(e,lib,context(6000),Set.of(),0).contains("fits the remaining time window"));
        assertTrue(p.conditions.summary().contains("fit songs"));
    }
    @Test void fittingNeverTraversesUnrelatedLibraryEntries(){
        var lib=library();var tracks=new LibraryIndexTest.GuardedTracks();lib.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("t"+i,"file","Song",false));
        var p=playlist(lib,window(0,11999));p.tracks.put("t69999",true);tracks.getLast().durationSeconds=10;lib.rebuildTrackIndex();tracks.locked=true;
        var e=engine(new PlaybackEngineTest.FakeAudio());e.tick(0,lib,context(6000),Set.of());assertEquals("t69999",e.current().id);
    }
    @Test void budgetsAgreeWithIndependentTickByTickTruthForNestedAllAnyNot(){
        var random=new Random(51);int[] edges={0,1,5999,6000,11999,12000,18000,23999};
        for(int i=0;i<12;i++){
            var root=new PlaylistConditions();root.matchAny=random.nextBoolean();root.negated=random.nextBoolean();
            for(int j=0;j<2;j++){
                var branch=window(edges[random.nextInt(edges.length)],edges[random.nextInt(edges.length)]);
                branch.negated=random.nextBoolean();branch.matchAny=random.nextBoolean();
                branch.groups.add(window(edges[random.nextInt(edges.length)],edges[random.nextInt(edges.length)]));root.groups.add(branch);
            }
            for(int time:new int[]{0,6000,13000,23999}){
                if(!root.matches(context(time),settings))continue;
                double expected=Double.POSITIVE_INFINITY;
                for(int tick=1;tick<24000;tick++)if(!root.matches(context((time+tick)%24000),settings)){expected=tick/20.0;break;}
                assertEquals(expected,root.songTimeBudget(context(time),settings),"Tree "+i+" at "+time);
            }
        }
    }
    @Test void changingMusicFrequencyDoesNotLetACompletedLengthCheckBypassTheNewGap(){
        var lib=library();var p=playlist(lib,window(0,11999));song(lib,p,"unknown",0);
        var e=engine(new PlaybackEngineTest.FakeAudio());e.tick(0,lib,context(6000),Set.of());assertTrue(e.waitingForTimeFit());
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=600;e.rescheduleSilence(1,lib);
        assertFalse(e.waitingForTimeFit());assertEquals(600,e.remainingSilence(1));
    }
}

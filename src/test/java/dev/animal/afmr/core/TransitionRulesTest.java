package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import dev.animal.afmr.library.PlaylistPresets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TransitionRulesTest {
    @TempDir Path temp;
    static Context context(String biome,boolean combat,boolean dead){return new Context("minecraft:overworld",biome,combat,false,6000,false,15,"world",0,70,0,0,.8,dead,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,NearbyPlayers.EMPTY,false);}
    static final class Scenario {
        final Library lib=new Library();
        final Playlist a=new Playlist("a","Dark"),b=new Playlist("b","Underground"),c=new Playlist("c","Outside");
        final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
        final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
        Scenario(){
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=lib.settings.bufferSeconds=0;
            lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0;
            b.conditions.biomeEnabled=c.conditions.biomeEnabled=true;b.conditions.biomes.add("b");c.conditions.biomes.add("c");
            lib.playlists.addAll(List.of(c,b,a));
            for(var p:lib.playlists){p.includeVanilla=false;var t=new Track(p.id,"generated",p.name,false);lib.tracks.add(t);p.tracks.put(t.id,true);}
        }
        TransitionRule rule(Playlist to,TransitionRule.Finish finish){var r=new TransitionRule();r.finish=finish;a.transitions.put(to.id,r);return r;}
        void tick(double now,String biome){engine.tick(now,lib,context(biome,false,false),Set.of());}
        void start(){tick(0,"a");tick(.1,"a");assertEquals(a.id,engine.sourcePlaylistId());}
    }
    @Test void defaultRulePreservesOrdinaryFinishSettings(){var s=new Scenario();s.a.finishCurrentSongOnExit=true;s.rule(s.b,TransitionRule.Finish.INHERIT);s.start();s.tick(1,"b");assertEquals(s.a.id,s.engine.sourcePlaylistId());}
    @Test void finishExceptionAppliesOnlyToItsDestination(){var s=new Scenario();s.rule(s.b,TransitionRule.Finish.FINISH);s.rule(s.c,TransitionRule.Finish.INTERRUPT);s.start();s.tick(1,"b");assertEquals(s.a.id,s.engine.sourcePlaylistId());s.b.finishCurrentSongOnExit=true;s.tick(2,"c");assertEquals(s.c.id,s.engine.sourcePlaylistId());}
    @Test void interruptOverridesBothFinishSwitchesAndContinueOnMatch(){var s=new Scenario();s.a.finishCurrentSongOnExit=s.b.finishCurrentSong=s.lib.settings.continuePlayingOnMatch=true;s.b.tracks.put("a",true);s.rule(s.b,TransitionRule.Finish.INTERRUPT);s.start();s.tick(1,"b");assertEquals(s.b.id,s.engine.sourcePlaylistId());}
    @Test void rulesAreDirectional(){var s=new Scenario();s.rule(s.b,TransitionRule.Finish.FINISH);s.tick(0,"b");s.tick(.1,"b");s.tick(1,"a");assertEquals(s.a.id,s.engine.sourcePlaylistId());}
    @Test void customBufferOutGapAndInApplyOnce(){
        var s=new Scenario();var r=s.rule(s.b,TransitionRule.Finish.INTERRUPT);r.customBuffer=r.customFadeOut=r.customSilence=r.customFadeIn=true;
        r.bufferSeconds=2;r.fadeOutSeconds=2;r.silenceMinSeconds=r.silenceMaxSeconds=3;r.fadeInSeconds=4;s.start();
        s.tick(1,"b");assertEquals(2,s.engine.remainingBuffer(1,s.lib.settings));s.tick(2,"b");assertEquals(PlaybackEngine.Phase.PLAYING,s.engine.phase());
        s.tick(3,"b");assertEquals(PlaybackEngine.Phase.FADING,s.engine.phase());s.tick(4,"b");assertEquals(.5,s.audio.gain,.001);
        s.tick(5,"b");assertEquals(3,s.engine.remainingSilence(5));s.tick(8,"b");assertEquals(s.b.id,s.engine.sourcePlaylistId());
        s.tick(10,"b");assertEquals(.5,s.audio.gain,.001);s.audio.stop();s.tick(11,"b");s.tick(11.1,"b");assertEquals(1,s.audio.gain,.001);
    }
    @Test void explicitZeroBufferAndSilenceOverrideTriggerTimingIndependently(){
        var s=new Scenario();s.start();s.lib.settings.bufferSeconds=20;s.lib.settings.silenceMinSeconds=s.lib.settings.silenceMaxSeconds=30;
        var r=s.rule(s.b,TransitionRule.Finish.INTERRUPT);r.customBuffer=r.customSilence=true;s.tick(1,"b");assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void shorterPairBufferDoesNotDelayLaterSongsWithTheOldGlobalBuffer(){var s=new Scenario();s.start();s.lib.settings.bufferSeconds=60;var r=s.rule(s.b,TransitionRule.Finish.INTERRUPT);r.customBuffer=true;s.tick(1,"b");assertEquals("b",s.engine.sourcePlaylistId());s.audio.stop();s.tick(2,"b");assertEquals("b",s.engine.sourcePlaylistId());}
    @Test void playOnTriggerStillHonorsExplicitBufferAndGap(){
        var s=new Scenario();s.b.playOnTrigger=true;var r=s.rule(s.b,TransitionRule.Finish.INTERRUPT);r.customBuffer=r.customSilence=true;r.bufferSeconds=2;r.silenceMinSeconds=r.silenceMaxSeconds=3;s.start();
        s.tick(1,"b");assertEquals(s.a.id,s.engine.sourcePlaylistId());s.tick(3,"b");assertEquals(3,s.engine.remainingSilence(3));s.tick(6,"b");assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void triggerCanSkipInheritedGapAfterExplicitBuffer(){
        var s=new Scenario();s.start();s.b.playOnTrigger=true;s.lib.settings.silenceMinSeconds=s.lib.settings.silenceMaxSeconds=30;
        var r=s.rule(s.b,TransitionRule.Finish.INTERRUPT);r.customBuffer=true;r.bufferSeconds=2;s.tick(1,"b");s.tick(3,"b");assertEquals(s.b.id,s.engine.sourcePlaylistId());
    }
    @Test void finishThenCustomGapUsesOutgoingOwnership(){var s=new Scenario();var r=s.rule(s.b,TransitionRule.Finish.FINISH);r.customSilence=true;r.silenceMinSeconds=r.silenceMaxSeconds=7;s.start();s.tick(1,"b");s.audio.stop();s.tick(2,"b");assertEquals(7,s.engine.remainingSilence(2));s.tick(9,"b");assertEquals(s.b.id,s.engine.sourcePlaylistId());}
    @Test void destinationChangeDuringGapUsesElapsedSilence(){var s=new Scenario();var r=s.rule(s.b,TransitionRule.Finish.INTERRUPT);r.customSilence=true;r.silenceMinSeconds=r.silenceMaxSeconds=10;var q=s.rule(s.c,TransitionRule.Finish.INTERRUPT);q.customSilence=true;q.silenceMinSeconds=q.silenceMaxSeconds=2;s.start();s.tick(1,"b");s.tick(5,"c");assertEquals(s.c.id,s.engine.sourcePlaylistId());}
    @Test void manualNextBypassesPairRule(){var s=new Scenario();var r=s.rule(s.b,TransitionRule.Finish.FINISH);r.customBuffer=true;r.bufferSeconds=60;s.start();s.tick(1,"b");assertTrue(s.engine.nextSong(2,s.lib,context("b",false,false),Set.of()));s.tick(2,"b");assertEquals(s.b.id,s.engine.sourcePlaylistId());assertNull(s.engine.transitionRule());}
    @Test void skipUsesNormalGapRatherThanTransitionGap(){var s=new Scenario();var r=s.rule(s.b,TransitionRule.Finish.FINISH);r.customSilence=true;r.silenceMinSeconds=r.silenceMaxSeconds=50;s.start();s.tick(1,"b");s.lib.settings.silenceMinSeconds=s.lib.settings.silenceMaxSeconds=4;s.engine.skip(2,s.lib);assertEquals(4,s.engine.remainingSilence(2));}
    @Test void queueKeepsNormalSilenceAndBypassesRules(){var s=new Scenario();s.rule(s.b,TransitionRule.Finish.FINISH);s.start();assertTrue(s.engine.enqueue("b","b",s.lib));s.lib.settings.silenceMinSeconds=s.lib.settings.silenceMaxSeconds=3;s.audio.stop();s.tick(1,"b");assertEquals(3,s.engine.remainingSilence(1));s.tick(4,"b");assertTrue(s.engine.queuedPlaying());assertNull(s.engine.transitionRule());}
    @Test void overridesAndPreviewsIgnorePairRules(){var s=new Scenario();s.rule(s.b,TransitionRule.Finish.FINISH);s.start();assertTrue(s.engine.overridePlaylist("b",OverrideMode.UNTIL_RELEASED,1,s.lib,Set.of()));s.tick(1,"a");assertEquals("b",s.engine.sourcePlaylistId());assertNull(s.engine.transitionRule());s.engine.preview(s.lib.track("a"),2,false);s.tick(2,"b");assertTrue(s.engine.previewing());assertNull(s.engine.transitionRule());}
    @Test void combatDeathFadeHasPrecedence(){var s=new Scenario();s.a.conditions.combat=PlaylistConditions.State.YES;var r=s.rule(s.b,TransitionRule.Finish.FINISH);r.customFadeOut=true;r.fadeOutSeconds=60;s.lib.settings.combatDeathFadeSeconds=1;s.engine.tick(0,s.lib,context("a",true,false),Set.of());s.engine.tick(.1,s.lib,context("a",true,false),Set.of());s.engine.tick(1,s.lib,context("b",true,true),Set.of());s.engine.tick(2,s.lib,context("b",true,true),Set.of());assertNull(s.engine.current());assertNull(s.engine.transitionRule());}
    @Test void deathKeepPlayingPreservesGainAfterACustomIncomingFade(){
        var s=new Scenario();s.b.conditions.combat=PlaylistConditions.State.YES;var r=s.rule(s.b,TransitionRule.Finish.INTERRUPT);r.customFadeIn=true;r.fadeInSeconds=4;
        s.engine.tick(0,s.lib,context("a",true,false),Set.of());s.engine.tick(1,s.lib,context("b",true,false),Set.of());s.engine.tick(3,s.lib,context("b",true,false),Set.of());assertEquals(.5,s.audio.gain,.001);
        s.lib.settings.fadeOutSeconds=4;s.engine.tick(4,s.lib,context("c",true,false),Set.of());s.engine.tick(5,s.lib,context("c",true,false),Set.of());assertEquals(.375,s.audio.gain,.001);
        s.lib.settings.combatDeathMusic=DeathMusic.KEEP_PLAYING;s.engine.tick(5.1,s.lib,context("c",true,true),Set.of());assertEquals(.375,s.audio.gain,.001);assertTrue(s.engine.finishingAfterDeath());
    }
    @Test void pauseHoldsBufferAndGap(){var s=new Scenario();var r=s.rule(s.b,TransitionRule.Finish.INTERRUPT);r.customBuffer=r.customSilence=true;r.bufferSeconds=2;r.silenceMinSeconds=r.silenceMaxSeconds=3;s.start();s.tick(1,"b");s.engine.setPaused(true,2);s.engine.setPaused(false,12);s.tick(12,"b");assertEquals(1,s.engine.remainingBuffer(12,s.lib.settings));s.tick(13,"b");assertEquals(3,s.engine.remainingSilence(13));s.tick(16,"b");assertEquals("b",s.engine.sourcePlaylistId());}
    @Test void snapshotsAndProfileCopiesOwnTheirRules(){var s=new Scenario();s.rule(s.b,TransitionRule.Finish.FINISH);var copy=s.lib.snapshot();var profile=s.lib.profiles.create(s.lib,"Copy","default");s.a.transitions.get("b").finish=TransitionRule.Finish.INTERRUPT;assertEquals(TransitionRule.Finish.FINISH,copy.playlist("a").transitions.get("b").finish);s.lib.profiles.activate(s.lib,profile);assertEquals(TransitionRule.Finish.FINISH,s.lib.playlist("a").transitions.get("b").finish);}
    @Test void deletionClearsIncomingReferences(){var s=new Scenario();s.rule(s.b,TransitionRule.Finish.FINISH);s.lib.removePlaylist("b");assertTrue(s.a.transitions.isEmpty());}
    @Test void diskStorageKeepsDirectionalRules()throws Exception{var s=new Scenario();s.rule(s.b,TransitionRule.Finish.FINISH);var file=temp.resolve("library.json");try(var store=new LibraryStore(file)){store.save(s.lib.snapshot());}try(var store=new LibraryStore(file)){assertEquals(TransitionRule.Finish.FINISH,store.load().playlist("a").transitions.get("b").finish);}}
    @Test void sharingRemapsIncludedDestinationsAndOmitsOthers()throws Exception{
        var s=new Scenario();s.rule(s.b,TransitionRule.Finish.FINISH);s.rule(s.c,TransitionRule.Finish.INTERRUPT);
        var data=PlaylistPresets.capture(s.lib,Set.of("a","b"),false);var file=temp.resolve("rules.afmrp");PlaylistPresets.write(file,data);data=PlaylistPresets.read(file);
        var target=new Library();var added=PlaylistPresets.apply(target,"default","","",PlaylistPresets.Placement.TOP,data,"",null).ids();
        var a=target.playlist(added.get(1));assertEquals(1,a.transitions.size());assertEquals(TransitionRule.Finish.FINISH,a.transitions.get(added.get(0)).finish);
        assertTrue(PlaylistPresets.capture(s.lib,Set.of("a"),false).playlists.get(0).transitions.isEmpty());
    }
    @Test void malformedRulesAreRejectedBeforeImport(){var s=new Scenario();var r=s.rule(s.b,TransitionRule.Finish.FINISH);r.fadeInSeconds=Double.NaN;assertThrows(IllegalArgumentException.class,()->PlaylistPresets.capture(s.lib,Set.of("a","b"),false));r.fadeInSeconds=0;r.finish=null;assertThrows(IllegalArgumentException.class,()->s.lib.normalize());}
    @Test void unknownRuleFieldsAreRejected()throws Exception{var s=new Scenario();s.rule(s.b,TransitionRule.Finish.FINISH);var file=temp.resolve("rules.afmrp");PlaylistPresets.write(file,PlaylistPresets.capture(s.lib,Set.of("a","b"),false));Files.writeString(file,Files.readString(file).replace("\"finish\": \"FINISH\"","\"finish\": \"FINISH\", \"typo\": true"));assertThrows(java.io.IOException.class,()->PlaylistPresets.read(file));}
}

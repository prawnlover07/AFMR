package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Settings;
import dev.animal.afmr.core.CombatOpponent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatTargetingTest {
    private Settings settings(){var s=new Settings();s.combatQuietSeconds=5;return s;}
    @Test void targetingAloneCannotStartCombat(){
        var t=new CombatTracker();var s=settings();
        for(int i=0;i<100;i++){t.targeting(i,true,s);assertFalse(t.active(i,s));assertFalse(t.playersActive(i,s));}
    }
    @Test void aQualifyingAttackStartsCombatAndTargetingKeepsItActive(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);t.targeting(2,true,s);
        for(int i=3;i<100;i++){t.targeting(i,true,s);assertTrue(t.active(i,s));}
        assertFalse(t.playersActive(99,s));
    }
    @Test void qualifyingReceivedDamageAlsoArmsTargetRetention(){
        var t=new CombatTracker();var s=settings();t.damage(1,CombatOpponent.MOB,s);t.targeting(2,true,s);
        t.targeting(30,true,s);assertTrue(t.active(30,s));
    }
    @Test void losingTheLastTargetStartsAFreshQuietCountdown(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);t.targeting(2,true,s);
        t.targeting(100,false,s);assertTrue(t.active(104.99,s));assertFalse(t.active(105,s));
        t.targeting(104,false,s);assertFalse(t.active(105,s)); // Absence must not refresh it each tick.
    }
    @Test void evenABriefTargetHoldStartsTheCountdownWhenItEnds(){
        var t=new CombatTracker();var s=settings();t.attack(10,CombatOpponent.MOB,s);t.targeting(10.1,true,s);t.targeting(10.2,false,s);
        assertTrue(t.active(15.1,s));assertFalse(t.active(15.2,s));
    }
    @Test void switchingBetweenTargetingMobsDoesNotEndCombat(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);
        // An observation is true while any qualifying mob targets the player, irrespective of which one.
        t.targeting(2,true,s);t.targeting(20,true,s);t.targeting(40,true,s);assertTrue(t.active(40,s));
        t.targeting(41,false,s);assertFalse(t.active(46,s));
    }
    @Test void reacquiringATargetDuringTheQuietPeriodContinuesTheFight(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);t.targeting(2,true,s);t.targeting(20,false,s);
        t.targeting(24,true,s);t.targeting(100,true,s);assertTrue(t.active(100,s));
    }
    @Test void lateTargetObservationsCannotReviveAnExpiredFight(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);
        t.targeting(6,true,s);assertFalse(t.active(6,s));
        t.targeting(7,true,s);assertFalse(t.active(7,s));
        t.attack(8,CombatOpponent.MOB,s);t.targeting(8,true,s);assertTrue(t.active(100,s));
    }
    @Test void reacquiringAfterTheLossCountdownExpiresNeedsAnotherAttack(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);t.targeting(2,true,s);t.targeting(10,false,s);
        t.targeting(15,true,s);assertFalse(t.active(15,s));t.targeting(20,true,s);assertFalse(t.active(20,s));
    }
    @Test void passiveAndEnvironmentalTriggersDoNotArmRetentionByDefault(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.PASSIVE,s);t.damage(1,CombatOpponent.NONE,s);t.damage(1,CombatOpponent.PASSIVE,s);
        t.targeting(2,true,s);assertFalse(t.active(2,s));
    }
    @Test void broadTriggerModeCanArmRetentionWithoutInventingPvp(){
        var t=new CombatTracker();var s=settings();s.combatThreatsOnly=false;t.damage(1,CombatOpponent.NONE,s);t.targeting(2,true,s);
        assertTrue(t.active(100,s));assertFalse(t.playersActive(100,s));
    }
    @Test void disabledAttackAndDamageSwitchesCannotArmRetention(){
        var t=new CombatTracker();var s=settings();s.combatOnAttack=false;s.combatOnDamage=false;
        t.attack(1,CombatOpponent.MOB,s);t.damage(1,CombatOpponent.MOB,s);t.targeting(2,true,s);assertFalse(t.active(2,s));
        s.combatOnDamage=true;t.damage(3,CombatOpponent.MOB,s);t.targeting(4,true,s);assertTrue(t.active(100,s));
    }
    @Test void hostileTargetingDoesNotRefreshPlayersOnlyCombat(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.PLAYER,s);t.targeting(2,true,s);
        t.targeting(30,true,s);assertTrue(t.active(30,s));assertFalse(t.playersActive(30,s));
        t.damage(31,CombatOpponent.PLAYER,s);assertTrue(t.playersActive(35.9,s));assertFalse(t.playersActive(36,s));
        t.targeting(36,false,s);assertTrue(t.active(40.9,s));assertFalse(t.active(41,s));
    }
    @Test void globalOffDropsTheTargetHoldAndCannotArmNewCombat(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);t.targeting(2,true,s);
        s.combatEnabled=false;t.targeting(30,true,s);assertFalse(t.active(30,s));
        t.attack(31,CombatOpponent.MOB,s);s.combatEnabled=true;t.targeting(31,true,s);assertFalse(t.active(31,s));
    }
    @Test void deathRespawnAndSessionResetDropTheTargetLatch(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);t.targeting(2,true,s);t.update(true);
        t.targeting(30,true,s);assertFalse(t.active(30,s));t.update(false);t.targeting(31,true,s);assertFalse(t.active(31,s));
        t.damage(32,CombatOpponent.PLAYER,s);t.targeting(33,true,s);assertTrue(t.active(100,s));
        t.reset();t.targeting(101,true,s);assertFalse(t.active(101,s));assertFalse(t.playersActive(101,s));
    }
    @Test void unavailableOrMultiplayerTargetingKeepsTheOriginalEventTimeout(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);
        for(int i=1;i<=7;i++)t.targeting(i,false,s);
        assertFalse(t.active(7,s));assertFalse(t.playersActive(7,s));
    }
    @Test void losingSingleplayerTargetDataReleasesTheHoldInsteadOfKeepingItForever(){
        var t=new CombatTracker();var s=settings();t.attack(1,CombatOpponent.MOB,s);t.targeting(2,true,s);
        t.targeting(100,false,s);assertTrue(t.active(104,s));assertFalse(t.active(105,s));
    }
    @Test void playersTimerStillKeepsGeneralCombatWhenItsOtherTimerWasShortened(){
        var t=new CombatTracker();var s=settings();s.combatQuietSeconds=15;t.attack(1,CombatOpponent.PLAYER,s);
        s.combatQuietSeconds=1;t.damage(2,CombatOpponent.MOB,s);t.targeting(4,true,s);
        assertTrue(t.active(20,s));assertFalse(t.playersActive(20,s));
        t.targeting(21,false,s);assertFalse(t.active(22,s));
    }
}

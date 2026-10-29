package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Settings;
import dev.animal.afmr.core.CombatOpponent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatTrackerTest {
    @Test void attacksRefreshQuietPeriodOnlyWhenEnabled(){
        var t=new CombatTracker();var s=new Settings();s.combatQuietSeconds=5;t.attack(10,CombatOpponent.MOB,s);assertTrue(t.active(14.9,s));assertFalse(t.active(15,s));
        t.attack(14,CombatOpponent.MOB,s);assertTrue(t.active(18,s));s.combatOnAttack=false;t.attack(18,CombatOpponent.MOB,s);assertFalse(t.active(19,s));
    }
    @Test void passiveTargetsAndEnvironmentalDamageDoNotCountByDefault(){
        var t=new CombatTracker();var s=new Settings();assertTrue(s.combatThreatsOnly);
        t.attack(1,CombatOpponent.PASSIVE,s);t.attack(2,CombatOpponent.NONE,s);
        t.damage(3,CombatOpponent.NONE,s);t.damage(4,CombatOpponent.PASSIVE,s);t.update(false);
        assertFalse(t.active(4,s));assertFalse(t.playersActive(4,s));
    }
    @Test void qualifyingDamageRefreshesButUnrelatedDamageCannotExtendTheTimer(){
        var t=new CombatTracker();var s=new Settings();s.combatQuietSeconds=5;
        t.damage(1,CombatOpponent.MOB,s);assertTrue(t.active(5.9,s));
        t.damage(5,CombatOpponent.NONE,s);t.attack(5,CombatOpponent.PASSIVE,s);assertFalse(t.active(6,s));
        t.damage(7,CombatOpponent.PLAYER,s);assertTrue(t.active(7,s));assertTrue(t.playersActive(7,s));
    }
    @Test void triggerSwitchesAreIndependentAndGlobalOffDisablesBoth(){
        var t=new CombatTracker();var s=new Settings();s.combatOnDamage=false;t.damage(1,CombatOpponent.PLAYER,s);assertFalse(t.active(1,s));assertFalse(t.playersActive(1,s));
        t.attack(2,CombatOpponent.PLAYER,s);assertTrue(t.active(2,s));assertTrue(t.playersActive(2,s));s.combatEnabled=false;assertFalse(t.active(2,s));assertFalse(t.playersActive(2,s));
        t.reset();t.attack(3,CombatOpponent.PLAYER,s);t.damage(4,CombatOpponent.PLAYER,s);s.combatEnabled=true;assertFalse(t.active(4,s));
        s.combatOnAttack=false;s.combatOnDamage=true;t.attack(5,CombatOpponent.PLAYER,s);assertFalse(t.active(5,s));t.damage(5,CombatOpponent.PLAYER,s);assertTrue(t.playersActive(5,s));
    }
    @Test void deathAndRespawnDoNotCarryCombatOrTreatHealingAsDamage(){
        var t=new CombatTracker();var s=new Settings();t.attack(1,CombatOpponent.PLAYER,s);t.update(true);assertFalse(t.active(2,s));assertFalse(t.playersActive(2,s));
        t.attack(3,CombatOpponent.PLAYER,s);t.damage(3,CombatOpponent.PLAYER,s);assertFalse(t.active(3,s));t.update(false);assertFalse(t.active(4,s));
        t.damage(5,CombatOpponent.MOB,s);assertTrue(t.active(5,s));t.reset();assertFalse(t.active(5,s));assertFalse(t.playersActive(5,s));
    }
    @Test void generalAndPvpTimersExpireIndependently(){
        var t=new CombatTracker();var s=new Settings();s.combatQuietSeconds=5;
        t.attack(1,CombatOpponent.PLAYER,s);t.damage(5,CombatOpponent.MOB,s);
        assertTrue(t.active(6,s));assertFalse(t.playersActive(6,s));
        t.damage(7,CombatOpponent.PLAYER,s);assertTrue(t.playersActive(11.9,s));assertFalse(t.playersActive(12,s));
    }
    @Test void broadModeAllowsPassiveTargetsAndEnvironmentalDamageWithoutInventingPvp(){
        var t=new CombatTracker();var s=new Settings();s.combatThreatsOnly=false;s.combatQuietSeconds=5;
        t.attack(1,CombatOpponent.PASSIVE,s);assertTrue(t.active(1,s));assertFalse(t.playersActive(1,s));
        t.damage(5,CombatOpponent.NONE,s);assertTrue(t.active(9,s));assertFalse(t.playersActive(9,s));
        t.attack(10,CombatOpponent.NONE,s);assertFalse(t.active(10,s));
    }
    @Test void unexpiredPvpStillCountsAsGeneralCombatWhenQuietPeriodIsReduced(){
        var t=new CombatTracker();var s=new Settings();s.combatQuietSeconds=15;t.attack(1,CombatOpponent.PLAYER,s);
        s.combatQuietSeconds=1;t.damage(2,CombatOpponent.MOB,s);
        assertTrue(t.playersActive(4,s));assertTrue(t.active(4,s));assertFalse(t.active(16,s));
    }
    @Test void ordinaryStateUpdatesNeverCreateCombatEvents(){
        var t=new CombatTracker();var s=new Settings();t.update(false);t.update(false);
        assertFalse(t.active(1,s));assertFalse(t.playersActive(1,s));
    }
}

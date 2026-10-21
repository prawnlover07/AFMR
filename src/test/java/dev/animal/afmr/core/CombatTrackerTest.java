package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Settings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatTrackerTest {
    @Test void attacksRefreshQuietPeriodOnlyWhenEnabled(){
        var t=new CombatTracker();var s=new Settings();s.combatQuietSeconds=5;t.attack(10,s);assertTrue(t.active(14.9,s));assertFalse(t.active(15,s));
        t.attack(14,s);assertTrue(t.active(18,s));s.combatOnAttack=false;t.attack(18,s);assertFalse(t.active(19,s));
    }
    @Test void healthDropsAndHurtTimeTriggerDamageButHealingDoesNotRefresh(){
        var t=new CombatTracker();var s=new Settings();s.combatQuietSeconds=5;t.update(0,24,0,false,s);assertFalse(t.active(0,s));
        t.update(1,23,0,false,s);assertTrue(t.active(1,s));t.update(5,24,0,false,s);assertFalse(t.active(6,s));
        t.update(7,24,10,false,s);assertTrue(t.active(7,s));
    }
    @Test void triggerSwitchesAreIndependentAndGlobalOffDisablesBoth(){
        var t=new CombatTracker();var s=new Settings();s.combatOnDamage=false;t.update(0,20,0,false,s);t.update(1,10,10,false,s);assertFalse(t.active(1,s));
        t.attack(2,s);assertTrue(t.active(2,s));s.combatEnabled=false;assertFalse(t.active(2,s));t.reset();t.attack(3,s);t.update(4,0,10,false,s);s.combatEnabled=true;assertFalse(t.active(4,s));
    }
    @Test void deathAndRespawnDoNotCarryCombatOrTreatHealingAsDamage(){
        var t=new CombatTracker();var s=new Settings();t.attack(1,s);t.update(2,0,10,true,s);assertFalse(t.active(2,s));
        t.attack(3,s);assertFalse(t.active(3,s));t.update(4,20,0,false,s);assertFalse(t.active(4,s));
        t.update(5,19,10,false,s);assertTrue(t.active(5,s));t.reset();assertFalse(t.active(5,s));
    }
}

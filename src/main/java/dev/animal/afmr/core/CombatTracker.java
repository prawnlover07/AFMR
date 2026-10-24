package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Settings;

/** Source-qualified client observations. General combat and PvP expire independently. */
public final class CombatTracker {
    public enum Opponent { NONE, PASSIVE, MOB, PLAYER }
    private double until,playersUntil;
    private boolean dead,engaged,targeted;
    public void attack(double now,Opponent opponent,Settings settings){
        if(dead||!settings.combatEnabled||!settings.combatOnAttack||opponent==Opponent.NONE)return;
        if(settings.combatThreatsOnly&&opponent==Opponent.PASSIVE)return;
        until=now+settings.combatQuietSeconds;
        engaged=true;
        if(opponent==Opponent.PLAYER)playersUntil=until;
    }
    public void damage(double now,Opponent opponent,Settings settings){
        if(dead||!settings.combatEnabled||!settings.combatOnDamage)return;
        if(settings.combatThreatsOnly&&(opponent==Opponent.NONE||opponent==Opponent.PASSIVE))return;
        until=now+settings.combatQuietSeconds;
        engaged=true;
        if(opponent==Opponent.PLAYER)playersUntil=until;
    }
    public void update(boolean isDead){
        if(dead!=isDead){until=playersUntil=0;engaged=targeted=false;dead=isDead;}
    }
    /** Targeting sustains an event-started fight; it can never start or revive one by itself. */
    public void targeting(double now,boolean hostileTarget,Settings settings){
        if(dead||!settings.combatEnabled){engaged=targeted=false;return;}
        if(!engaged||!targeted&&now>=until&&now>=playersUntil){engaged=targeted=false;return;}
        if(targeted&&!hostileTarget)until=Math.max(until,now+settings.combatQuietSeconds);
        targeted=hostileTarget;
    }
    public boolean active(double now,Settings settings){return !dead&&settings.combatEnabled&&(targeted||now<until||now<playersUntil);}
    public boolean playersActive(double now,Settings settings){return !dead&&settings.combatEnabled&&now<playersUntil;}
    public void reset(){until=playersUntil=0;dead=engaged=targeted=false;}
}

package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Settings;

/** Client combat observations, reset on player replacement or disconnect. */
public final class CombatTracker {
    private double until;
    private float health=Float.NaN;
    private int hurtTime;
    private boolean dead;
    public void attack(double now,Settings settings){
        if(!dead&&settings.combatEnabled&&settings.combatOnAttack)until=now+settings.combatQuietSeconds;
    }
    public void update(double now,float currentHealth,int currentHurtTime,boolean isDead,Settings settings){
        if(isDead){until=0;dead=true;health=currentHealth;hurtTime=currentHurtTime;return;}
        if(dead){reset();health=currentHealth;hurtTime=currentHurtTime;return;}
        if(settings.combatEnabled&&settings.combatOnDamage&&((!Float.isNaN(health)&&currentHealth<health)||currentHurtTime>hurtTime))
            until=now+settings.combatQuietSeconds;
        health=currentHealth;hurtTime=currentHurtTime;
    }
    public boolean active(double now,Settings settings){return !dead&&settings.combatEnabled&&now<until;}
    public void reset(){until=0;health=Float.NaN;hurtTime=0;dead=false;}
}

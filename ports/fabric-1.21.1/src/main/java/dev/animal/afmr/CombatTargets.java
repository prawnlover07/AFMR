package dev.animal.afmr;

import dev.animal.afmr.core.CombatTracker.Opponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/** Client-visible hostile/neutral capabilities; never query server AI or nearby entity lists. */
public final class CombatTargets {
    private CombatTargets(){}

    public static Opponent classify(Entity entity,Player localPlayer){
        if(entity==null||entity==localPlayer)return Opponent.NONE;
        if(entity instanceof Player)return Opponent.PLAYER;
        if(dev.animal.afmr.server.CombatMobs.includes(entity))return Opponent.MOB;
        return Opponent.PASSIVE;
    }

    public static Entity attacker(DamageSource source){
        Entity entity=source.getEntity();
        if(entity==null)entity=source.getDirectEntity();
        if(entity instanceof Projectile projectile)return projectile.getOwner();
        return entity;
    }
}

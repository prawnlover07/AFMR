package dev.animal.afmr;

import dev.animal.afmr.core.CombatTracker.Opponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/** Client-visible hostile/neutral capabilities; never query server AI or nearby entity lists. */
public final class CombatTargets {
    private CombatTargets(){}

    public static Opponent classify(Entity entity,Player localPlayer){
        if(entity==null||entity==localPlayer)return Opponent.NONE;
        if(entity instanceof Player)return Opponent.PLAYER;
        if(entity instanceof Enemy||entity instanceof NeutralMob
            ||entity.getType().getCategory()==MobCategory.MONSTER
            ||entity instanceof Goat||entity instanceof Panda||entity instanceof Dolphin||entity instanceof Pufferfish||entity instanceof Llama
            ||entity instanceof Rabbit rabbit&&rabbit.getVariant()==Rabbit.Variant.EVIL)return Opponent.MOB;
        return Opponent.PASSIVE;
    }

    public static Entity attacker(DamageSource source){
        Entity entity=source.getEntity();
        if(entity==null)entity=source.getDirectEntity();
        if(entity instanceof Projectile projectile)return projectile.getOwner();
        return entity;
    }
}

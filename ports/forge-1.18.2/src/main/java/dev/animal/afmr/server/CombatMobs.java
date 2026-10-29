package dev.animal.afmr.server;

import dev.animal.afmr.core.CombatOpponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Enemy;

/** Hostile and neutral mob capabilities, shared with client event classification. */
public final class CombatMobs {
    private CombatMobs(){}
    public static CombatOpponent classify(Entity entity,net.minecraft.world.entity.player.Player observer){
        if(entity==null||entity==observer)return CombatOpponent.NONE;
        if(entity instanceof net.minecraft.world.entity.player.Player)return CombatOpponent.PLAYER;
        return includes(entity)?CombatOpponent.MOB:CombatOpponent.PASSIVE;
    }
    public static boolean includes(Entity entity){
        return entity instanceof Enemy||entity instanceof NeutralMob
            ||entity.getType().getCategory()==MobCategory.MONSTER
            ||entity instanceof Goat||entity instanceof Panda||entity instanceof Dolphin||entity instanceof Pufferfish||entity instanceof Llama
            ||entity instanceof Rabbit rabbit&&rabbit.getRabbitType()==99;
    }
}

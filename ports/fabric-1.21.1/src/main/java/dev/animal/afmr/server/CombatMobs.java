package dev.animal.afmr.server;

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
    public static boolean includes(Entity entity){
        return entity instanceof Enemy||entity instanceof NeutralMob
            ||entity.getType().getCategory()==MobCategory.MONSTER
            ||entity instanceof Goat||entity instanceof Panda||entity instanceof Dolphin||entity instanceof Pufferfish||entity instanceof Llama
            ||entity instanceof Rabbit rabbit&&rabbit.getVariant()==Rabbit.Variant.EVIL;
    }
}

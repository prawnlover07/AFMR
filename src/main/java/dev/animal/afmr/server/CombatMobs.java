package dev.animal.afmr.server;

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

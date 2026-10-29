package dev.animal.afmr.mixin;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs on the client when the server's damage packet has resolved its attacker. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method="handleDamageEvent",at=@At("TAIL"))
    private void afmr$damage(DamageSource source,CallbackInfo ci){
        if(Minecraft.getInstance().player!=null)
            ClientRuntime.get().damageObserved((LivingEntity)(Object)this,source);
    }
}

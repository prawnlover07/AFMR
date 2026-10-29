package dev.animal.afmr.server.mixin;
import dev.animal.afmr.server.ServerCombatObservations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Old clients do not receive an attributed damage packet; the server confirms it. */
@Mixin(LivingEntity.class)
public abstract class ServerLivingEntityMixin {
 @Inject(method="hurt",at=@At("RETURN"))
 private void afmr$confirmedHit(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){
  var victim=(LivingEntity)(Object)this;
  if(!victim.level.isClientSide&&amount>0&&Boolean.TRUE.equals(result.getReturnValue()))ServerCombatObservations.hurt(victim,source);
 }
}

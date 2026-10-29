package dev.animal.afmr.mixin;
import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class MinecraftCloseMixin {
    @Inject(method="close",at=@At("HEAD"))
    private void afmr$close(CallbackInfo callback){ClientRuntime.shutdown();}
}

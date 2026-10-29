package dev.animal.afmr.mixin;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
    @Inject(method="setMinutesBetweenSongs",at=@At("TAIL"))
    private void afmr$frequencyChanged(MusicManager.MusicFrequency frequency,CallbackInfo ci){
        ClientRuntime.musicFrequencyChanged(frequency);
    }
}

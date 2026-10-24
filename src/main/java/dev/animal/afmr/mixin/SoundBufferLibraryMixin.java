package dev.animal.afmr.mixin;

import dev.animal.afmr.audio.*;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.*;

@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void afmr$localStream(Identifier location, boolean looping, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        var source = GameAudio.localSource(location);
        if (source == null) return;
        cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
            try { return new LocalAudioStream(source.path(),source.replayGain(),source.loudness()); }
            catch (Exception e) {
                dev.animal.afmr.ClientRuntime.reportAudioFailure(source.path(), e);
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool()));
    }
}

package dev.animal.afmr.mixin;

import dev.animal.afmr.audio.MusicSound;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Master mute stops other sounds while AFMR's stream continues silently. */
@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    @Shadow @Final private SoundEngine soundEngine;
    @Redirect(method="updateSourceVolume",at=@At(value="INVOKE",target="Lnet/minecraft/client/sounds/SoundManager;stop()V"))
    private void afmr$masterMute(SoundManager manager){
        var state=(SoundEngineAccess)soundEngine;
        boolean music=state.afmr$channels().keySet().stream().anyMatch(MusicSound.class::isInstance);
        if(!music){manager.stop();return;}
        // This is an explicit volume-settings transition, not a steady gameplay scan.
        var channels=state.afmr$channels().entrySet().iterator();
        while(channels.hasNext()){
            var entry=channels.next();var sound=entry.getKey();if(sound instanceof MusicSound)continue;
            entry.getValue().execute(com.mojang.blaze3d.audio.Channel::stop);channels.remove();
            state.afmr$bySource().remove(sound.getSource(),sound);state.afmr$deleteTimes().remove(sound);
        }
        state.afmr$tickingSounds().removeIf(sound->!(sound instanceof MusicSound));
        state.afmr$queuedSounds().clear();state.afmr$queuedTickableSounds().clear();
        // Native channel cleanup retires the stopped channels; updateCategoryVolume
        // still applies exact zero listener gain immediately after this call.
    }
}

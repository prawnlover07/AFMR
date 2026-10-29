package dev.animal.afmr.mixin;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow private ClientLevel level;
    @Shadow @Final private Map<BlockPos,SoundInstance> playingRecords;
    @Inject(method="playStreamingMusic",at=@At("TAIL"))
    private void afmr$jukeboxStarted(SoundEvent song,BlockPos pos,CallbackInfo ci){
        if(song==null)ClientRuntime.get().jukebox().stopped(level,pos);
        else ClientRuntime.get().jukebox().started(level,pos,playingRecords.get(pos));
    }
}

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
    @Shadow @Final private Map<BlockPos,SoundInstance> playingJukeboxSongs;
    @Inject(method="playJukeboxSong",at=@At("TAIL"))
    private void afmr$jukeboxStarted(net.minecraft.core.Holder<net.minecraft.world.item.JukeboxSong> song,BlockPos pos,CallbackInfo ci){
        ClientRuntime.get().jukebox().started(level,pos,playingJukeboxSongs.get(pos));
    }
    @Inject(method="stopJukeboxSong",at=@At("TAIL"))
    private void afmr$jukeboxStopped(BlockPos pos,CallbackInfo ci){ClientRuntime.get().jukebox().stopped(level,pos);}
}

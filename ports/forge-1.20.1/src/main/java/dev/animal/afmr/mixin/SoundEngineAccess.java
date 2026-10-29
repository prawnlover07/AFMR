package dev.animal.afmr.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
import java.util.Map;

@Mixin(SoundEngine.class)
public interface SoundEngineAccess {
    @Accessor("instanceToChannel") Map<SoundInstance,ChannelAccess.ChannelHandle> afmr$channels();
    @Accessor("instanceBySource") com.google.common.collect.Multimap<net.minecraft.sounds.SoundSource,SoundInstance> afmr$bySource();
    @Accessor("soundDeleteTime") Map<SoundInstance,Integer> afmr$deleteTimes();
    @Accessor("tickingSounds") List<TickableSoundInstance> afmr$tickingSounds();
    @Accessor("queuedSounds") Map<SoundInstance,Integer> afmr$queuedSounds();
    @Accessor("queuedTickableSounds") List<TickableSoundInstance> afmr$queuedTickableSounds();
}

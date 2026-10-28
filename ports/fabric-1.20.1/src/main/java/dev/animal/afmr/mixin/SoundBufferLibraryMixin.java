package dev.animal.afmr.mixin;

import dev.animal.afmr.audio.*;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.*;

@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    private net.minecraft.server.packs.resources.ResourceProvider resourceManager;
    @Inject(method="getStream",at=@At("HEAD"),cancellable=true)
    private void afmr$stream(ResourceLocation location,boolean looping,CallbackInfoReturnable<CompletableFuture<AudioStream>> cir){
        var source=GameAudio.streamSource(location);if(source==null)return;
        var resources=resourceManager;
        cir.setReturnValue(CompletableFuture.supplyAsync(()->{
            try{
                AudioStream stream;
                if(source.path()!=null)stream=new LocalAudioStream(source.path(),source.replayGain(),source.loudness());
                else{
                    var input=resources.open(source.resource());
                    try{stream=new com.mojang.blaze3d.audio.OggAudioStream(input);}catch(Exception error){input.close();throw error;}
                }
                StreamSeek.skip(stream,source.offset(),source.owner()::ended);return stream;
            }catch(Exception error){
                if(!source.owner().ended())dev.animal.afmr.ClientRuntime.reportAudioFailure(source.owner(),error);
                throw new CompletionException(error);
            }
        },Util.backgroundExecutor()));
    }
}

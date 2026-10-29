package dev.animal.afmr.mixin;

import com.mojang.blaze3d.audio.Channel;
import dev.animal.afmr.audio.MusicSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import dev.animal.afmr.ClientRuntime;
import net.minecraft.sounds.SoundSource;

/** Track AFMR's single channel; OpenAL changes execute only on Minecraft's sound executor. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow @Final private Map<SoundInstance,ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow private boolean loaded;
    @Shadow private float calculateVolume(SoundInstance sound){throw new AssertionError();}
    @Unique private MusicSound afmr$music;
    @Unique private boolean afmr$appliedPause;

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="tickNonPaused",at=@At(value="INVOKE",target="Lnet/minecraft/client/Options;getSoundSourceVolume(Lnet/minecraft/sounds/SoundSource;)F"))
    private float afmr$retainMutedChannel(float volume,@com.llamalad7.mixinextras.sugar.Local SoundInstance sound){
        // This value controls vanilla's removal decision only. Actual OpenAL gain remains exactly zero.
        return sound instanceof MusicSound&&volume<=0?Float.MIN_NORMAL:volume;
    }
    @Redirect(method="play",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/audio/Listener;getGain()F"))
    private float afmr$allowSilentStart(com.mojang.blaze3d.audio.Listener listener,SoundInstance sound){
        float gain=listener.getGain();return sound instanceof MusicSound&&gain<=0?Float.MIN_NORMAL:gain;
    }
    @Inject(method="updateCategoryVolume",at=@At("HEAD"),cancellable=true)
    private void afmr$preserveMutedStream(SoundSource source,float gain,CallbackInfo ci){
        if(!loaded||source==SoundSource.MASTER||afmr$music==null)return;
        instanceToChannel.forEach((sound,handle)->{
            float volume=calculateVolume(sound);boolean preserve=sound instanceof MusicSound;
            handle.execute(channel->{if(volume<=0&&!preserve)channel.stop();else channel.setVolume(volume);});
        });ci.cancel();
    }

    @Inject(method="play",at=@At("RETURN"))
    private void afmr$remember(SoundInstance instance,CallbackInfo ci){
        if(instance instanceof MusicSound music&&instanceToChannel.containsKey(instance)){afmr$music=music;afmr$appliedPause=false;}
    }
    @Inject(method="play",at=@At("HEAD"),cancellable=true)
    private void afmr$replaceVanilla(SoundInstance instance,CallbackInfo ci){
        if(instance.getSource()==SoundSource.MUSIC&&!(instance instanceof MusicSound)
            &&net.minecraft.client.Minecraft.getInstance().level!=null&&ClientRuntime.get().enabled())ci.cancel();
    }
    @Inject(method="reload",at=@At("TAIL"))
    private void afmr$reload(CallbackInfo ci){ClientRuntime.requestReload();}
    /** Replace NeoForge's streaming-source callback at the actual asynchronous attachment. */
    @Redirect(method="play",at=@At(value="INVOKE",target="Ljava/util/concurrent/CompletableFuture;thenAccept(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> afmr$streamStarted(CompletableFuture<Object> future,Consumer<Object> attach,SoundInstance instance){
        if(!(instance instanceof MusicSound music))return future.thenAccept(attach);
        var handle=instanceToChannel.get(instance);
        return future.thenAccept(stream->{
            if(music.ended()){
                try{((AudioStream)stream).close();}catch(java.io.IOException error){dev.animal.afmr.Afmr.LOG.debug("Canceled stream close failed",error);}return;
            }
            attach.accept(stream);
            if(handle!=null)handle.execute(channel->{if(music.paused())channel.pause();});
        });
    }
    @Inject(method="tick",at=@At("TAIL"))
    private void afmr$pauseChange(boolean gamePaused,CallbackInfo ci){
        if(afmr$music==null)return;
        var handle=instanceToChannel.get(afmr$music);if(handle==null){afmr$music=null;return;}
        // Sound-executor observation publishes only this sound object's start flag.
        // Capturing the object prevents an old callback from marking a replacement track.
        var music=afmr$music;
        if(!music.started())handle.execute(channel->{if(channel.playing())music.markStarted();});
        boolean pause=afmr$music.paused();
        if(pause!=afmr$appliedPause){handle.execute(pause?Channel::pause:Channel::unpause);afmr$appliedPause=pause;}
    }
    @Inject(method="resume",at=@At("TAIL"))
    private void afmr$keepManualPause(CallbackInfo ci){
        if(afmr$music!=null&&afmr$music.paused()){
            var handle=instanceToChannel.get(afmr$music);if(handle!=null){handle.execute(Channel::pause);afmr$appliedPause=true;}
        }
    }
    @Inject(method="stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",at=@At("TAIL"))
    private void afmr$stopPending(SoundInstance instance,CallbackInfo ci){
        if(instance instanceof MusicSound music&&!music.started()){
            var handle=instanceToChannel.get(instance);
            // OpenAL stop on an INITIAL source leaves it INITIAL. Play/stop makes it reclaimable.
            if(handle!=null)handle.execute(channel->{if(!channel.stopped()&&!channel.playing())channel.play();channel.stop();});
        }
    }
    @Inject(method="stopAll",at=@At("TAIL"))
    private void afmr$clear(CallbackInfo ci){afmr$music=null;afmr$appliedPause=false;}
}

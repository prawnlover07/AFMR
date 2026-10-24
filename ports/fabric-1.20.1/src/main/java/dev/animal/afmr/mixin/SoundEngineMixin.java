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
    @Unique private MusicSound afmr$music;
    @Unique private boolean afmr$appliedPause;

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
    @Inject(method="stopAll",at=@At("TAIL"))
    private void afmr$clear(CallbackInfo ci){afmr$music=null;afmr$appliedPause=false;}
}

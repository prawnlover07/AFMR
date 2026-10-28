package dev.animal.afmr.mixin;

import com.mojang.blaze3d.audio.Channel;
import dev.animal.afmr.audio.MusicSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Map;

/** Track AFMR's single channel; OpenAL changes execute only on Minecraft's sound executor. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow @Final private Map<SoundInstance,ChannelAccess.ChannelHandle> instanceToChannel;
    @Unique private MusicSound afmr$music;
    @Unique private boolean afmr$appliedPause;

    @Inject(method="play",at=@At("RETURN"))
    private void afmr$remember(SoundInstance instance,CallbackInfoReturnable<SoundEngine.PlayResult> cir){
        if(instance instanceof MusicSound music&&cir.getReturnValue()!=SoundEngine.PlayResult.NOT_STARTED){afmr$music=music;afmr$appliedPause=false;}
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

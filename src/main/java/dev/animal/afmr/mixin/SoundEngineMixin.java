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

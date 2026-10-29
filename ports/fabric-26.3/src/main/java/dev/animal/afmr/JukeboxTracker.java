package dev.animal.afmr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import java.util.Arrays;

/** Client-thread jukebox sounds only; fixed storage, no world or library scans. */
public final class JukeboxTracker {
    private static final int CAPACITY=128;
    private final BlockPos[] positions=new BlockPos[CAPACITY];
    private final SoundInstance[] sounds=new SoundInstance[CAPACITY];
    private final int[] warmup=new int[CAPACITY];
    private ClientLevel level;
    private int count,nextReplacement;

    public void started(ClientLevel source,BlockPos pos,SoundInstance sound){
        if(source!=level){clear();level=source;}
        if(sound==null)return;
        int index=0;while(index<count&&!positions[index].equals(pos))index++;
        if(index==count){if(count<CAPACITY)count++;else{index=nextReplacement;nextReplacement=(nextReplacement+1)%CAPACITY;}}
        positions[index]=pos.immutable();sounds[index]=sound;warmup[index]=10;
    }
    public void stopped(ClientLevel source,BlockPos pos){
        if(source!=level)return;
        for(int i=0;i<count;i++)if(positions[i].equals(pos)){remove(i);return;}
    }
    private void remove(int index){
        int last=--count;positions[index]=positions[last];sounds[index]=sounds[last];warmup[index]=warmup[last];
        positions[last]=null;sounds[last]=null;warmup[last]=0;
    }
    public boolean audible(Minecraft mc){
        if(level!=mc.level){clear();return false;}
        if(count==0||mc.player==null)return false;
        boolean audible=false,enabled=mc.options.getFinalSoundSourceVolume(SoundSource.RECORDS)>0;
        var camera=mc.gameRenderer.mainCamera();var listener=camera.isInitialized()?camera.position():mc.player.position();
        for(int i=0;i<count;){
            var instance=sounds[i];boolean active=mc.getSoundManager().isActive(instance);
            if(warmup[i]>0)warmup[i]--;
            if(!active&&warmup[i]==0){remove(i);continue;}
            var sound=instance.getSound();
            if(enabled&&active&&sound!=null&&instance.getVolume()>0){
                double radius=sound.getAttenuationDistance(instance.getVolume());
                double dx=listener.x-instance.getX(),dy=listener.y-instance.getY(),dz=listener.z-instance.getZ();
                if(instance.getAttenuation()==SoundInstance.Attenuation.NONE||dx*dx+dy*dy+dz*dz<radius*radius)audible=true;
            }
            i++;
        }
        return audible;
    }
    public void clear(){Arrays.fill(positions,null);Arrays.fill(sounds,null);count=nextReplacement=0;level=null;}
}

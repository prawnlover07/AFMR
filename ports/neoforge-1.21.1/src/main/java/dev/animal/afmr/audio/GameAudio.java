package dev.animal.afmr.audio;

import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.core.Library.Settings;
import dev.animal.afmr.core.PlaybackEngine;
import dev.animal.afmr.core.TrackLoudness;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class GameAudio implements PlaybackEngine.Audio {
    public record StreamSource(Path path,ResourceLocation resource,boolean replayGain,TrackLoudness loudness,double offset,MusicSound owner) {}
    private static final Map<ResourceLocation, StreamSource> STREAMS = new ConcurrentHashMap<>();
    private final Minecraft minecraft;
    private final Settings settings;
    private MusicSound sound;
    private int warmup;
    private long generation;
    private float gain;
    private boolean paused;
    public GameAudio(Minecraft minecraft,Settings settings) { this.minecraft=minecraft;this.settings=settings; }
    public static StreamSource streamSource(ResourceLocation path) { return STREAMS.get(path); }
    @Override public void start(Track track,double offset) throws Exception {
        stop();
        Path path=track.vanilla?null:Path.of(track.path);
        if(path!=null&&!Files.isRegularFile(path))throw new java.io.IOException("File missing; rescan its folder");
        ResourceLocation original=track.vanilla?ResourceLocation.parse(track.path):null;
        ResourceLocation resource=original==null?null:ResourceLocation.fromNamespaceAndPath(original.getNamespace(),"sounds/"+original.getPath()+".ogg");
        ResourceLocation file=ResourceLocation.fromNamespaceAndPath("afmr","stream/"+(++generation));
        sound = new MusicSound(file); sound.gain(gain);sound.setPaused(paused);warmup = 10;
        STREAMS.put(ResourceLocation.fromNamespaceAndPath(file.getNamespace(),"sounds/"+file.getPath()+".ogg"),new StreamSource(path,resource,!track.vanilla&&settings.replayGainEnabled,track.loudness,offset,sound));
        minecraft.getSoundManager().play(sound);
        dev.animal.afmr.Afmr.LOG.info("AFMR playing: {} ({})", track.title, track.path);
    }
    @Override public boolean playing() {
        return sound != null && (warmup-- > 0 || minecraft.getSoundManager().isActive(sound));
    }
    @Override public boolean started(){return sound!=null&&sound.started();}
    @Override public void gain(float gain) { this.gain = gain; if (sound != null) sound.gain(gain); }
    @Override public void setPaused(boolean value){paused=value;if(sound!=null)sound.setPaused(value);}
    @Override public void stop() {
        if (sound != null) { sound.end(); minecraft.getSoundManager().stop(sound); sound = null; }
        STREAMS.clear();
    }
}

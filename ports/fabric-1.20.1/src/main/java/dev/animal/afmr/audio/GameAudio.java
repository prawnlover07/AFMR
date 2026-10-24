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
    public record LocalSource(Path path,boolean replayGain,TrackLoudness loudness) {}
    private static final Map<ResourceLocation, LocalSource> LOCAL = new ConcurrentHashMap<>();
    private final Minecraft minecraft;
    private final Settings settings;
    private MusicSound sound;
    private int warmup;
    private float gain;
    private boolean paused;
    public GameAudio(Minecraft minecraft,Settings settings) { this.minecraft=minecraft;this.settings=settings; }
    public static LocalSource localSource(ResourceLocation path) { return LOCAL.get(path); }
    @Override public void start(Track track) throws Exception {
        stop();
        ResourceLocation file;
        if (track.vanilla) file = new ResourceLocation(track.path);
        else {
            Path path = Path.of(track.path);
            if (!Files.isRegularFile(path)) throw new java.io.IOException("File missing; rescan its folder");
            file = new ResourceLocation("afmr", "library/" + track.id.substring(5));
            LOCAL.put(new ResourceLocation("afmr", "sounds/" + file.getPath() + ".ogg"), new LocalSource(path,settings.replayGainEnabled,track.loudness));
        }
        sound = new MusicSound(file); sound.gain(gain);sound.setPaused(paused);warmup = 10;
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
        LOCAL.clear();
    }
}

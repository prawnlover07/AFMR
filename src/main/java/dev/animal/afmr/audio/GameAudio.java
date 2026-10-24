package dev.animal.afmr.audio;

import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.core.Library.Settings;
import dev.animal.afmr.core.PlaybackEngine;
import dev.animal.afmr.core.TrackLoudness;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class GameAudio implements PlaybackEngine.Audio {
    public record LocalSource(Path path,boolean replayGain,TrackLoudness loudness) {}
    private static final Map<Identifier, LocalSource> LOCAL = new ConcurrentHashMap<>();
    private final Minecraft minecraft;
    private final Settings settings;
    private MusicSound sound;
    private int warmup;
    private float gain;
    private boolean paused;
    public GameAudio(Minecraft minecraft,Settings settings) { this.minecraft=minecraft;this.settings=settings; }
    public static LocalSource localSource(Identifier path) { return LOCAL.get(path); }
    @Override public void start(Track track) throws Exception {
        stop();
        Identifier file;
        if (track.vanilla) file = Identifier.parse(track.path);
        else {
            Path path = Path.of(track.path);
            if (!Files.isRegularFile(path)) throw new java.io.IOException("File missing; rescan its folder");
            file = Identifier.fromNamespaceAndPath("afmr", "library/" + track.id.substring(5));
            LOCAL.put(Identifier.fromNamespaceAndPath("afmr", "sounds/" + file.getPath() + ".ogg"), new LocalSource(path,settings.replayGainEnabled,track.loudness));
        }
        sound = new MusicSound(file); sound.gain(gain);sound.setPaused(paused);warmup = 10;
        if (minecraft.getSoundManager().play(sound) == net.minecraft.client.sounds.SoundEngine.PlayResult.NOT_STARTED) {
            stop(); throw new java.io.IOException("Minecraft sound engine could not start the track");
        }
        dev.animal.afmr.Afmr.LOG.info("AFMR playing: {} ({})", track.title, track.path);
    }
    @Override public boolean playing() {
        return sound != null && (warmup-- > 0 || minecraft.getSoundManager().isActive(sound));
    }
    @Override public void gain(float gain) { this.gain = gain; if (sound != null) sound.gain(gain); }
    @Override public void setPaused(boolean value){paused=value;if(sound!=null)sound.setPaused(value);}
    @Override public void stop() {
        if (sound != null) { sound.end(); minecraft.getSoundManager().stop(sound); sound = null; }
        LOCAL.clear();
    }
}

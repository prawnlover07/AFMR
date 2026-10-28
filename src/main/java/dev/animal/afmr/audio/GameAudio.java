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
    public record StreamSource(Path path,Identifier resource,boolean replayGain,TrackLoudness loudness,double offset,MusicSound owner) {}
    private static final Map<Identifier, StreamSource> STREAMS = new ConcurrentHashMap<>();
    private final Minecraft minecraft;
    private final Settings settings;
    private MusicSound sound;
    private int warmup;
    private long generation;
    private float gain;
    private boolean paused;
    public GameAudio(Minecraft minecraft,Settings settings) { this.minecraft=minecraft;this.settings=settings; }
    public static StreamSource streamSource(Identifier path) { return STREAMS.get(path); }
    @Override public void start(Track track,double offset) throws Exception {
        stop();
        Path path=track.vanilla?null:Path.of(track.path);
        if(path!=null&&!Files.isRegularFile(path))throw new java.io.IOException("File missing; rescan its folder");
        Identifier original=track.vanilla?Identifier.parse(track.path):null;
        Identifier resource=original==null?null:Identifier.fromNamespaceAndPath(original.getNamespace(),"sounds/"+original.getPath()+".ogg");
        Identifier file=Identifier.fromNamespaceAndPath("afmr","stream/"+(++generation));
        sound = new MusicSound(file); sound.gain(gain);sound.setPaused(paused);warmup = 10;
        STREAMS.put(Identifier.fromNamespaceAndPath(file.getNamespace(),"sounds/"+file.getPath()+".ogg"),new StreamSource(path,resource,!track.vanilla&&settings.replayGainEnabled,track.loudness,offset,sound));
        if (minecraft.getSoundManager().play(sound) == net.minecraft.client.sounds.SoundEngine.PlayResult.NOT_STARTED) {
            stop(); throw new java.io.IOException("Minecraft sound engine could not start the track");
        }
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

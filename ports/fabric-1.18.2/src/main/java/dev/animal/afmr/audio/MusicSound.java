package dev.animal.afmr.audio;

import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;

/** A directly resolved, streaming music sound, retaining Minecraft's volume and device handling. */
public final class MusicSound extends AbstractTickableSoundInstance {
    private volatile boolean paused,started,ended;
    public boolean started(){return started;}
    public void markStarted(){started=true;}
    private final WeighedSoundEvents soundEvent;
    public MusicSound(ResourceLocation file) {
        super(new SoundEvent(new ResourceLocation("afmr", "playback")),
            SoundSource.MUSIC);
        sound = new Sound(file.toString(), 1, 1, 1, Sound.Type.FILE, true, false, 16);
        soundEvent = new WeighedSoundEvents(location, null); soundEvent.addSound(sound);
        relative = true; attenuation = Attenuation.NONE; volume = 0;
    }
    @Override public WeighedSoundEvents resolve(SoundManager manager) { return soundEvent; }
    @Override public boolean canStartSilent() { return true; }
    @Override public void tick() {}
    public void gain(float value) { volume = value; }
    public void setPaused(boolean value){paused=value;}
    public boolean paused(){return paused;}
    public boolean ended(){return ended;}
    public void end() { ended=true;stop(); }
}

package dev.animal.afmr.audio;

import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.system.MemoryUtil;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

public final class LocalAudioStream implements AudioStream {
    private final AudioInputStream pcm;
    private final double replayGain;
    public LocalAudioStream(Path file,boolean normalize) throws Exception {
        replayGain=normalize?ReplayGain.readMultiplier(file):1;
        pcm=PcmDecoder.open(file);
    }
    @Override public AudioFormat getFormat() { return pcm.getFormat(); }
    @Override public ByteBuffer read(int requested) throws IOException {
        int frame = pcm.getFormat().getFrameSize();
        byte[] data = pcm.readNBytes(Math.max(frame, requested / frame * frame));
        if (data.length == 0) return null;
        ReplayGain.apply(data,replayGain);
        // SoundBuffer takes ownership and frees this allocation after uploading it to OpenAL.
        ByteBuffer output = MemoryUtil.memAlloc(data.length); output.put(data).flip(); return output;
    }
    @Override public void close() throws IOException { pcm.close(); }
}

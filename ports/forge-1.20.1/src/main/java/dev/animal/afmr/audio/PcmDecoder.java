package dev.animal.afmr.audio;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.sound.sampled.spi.*;

/** Explicit provider loading also works with NeoForge's nested dependency class loader. */
public final class PcmDecoder {
    // Forge discovers bundled providers in its Java module layer. The plain
    // class-path runner used by the regression suite has no associated layer.
    private static final ModuleLayer LAYER=PcmDecoder.class.getModule().getLayer();
    private PcmDecoder() {}
    public static AudioInputStream open(Path file) throws Exception {
        AudioInputStream encoded = null;
        try { encoded = AudioSystem.getAudioInputStream(file.toFile()); }
        catch (UnsupportedAudioFileException ignored) {}
        if (encoded == null) {
            for (AudioFileReader reader : (LAYER==null?ServiceLoader.load(AudioFileReader.class,PcmDecoder.class.getClassLoader()):ServiceLoader.load(LAYER,AudioFileReader.class))) {
                try { encoded = reader.getAudioInputStream(file.toFile()); break; }
                catch (UnsupportedAudioFileException ignored) {}
            }
        }
        if (encoded == null) throw new UnsupportedAudioFileException("Cannot decode " + file.getFileName());
        try {
            AudioFormat source = encoded.getFormat();
            if (source.getChannels() < 1 || source.getChannels() > 2)
                throw new UnsupportedAudioFileException("Only mono and stereo music are supported");
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, source.getSampleRate(),
                16, source.getChannels(), source.getChannels() * 2, source.getSampleRate(), false);
            if (source.matches(target)) return encoded;
            for (FormatConversionProvider provider : (LAYER==null?ServiceLoader.load(FormatConversionProvider.class,PcmDecoder.class.getClassLoader()):ServiceLoader.load(LAYER,FormatConversionProvider.class)))
                if (provider.isConversionSupported(target, source)) return provider.getAudioInputStream(target, encoded);
            if (AudioSystem.isConversionSupported(target, source)) return AudioSystem.getAudioInputStream(target, encoded);
            // FLAC's provider preserves its source depth. Decode 24-bit FLAC to 24-bit PCM
            // first, then let Java Sound convert the PCM depth for Minecraft/OpenAL.
            for (FormatConversionProvider provider : (LAYER==null?ServiceLoader.load(FormatConversionProvider.class,PcmDecoder.class.getClassLoader()):ServiceLoader.load(LAYER,FormatConversionProvider.class))) {
                for (AudioFormat intermediate : provider.getTargetFormats(AudioFormat.Encoding.PCM_SIGNED, source)) {
                    if (AudioSystem.isConversionSupported(target, intermediate)) {
                        AudioInputStream decoded = provider.getAudioInputStream(intermediate, encoded);
                        try { return AudioSystem.getAudioInputStream(target, decoded); }
                        catch (Exception e) { decoded.close(); throw e; }
                    }
                }
            }
            throw new UnsupportedAudioFileException("No PCM decoder for " + source);
        } catch (Exception e) { encoded.close(); throw e; }
    }
}

package dev.animal.afmr.audio;

import dev.animal.afmr.core.TrackLoudness;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.wav.WavTag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import static org.junit.jupiter.api.Assertions.*;

class LocalAudioGainTest {
    @TempDir Path directory;
    @Test void playbackUsesMatchingCacheAndRejectsStaleOrInvalidMeasurements() throws Exception {
        Path file=directory.resolve("tone.wav");LoudnessMeterTest.wav(file,LoudnessMeterTest.tone(48000,2,0.1,1),48000,2);
        var cached=new TrackLoudness(Files.size(file),Files.getLastModifiedTime(file).toString(),6.020599913279624,0.1);
        assertEquals(2,ReplayGain.resolve(file,cached),1e-10);
        assertEquals(1,ReplayGain.resolve(file,new TrackLoudness(Files.size(file),cached.modifiedTime(),Double.NaN,0.1)));
        Files.setLastModifiedTime(file,FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis()+2000));
        assertEquals(1,ReplayGain.resolve(file,cached));assertEquals(1,ReplayGain.resolve(file,null));
    }
    @Test void existingTagsReplaceCachedGainRatherThanStackingIt() throws Exception {
        Path file=directory.resolve("tagged.wav");LoudnessMeterTest.wav(file,LoudnessMeterTest.tone(48000,2,0.1,1),48000,2);
        var audio=AudioFileIO.read(file.toFile());var wav=(WavTag)audio.getTagOrCreateAndSetDefault();var tag=new ID3v23Tag();
        var frame=new ID3v23Frame("TXXX");frame.setBody(new FrameBodyTXXX((byte)0,"REPLAYGAIN_TRACK_GAIN","-6.020599913279624 dB"));tag.setField(frame);wav.setID3Tag(tag);audio.commit();
        var cached=new TrackLoudness(Files.size(file),Files.getLastModifiedTime(file).toString(),6.020599913279624,0.1);
        assertEquals(0.5,ReplayGain.resolve(file,cached),1e-10);
    }
}

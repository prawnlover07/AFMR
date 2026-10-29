package dev.animal.afmr.library;

import dev.animal.afmr.audio.ReplayGain;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.jaudiotagger.tag.wav.WavTag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SongMetadataTest {
    @TempDir Path directory;
    private static void gainField(Tag tag,String key,String value) throws Exception {
        if(tag instanceof AbstractID3v2Tag id3){
            AbstractID3v2Frame frame=id3 instanceof ID3v23Tag?new ID3v23Frame("TXXX"):new ID3v24Frame("TXXX");
            frame.setBody(new FrameBodyTXXX((byte)0,key,value));tag.setField(frame);
        }else if(tag instanceof FlacTag flac)flac.setField(key,value);
        else if(tag instanceof VorbisCommentTag vorbis)vorbis.setField(key,value);
        else fail("Unexpected generated tag format: "+tag.getClass());
    }
    @TestFactory List<DynamicTest> importsAdditionalTagsFromEverySupportedFormatWithoutWritingSourceFiles(){
        return List.of("test.mp3","test.flac","test.ogg","test.wav").stream().map(name->DynamicTest.dynamicTest(name,()->{
            Path file=directory.resolve(name);try(var input=getClass().getResourceAsStream("/audio/"+name)){Files.copy(input,file);}
            var audio=AudioFileIO.read(file.toFile());var tag=audio.getTagOrCreateAndSetDefault();
            if(tag instanceof WavTag wav){var id3=new ID3v23Tag();wav.setID3Tag(id3);tag=id3;}
            tag.setField(FieldKey.ALBUM_ARTIST,"Ensemble");tag.setField(FieldKey.TRACK,"3");tag.setField(FieldKey.DISC_NO,"2");
            tag.setField(FieldKey.GENRE,"Ambient");tag.setField(FieldKey.YEAR,"2026");
            gainField(tag,"REPLAYGAIN_TRACK_GAIN","-6 dB");gainField(tag,"REPLAYGAIN_TRACK_PEAK","0.8");
            gainField(tag,"REPLAYGAIN_ALBUM_GAIN","-8 dB");gainField(tag,"REPLAYGAIN_ALBUM_PEAK","0.9");
            audio.commit();byte[] before=Files.readAllBytes(file);var modified=Files.getLastModifiedTime(file);
            var result=MusicFiles.scan(List.of(new MusicFiles.Request(file,"library",false)),List.of(),p->{});assertEquals(1,result.found().size());
            var track=result.found().getFirst().track();assertEquals("Ensemble",track.albumArtist);assertEquals("3",track.trackNumber);assertEquals("2",track.discNumber);
            assertEquals("Ambient",track.genre);assertEquals("2026",track.date);assertNotNull(track.replayGain);
            assertEquals(-6,track.replayGain.trackDb());assertEquals(0.8,track.replayGain.trackPeak());assertEquals(-8,track.replayGain.albumDb());assertEquals(0.9,track.replayGain.albumPeak());
            assertArrayEquals(before,Files.readAllBytes(file));assertEquals(modified,Files.getLastModifiedTime(file));
        })).toList();
    }
    @Test void albumOnlyAndPeakOnlyTagsRemainVisibleWithoutChangingPlaybackGain() throws Exception {
        var tag=new VorbisCommentTag();tag.setField("REPLAYGAIN_ALBUM_GAIN","-8 dB");tag.setField("replaygain_track_peak","0.8");
        var info=ReplayGain.metadata(tag);assertNull(info.trackDb());assertEquals(-8,info.albumDb());assertEquals(0.8,info.trackPeak());
    }
    @Test void invalidReplayGainValuesDoNotAppearAsValidMeasurements() throws Exception {
        var tag=new VorbisCommentTag();tag.setField("REPLAYGAIN_TRACK_GAIN","NaN");tag.setField("REPLAYGAIN_TRACK_PEAK","Infinity");
        tag.setField("REPLAYGAIN_ALBUM_GAIN","garbage");tag.setField("REPLAYGAIN_ALBUM_PEAK","-1");assertNull(ReplayGain.metadata(tag));assertNull(ReplayGain.metadata(null));
    }
}

package dev.animal.afmr.audio;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.wav.WavOptions;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.jaudiotagger.tag.wav.WavTag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReplayGainTest {
    @TempDir Path directory;
    private static AbstractID3v2Frame frame(AbstractID3v2Tag tag,String key,String value){
        AbstractID3v2Frame frame=tag instanceof ID3v23Tag?new ID3v23Frame("TXXX"):new ID3v24Frame("TXXX");
        frame.setBody(new FrameBodyTXXX((byte)0,key,value));return frame;
    }
    private static void fields(AbstractID3v2Tag tag) throws Exception {
        tag.setField(frame(tag,"replaygain_track_gain","-6.020599913279624 dB"));
        tag.setField(frame(tag,"REPLAYGAIN_TRACK_PEAK","0.25"));
        tag.setField(frame(tag,"REPLAYGAIN_ALBUM_GAIN","+6 dB"));
    }
    @Test void decibelsConvertToLinearAmplitude(){
        assertEquals(0.5,ReplayGain.multiplier(" -6.020599913279624 dB ","1"),1e-10);
        assertEquals(2,ReplayGain.multiplier("+6.020599913279624 DB","0.25"),1e-10);
        assertEquals(1,ReplayGain.multiplier("0","1"));
    }
    @Test void peakProtectionReducesBoostAndAcceptsPeaksAboveFullScale(){
        assertEquals(1.25,ReplayGain.multiplier("+12 dB","0.8"),1e-10);
        assertEquals(0.5,ReplayGain.multiplier("0 dB","2"),1e-10);
        assertEquals(0.5,ReplayGain.multiplier("-3 dB","2"),1e-10);
    }
    @Test void missingOrInvalidPeakAllowsAttenuationButNotUnverifiedBoost(){
        for(String peak:List.of("","bogus","NaN","Infinity","0","-1")){
            assertEquals(1,ReplayGain.multiplier("+6 dB",peak));
            assertEquals(0.5,ReplayGain.multiplier("-6.020599913279624 dB",peak),1e-10);
        }
    }
    @Test void malformedOrMissingGainIsUnchanged(){
        for(String gain:List.of("","loud","NaN","Infinity","1e999","1e99","-1e99"))assertEquals(1,ReplayGain.multiplier(gain,"0.25"));
        assertEquals(1,ReplayGain.multiplier((Tag)null));
    }
    @Test void albumTagsAloneDoNotChangeTrackPlayback() throws Exception {
        var tag=new VorbisCommentTag();tag.setField("REPLAYGAIN_ALBUM_GAIN","-12 dB");tag.setField("REPLAYGAIN_ALBUM_PEAK","1");
        assertEquals(1,ReplayGain.multiplier(tag));
    }
    @Test void readsCaseInsensitiveVorbisFieldsAndPrefersTrackToAlbum() throws Exception {
        var tag=new VorbisCommentTag();tag.setField("replaygain_track_gain","-6.020599913279624 dB");tag.setField("replaygain_track_peak","0.25");tag.setField("REPLAYGAIN_ALBUM_GAIN","+6 dB");
        assertEquals(0.5,ReplayGain.multiplier(tag),1e-10);
        assertEquals(0.5,ReplayGain.multiplier(new FlacTag(tag,List.of())),1e-10);
    }
    @TestFactory List<DynamicTest> readsId3v23And24UserTextFrames(){
        return List.of(new ID3v23Tag(),new ID3v24Tag()).stream().map(tag->DynamicTest.dynamicTest(tag.getClass().getSimpleName(),()->{
            fields(tag);assertEquals(0.5,ReplayGain.multiplier(tag),1e-10);
        })).toList();
    }
    @Test void readsWavId3EvenWhenInfoTagsArePreferred() throws Exception {
        var tag=new WavTag(WavOptions.READ_INFO_UNLESS_ONLY_ID3);var id3=new ID3v23Tag();fields(id3);tag.setID3Tag(id3);
        assertEquals(0.5,ReplayGain.multiplier(tag),1e-10);
    }
    @Test void pcmBoostAttenuationAndSaturationDoNotWrapSignedSamples(){
        byte[] samples=pcm(-32768,-1000,0,1000,32767);ReplayGain.apply(samples,2);
        assertArrayEquals(new short[]{-32768,-2000,0,2000,32767},shorts(samples));
        samples=pcm(-1000,1000);ReplayGain.apply(samples,0.5);assertArrayEquals(new short[]{-500,500},shorts(samples));
    }
    @Test void unityIsBitExactAndEmptyBuffersAreAllowed(){
        byte[] data=pcm(-32768,12,-16,32767),before=data.clone();ReplayGain.apply(data,1);assertArrayEquals(before,data);
        ReplayGain.apply(new byte[0],2);
    }
    @Test void unavailableOrUnreadableTagsDoNotBlockPlayback() throws Exception {
        assertEquals(1,ReplayGain.readMultiplier(directory.resolve("missing.flac")));
        var file=directory.resolve("invalid.mp3");Files.writeString(file,"not audio");assertEquals(1,ReplayGain.readMultiplier(file));
    }
    @TestFactory List<DynamicTest> actualTaggedAudioFormatsScaleDecodedWaveformsWithoutChangingFiles(){
        return List.of("test.mp3","test.flac","test.ogg","test.wav").stream().map(name->DynamicTest.dynamicTest(name,()->{
            Path file=directory.resolve(name);try(var input=getClass().getResourceAsStream("/audio/"+name)){Files.copy(input,file);}
            var audio=AudioFileIO.read(file.toFile());var tag=audio.getTagOrCreateAndSetDefault();
            if(tag instanceof WavTag wav){var id3=new ID3v23Tag();fields(id3);wav.setID3Tag(id3);}
            else if(tag instanceof AbstractID3v2Tag id3)fields(id3);
            else if(tag instanceof FlacTag flac){flac.setField("REPLAYGAIN_TRACK_GAIN","-6.020599913279624 dB");flac.setField("REPLAYGAIN_TRACK_PEAK","0.25");}
            else if(tag instanceof VorbisCommentTag vorbis){vorbis.setField("REPLAYGAIN_TRACK_GAIN","-6.020599913279624 dB");vorbis.setField("REPLAYGAIN_TRACK_PEAK","0.25");}
            else fail("Unexpected fixture tag format: "+tag.getClass());
            audio.commit();byte[] original=Files.readAllBytes(file);var modified=Files.getLastModifiedTime(file);
            double gain=ReplayGain.readMultiplier(file);assertEquals(0.5,gain,1e-10);
            byte[] decoded;try(var pcm=PcmDecoder.open(file)){decoded=pcm.readAllBytes();}
            double before=rms(decoded);ReplayGain.apply(decoded,gain);assertEquals(0.5,rms(decoded)/before,0.002);
            assertArrayEquals(original,Files.readAllBytes(file));assertEquals(modified,Files.getLastModifiedTime(file));
        })).toList();
    }
    private static byte[] pcm(int... samples){var b=ByteBuffer.allocate(samples.length*2).order(ByteOrder.LITTLE_ENDIAN);for(int s:samples)b.putShort((short)s);return b.array();}
    private static short[] shorts(byte[] bytes){var b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();var out=new short[b.remaining()];b.get(out);return out;}
    private static double rms(byte[] bytes){double sum=0;for(short s:shorts(bytes))sum+=(double)s*s;return Math.sqrt(sum/(bytes.length/2));}
}

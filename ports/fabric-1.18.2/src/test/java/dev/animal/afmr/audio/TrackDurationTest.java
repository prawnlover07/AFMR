package dev.animal.afmr.audio;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrackDurationTest {
    @TempDir Path directory;
    @TestFactory List<DynamicTest> localFormatsExposePositiveDurationsWithoutChangingFiles(){
        return List.of("test.wav","test.mp3","test.flac","test.ogg","test24.wav","test24.flac","testfloat.wav").stream().map(name->DynamicTest.dynamicTest(name,()->{
            Path path=directory.resolve(name);try(var input=getClass().getResourceAsStream("/audio/"+name)){Files.copy(input,path);}
            byte[] original=Files.readAllBytes(path);var modified=Files.getLastModifiedTime(path);double seconds=TrackDuration.local(path);
            assertTrue(Double.isFinite(seconds)&&seconds>0&&seconds<10,"Duration for "+name+": "+seconds);
            assertArrayEquals(original,Files.readAllBytes(path));assertEquals(modified,Files.getLastModifiedTime(path));
        })).toList();
    }
    @Test void actualVorbisGranulesExposeTheGeneratedFixtureDuration()throws Exception{
        try(var wav=new BufferedInputStream(getClass().getResourceAsStream("/audio/test.wav"));var input=getClass().getResourceAsStream("/audio/test.ogg")){
            var reference=javax.sound.sampled.AudioSystem.getAudioFileFormat(wav);
            double seconds=reference.getFrameLength()/(double)reference.getFormat().getFrameRate();
            assertEquals(seconds,TrackDuration.vorbis(input),1e-9);
        }
    }
    private byte[] page(long granule,int flags,int serial,byte[] body)throws Exception{
        var out=new ByteArrayOutputStream();var header=ByteBuffer.allocate(27).order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{'O','g','g','S',0,(byte)flags});header.putLong(granule).putInt(serial).putInt(0).putInt(0).put((byte)1);
        out.write(header.array());out.write(body.length);out.write(body);return out.toByteArray();
    }
    private byte[] identification(){
        var packet=ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
        packet.put(new byte[]{1,'v','o','r','b','i','s'}).putInt(0).put((byte)2).putInt(48000);return packet.array();
    }
    @Test void granuleMathIgnoresUnknownPositionsAndRequiresAnEndPage()throws Exception{
        var data=new ByteArrayOutputStream();data.write(page(0,2,7,identification()));data.write(page(-1,0,7,new byte[1]));data.write(page(120000,4,7,new byte[1]));
        assertEquals(2.5,TrackDuration.vorbis(new ByteArrayInputStream(data.toByteArray())));
        assertThrows(IOException.class,()->TrackDuration.vorbis(new ByteArrayInputStream(page(0,2,7,identification()))));
    }
    @Test void invalidTruncatedAndChainedResourcesDoNotYieldShortGuesses()throws Exception{
        assertThrows(IOException.class,()->TrackDuration.vorbis(new ByteArrayInputStream("not ogg".getBytes())));
        var data=new ByteArrayOutputStream();data.write(page(0,2,7,identification()));data.write(page(48000,4,7,new byte[1]));
        byte[] truncated=Arrays.copyOf(data.toByteArray(),data.size()-1);
        assertThrows(IOException.class,()->TrackDuration.vorbis(new ByteArrayInputStream(truncated)));
        data.write(page(0,2,8,identification()));data.write(page(48000,4,8,new byte[1]));
        assertThrows(IOException.class,()->TrackDuration.vorbis(new ByteArrayInputStream(data.toByteArray())));
    }
    @Test void malformedLocalAudioCannotProduceAValidDuration()throws Exception{
        Path path=directory.resolve("broken.wav");Files.writeString(path,"not audio");
        assertThrows(Exception.class,()->TrackDuration.local(path));
    }
}

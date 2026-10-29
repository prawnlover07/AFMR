package dev.animal.afmr.audio;

import dev.animal.afmr.library.MusicFiles;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import static org.junit.jupiter.api.Assertions.*;

class PcmDecoderTest {
    @TempDir Path directory;
    @TestFactory List<DynamicTest> allAdvertisedFormatsDecodeToPcm(){
        return List.of("test.wav","test.mp3","test.flac","test.ogg","test24.wav","test24.flac","testfloat.wav").stream().map(name->DynamicTest.dynamicTest(name,()->{
            Path file=directory.resolve(name);
            try(var input=getClass().getResourceAsStream("/audio/"+name)){assertNotNull(input);Files.copy(input,file);}
            try(var pcm=PcmDecoder.open(file)){
                assertEquals(AudioFormat.Encoding.PCM_SIGNED,pcm.getFormat().getEncoding());
                assertEquals(16,pcm.getFormat().getSampleSizeInBits());assertEquals(2,pcm.getFormat().getChannels());
                assertEquals(44100,pcm.getFormat().getSampleRate());assertFalse(pcm.getFormat().isBigEndian());
                byte[] data=pcm.readAllBytes();assertTrue(data.length>300000,"Two seconds of PCM audio expected");
                boolean nonzero=false;for(byte value:data)if(value!=0){nonzero=true;break;}assertTrue(nonzero,"Tone must not decode to silence");
                double squares=0;for(int i=0;i+1<data.length;i+=2){short sample=(short)((data[i]&255)|(data[i+1]<<8));squares+=(double)sample*sample;}
                double rms=Math.sqrt(squares/(data.length/2.0));assertTrue(rms>500&&rms<5000,"PCM sample amplitude should match the generated quiet sine wave: "+rms);
            }
            var scan=MusicFiles.scan(List.of(new MusicFiles.Request(file,"general",false)),List.of(),p->{});
            assertEquals(1,scan.found().size());assertEquals("AFMR Format Test",scan.found().getFirst().track().title);
        })).toList();
    }
    @Test void badFileFailsCleanly()throws Exception{
        Path file=directory.resolve("broken.mp3");Files.writeString(file,"not audio");
        assertThrows(Exception.class,()->PcmDecoder.open(file));
    }
}

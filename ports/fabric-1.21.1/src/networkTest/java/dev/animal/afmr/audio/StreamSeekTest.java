package dev.animal.afmr.audio;

import net.minecraft.client.sounds.AudioStream;
import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Minecraft's stream contract is on the component-test classpath, without starting a game. */
class StreamSeekTest {
    private static final class Stream implements AudioStream {
        final ByteBuffer storage=ByteBuffer.allocateDirect(32);
        int position,closes;boolean fail;
        Stream(){for(int i=0;i<32;i++)storage.put(i,(byte)i);}
        public AudioFormat getFormat(){return new AudioFormat(4,16,1,true,false);}
        public ByteBuffer read(int size)throws IOException{
            if(fail)throw new IOException("fixture");
            if(position==32)return null;
            int count=Math.min(size,32-position);var result=storage.slice(position,count);position+=count;return result;
        }
        public void close(){closes++;}
    }
    @Test void skipsWholeFramesWithoutFreeingDecoderOwnedStorage()throws Exception{
        var stream=new Stream();StreamSeek.skip(stream,1.5,()->false);
        assertEquals(12,stream.position);assertEquals(0,stream.closes);
        // The decoder retains and reuses the same GC-managed allocation.
        assertEquals(12,stream.read(2).get(0));assertEquals(31,stream.storage.get(31));
    }
    @Test void canceledWorkClosesItsStreamBeforeReturning(){
        var stream=new Stream();assertThrows(CancellationException.class,()->StreamSeek.skip(stream,1,()->true));assertEquals(1,stream.closes);assertEquals(0,stream.position);
    }
    @Test void decodingFailureClosesItsStreamAndKeepsTheFailure(){
        var stream=new Stream();stream.fail=true;assertThrows(IOException.class,()->StreamSeek.skip(stream,1,()->false));assertEquals(1,stream.closes);
    }
}

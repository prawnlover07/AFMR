package dev.animal.afmr.audio;

import net.minecraft.client.sounds.AudioStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Discards decoded frames on the existing decoder worker, never the client/sound executor. */
public final class StreamSeek {
    private StreamSeek() {}
    public static void skip(AudioStream stream,double seconds,BooleanSupplier cancelled) throws IOException {
        var format=stream.getFormat();
        long remaining=(long)(seconds*format.getFrameRate())*format.getFrameSize();
        try {
            while(remaining>0){
                if(cancelled.getAsBoolean())throw new CancellationException();
                var buffer=stream.read((int)Math.min(65536,remaining));
                if(buffer==null)break;
                int count=buffer.remaining();
                if(count==0)break;
                remaining-=count;
            }
            if(cancelled.getAsBoolean())throw new CancellationException();
        }catch(IOException|RuntimeException error){stream.close();throw error;}
    }
}

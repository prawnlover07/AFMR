package dev.animal.afmr.audio;

import org.jaudiotagger.audio.AudioFileIO;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

/** Read-only duration discovery. Call on the library worker, never the game/audio thread. */
public final class TrackDuration {
    private TrackDuration(){}
    public static double local(Path path)throws Exception{
        try{
            double seconds=AudioFileIO.read(path.toFile()).getAudioHeader().getPreciseTrackLength();
            if(Double.isFinite(seconds)&&seconds>0)return seconds;
        }catch(Exception ignored){ /* WAV and other untagged files may still expose frame counts. */ }
        var audio=AudioSystem.getAudioFileFormat(path.toFile());
        var duration=audio.properties().get("duration");
        if(duration instanceof Number micros&&micros.doubleValue()>0)return micros.doubleValue()/1_000_000;
        double rate=audio.getFormat().getFrameRate();
        return rate>0&&audio.getFrameLength()>0?audio.getFrameLength()/rate:Double.NaN;
    }
    /** Vorbis granule positions count PCM frames, so no audio decoding or temporary files are needed. */
    public static double vorbis(InputStream input)throws IOException{
        byte[] header=new byte[27],segments=new byte[255],prefix=new byte[30];
        long serial=-1,frames=-1;int sampleRate=0;boolean ended=false;
        while(true){
            int read=input.readNBytes(header,0,header.length);if(read==0)break;
            if(read!=27||header[0]!='O'||header[1]!='g'||header[2]!='g'||header[3]!='S'||header[4]!=0)throw new IOException("Invalid Ogg page");
            var page=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            long granule=page.getLong(6),pageSerial=Integer.toUnsignedLong(page.getInt(14));
            int count=header[26]&255;
            if(input.readNBytes(segments,0,count)!=count)throw new EOFException("Ogg lacing table");
            int bytes=0;for(int i=0;i<count;i++)bytes+=segments[i]&255;
            int length=Math.min(prefix.length,bytes);
            if(input.readNBytes(prefix,0,length)!=length)throw new EOFException("Ogg packet");
            input.skipNBytes(bytes-length);
            if(length>=30&&prefix[0]==1&&prefix[1]=='v'&&prefix[2]=='o'&&prefix[3]=='r'&&prefix[4]=='b'&&prefix[5]=='i'&&prefix[6]=='s'){
                if(sampleRate!=0)throw new IOException("Multiple Vorbis streams");
                serial=pageSerial;sampleRate=ByteBuffer.wrap(prefix).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
                if(sampleRate<=0)throw new IOException("Invalid Vorbis sample rate");
            }
            if(pageSerial==serial){if(granule>=0)frames=Math.max(frames,granule);if((header[5]&4)!=0)ended=true;}
        }
        if(!ended||sampleRate<=0||frames<=0)throw new IOException("Incomplete Vorbis duration");
        return (double)frames/sampleRate;
    }
}

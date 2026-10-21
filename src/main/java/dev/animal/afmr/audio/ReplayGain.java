package dev.animal.afmr.audio;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.wav.WavTag;
import java.nio.file.Path;

/** Existing track tags only. No loudness analysis, writes, or work on the game thread. */
public final class ReplayGain {
    private ReplayGain() {}
    public static double readMultiplier(Path file){
        try{return multiplier(AudioFileIO.read(file.toFile()).getTag());}
        catch(Exception ignored){return 1;}
    }
    static double multiplier(Tag tag){
        if(tag instanceof WavTag wav)tag=wav.getID3Tag();
        if(tag==null)return 1;
        String gain="",peak="";
        var fields=tag.getFields();
        while(fields.hasNext()){
            var field=fields.next();String key,value;
            if(field instanceof AbstractID3v2Frame frame){
                if(!(frame.getBody() instanceof FrameBodyTXXX body))continue;
                key=body.getDescription();
                if(!"REPLAYGAIN_TRACK_GAIN".equalsIgnoreCase(key)&&!"REPLAYGAIN_TRACK_PEAK".equalsIgnoreCase(key))continue;
                value=body.getText();
            }
            else if(field instanceof TagTextField text){
                key=field.getId();
                if(!"REPLAYGAIN_TRACK_GAIN".equalsIgnoreCase(key)&&!"REPLAYGAIN_TRACK_PEAK".equalsIgnoreCase(key))continue;
                value=text.getContent();
            }
            else continue;
            if("REPLAYGAIN_TRACK_GAIN".equalsIgnoreCase(key))gain=value;
            else if("REPLAYGAIN_TRACK_PEAK".equalsIgnoreCase(key))peak=value;
        }
        return multiplier(gain,peak);
    }
    static double multiplier(String gain,String peak){
        try{
            String value=gain.strip();
            if(value.length()>=2&&value.regionMatches(true,value.length()-2,"dB",0,2))value=value.substring(0,value.length()-2).strip();
            double db=Double.parseDouble(value);if(!Double.isFinite(db))return 1;
            double scale=Math.pow(10,db/20);if(!Double.isFinite(scale)||scale<=0)return 1;
            double maximum=1;
            try{double p=Double.parseDouble(peak.strip());if(Double.isFinite(p)&&p>0)maximum=1/p;}catch(NumberFormatException ignored){}
            // Without a usable peak, attenuation is safe but amplification cannot be verified.
            return Math.min(scale,maximum);
        }catch(NumberFormatException e){return 1;}
    }
    /** Scale signed little-endian PCM16 in place; independent of fades and Minecraft volume. */
    public static void apply(byte[] pcm,double multiplier){
        if(multiplier==1)return;
        for(int i=0;i+1<pcm.length;i+=2){
            int sample=(short)((pcm[i]&255)|(pcm[i+1]<<8));
            int scaled=(int)Math.round(Math.clamp(sample*multiplier,-32768,32767));
            pcm[i]=(byte)scaled;pcm[i+1]=(byte)(scaled>>8);
        }
    }
}

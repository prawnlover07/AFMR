package dev.animal.afmr.audio;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.wav.WavTag;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import dev.animal.afmr.core.TrackLoudness;
import dev.animal.afmr.core.ReplayGainInfo;

/** Track-tag parsing and the single PCM gain stage. Metadata reads stay off the game thread. */
public final class ReplayGain {
    private ReplayGain() {}
    public record Gain(double db, double peak) {
        public double multiplier() { return Math.min(Math.pow(10,db/20),peak>0?1/peak:1); }
        public double effectiveDb() { return 20*Math.log10(multiplier()); }
    }
    /** Null means no usable track gain, distinct from a valid zero-dB tag. */
    public static Gain read(Path file){
        try{return tags(AudioFileIO.read(file.toFile()).getTag());}
        catch(Exception ignored){return null;}
    }
    /** Resolve exactly one gain when playback opens, never stacking tags and an AFMR measurement. */
    public static double resolve(Path file,TrackLoudness cached) throws IOException {
        var tags=read(file);if(tags!=null)return tags.multiplier();
        if(cached!=null&&cached.valid()){
            var attributes=Files.readAttributes(file,BasicFileAttributes.class);
            if(cached.matches(attributes.size(),attributes.lastModifiedTime().toString()))
                return new Gain(cached.gainDb(),cached.peak()).multiplier();
        }
        return 1;
    }
    static Gain tags(Tag tag){
        var fields=fields(tag);return parse(fields[0],fields[1]);
    }
    /** Display metadata reads the same tags as playback, without changing album/track gain policy. */
    public static ReplayGainInfo metadata(Tag tag){
        var fields=fields(tag);var track=parse(fields[0],fields[1]);var album=parse(fields[2],fields[3]);
        double trackPeak=peak(fields[1]),albumPeak=peak(fields[3]);
        if(track==null&&album==null&&trackPeak==0&&albumPeak==0)return null;
        return new ReplayGainInfo(track==null?null:track.db(),trackPeak==0?null:trackPeak,album==null?null:album.db(),albumPeak==0?null:albumPeak);
    }
    private static String[] fields(Tag tag){
        String[] values={"","","",""};
        if(tag instanceof WavTag wav)tag=wav.getID3Tag();
        if(tag==null)return values;
        var fields=tag.getFields();
        while(fields.hasNext()){
            var field=fields.next();String key,value;
            if(field instanceof AbstractID3v2Frame frame){
                if(!(frame.getBody() instanceof FrameBodyTXXX body))continue;
                key=body.getDescription();
                value=body.getText();
            }
            else if(field instanceof TagTextField text){
                key=field.getId();
                value=text.getContent();
            }
            else continue;
            if("REPLAYGAIN_TRACK_GAIN".equalsIgnoreCase(key))values[0]=value;
            else if("REPLAYGAIN_TRACK_PEAK".equalsIgnoreCase(key))values[1]=value;
            else if("REPLAYGAIN_ALBUM_GAIN".equalsIgnoreCase(key))values[2]=value;
            else if("REPLAYGAIN_ALBUM_PEAK".equalsIgnoreCase(key))values[3]=value;
        }
        return values;
    }
    private static double peak(String text){
        try{double p=Double.parseDouble(text.strip());return Double.isFinite(p)&&p>0&&Double.isFinite(1/p)?p:0;}
        catch(NumberFormatException e){return 0;}
    }
    static Gain parse(String gain,String peak){
        try{
            String value=gain.strip();
            if(value.length()>=2&&value.regionMatches(true,value.length()-2,"dB",0,2))value=value.substring(0,value.length()-2).strip();
            double db=Double.parseDouble(value);if(!Double.isFinite(db))return null;
            double scale=Math.pow(10,db/20);if(!Double.isFinite(scale)||scale<=0)return null;
            double maximum=peak(peak);
            // Without a usable peak, attenuation is safe but amplification cannot be verified.
            return new Gain(db,maximum);
        }catch(NumberFormatException e){return null;}
    }
    /** Scale signed little-endian PCM16 in place; independent of fades and Minecraft volume. */
    public static void apply(byte[] pcm,double multiplier){
        if(multiplier==1)return;
        for(int i=0;i+1<pcm.length;i+=2){
            int sample=(short)((pcm[i]&255)|(pcm[i+1]<<8));
            int scaled=(int)Math.round(Math.max(-32768,Math.min(sample*multiplier,32767)));
            pcm[i]=(byte)scaled;pcm[i+1]=(byte)(scaled>>8);
        }
    }
}

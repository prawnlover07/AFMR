package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Track;
import java.util.*;

/** Song-list fields, display formatting and typed ordering. No file access. */
public enum SongColumn {
    TITLE("Title",168), ARTIST("Artist",104), ALBUM("Album",108), DURATION("Time",56),
    ALBUM_ARTIST("Album artist",130), TRACK_NUMBER("Track no.",68), DISC_NUMBER("Disc no.",64),
    GENRE("Genre",100), DATE("Date",80),
    RG_TRACK_GAIN("RG track gain",110), RG_TRACK_PEAK("RG track peak",110),
    RG_ALBUM_GAIN("RG album gain",110), RG_ALBUM_PEAK("RG album peak",110),
    AFMR_GAIN("AFMR gain",100), PATH("File path",250);

    public final String label;
    public final int width;
    SongColumn(String label,int width){this.label=label;this.width=width;}
    public static EnumSet<SongColumn> defaults(){return EnumSet.of(TITLE,ARTIST,ALBUM,DURATION);}
    public static EnumSet<SongColumn> normalize(Set<SongColumn> columns){
        if(columns==null)return defaults();
        var result=EnumSet.noneOf(SongColumn.class);
        for(var column:columns)if(column!=null)result.add(column);
        result.add(TITLE);return result;
    }
    public String text(Track t){
        return switch(this){
            case TITLE->t.title;case ARTIST->t.artist;case ALBUM->t.album;case ALBUM_ARTIST->t.albumArtist;
            case TRACK_NUMBER->t.trackNumber;case DISC_NUMBER->t.discNumber;case GENRE->t.genre;case DATE->t.date;
            case PATH->t.path;
            case DURATION->{int seconds=(int)t.durationSeconds;yield !Double.isFinite(t.durationSeconds)||seconds<=0?"--:--":seconds/60+":"+(seconds%60<10?"0":"")+seconds%60;}
            case RG_TRACK_GAIN,RG_ALBUM_GAIN,AFMR_GAIN->{Double n=number(t);yield n==null?"":String.format(Locale.ROOT,"%+.2f dB",n==0?0:n);}
            case RG_TRACK_PEAK,RG_ALBUM_PEAK->{Double n=number(t);yield n==null?"":String.format(Locale.ROOT,"%.6f",n);}
        };
    }
    private Double number(Track t){
        var rg=t.replayGain;
        Double n=switch(this){
            case DURATION->t.durationSeconds>0?t.durationSeconds:null;
            case TRACK_NUMBER->leadingNumber(t.trackNumber);case DISC_NUMBER->leadingNumber(t.discNumber);
            case RG_TRACK_GAIN->rg==null?null:rg.trackDb();case RG_TRACK_PEAK->rg==null?null:rg.trackPeak();
            case RG_ALBUM_GAIN->rg==null?null:rg.albumDb();case RG_ALBUM_PEAK->rg==null?null:rg.albumPeak();
            case AFMR_GAIN->t.loudness!=null&&t.loudness.valid()?t.loudness.gainDb():null;
            default->null;
        };
        return n!=null&&Double.isFinite(n)?n:null;
    }
    private static Double leadingNumber(String text){
        int end=0;while(end<text.length()&&Character.isDigit(text.charAt(end)))end++;
        if(end==0)return null;
        try{return Double.valueOf(text.substring(0,end));}catch(NumberFormatException e){return null;}
    }
    public Comparator<Track> comparator(boolean descending){
        boolean numeric=switch(this){case DURATION,TRACK_NUMBER,DISC_NUMBER,RG_TRACK_GAIN,RG_TRACK_PEAK,RG_ALBUM_GAIN,RG_ALBUM_PEAK,AFMR_GAIN->true;default->false;};
        return (a,b)->{
            int result;
            if(numeric){
                Double x=number(a),y=number(b);
                if(x==null||y==null)return x==null?(y==null?0:1):-1;
                result=Double.compare(x,y);
            }else{
                String x=text(a),y=text(b);
                if(x.isBlank()||y.isBlank())return x.isBlank()?(y.isBlank()?0:1):-1;
                result=x.compareToIgnoreCase(y);
            }
            return descending?-result:result;
        };
    }

}

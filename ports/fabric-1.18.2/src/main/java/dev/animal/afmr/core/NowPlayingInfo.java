package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Settings;
import dev.animal.afmr.core.Library.Track;

/** Built at a song/settings transition, never by traversing the library during rendering. */
public final class NowPlayingInfo {
    public final String[] lines;

    /** Wrap only between words. An oversized word stays intact for the renderer's scroll/ellipsis choice. */
    public String[] wrap(int pixels,java.util.function.ToIntFunction<String> measure){
        var wrapped=new java.util.ArrayList<String>();
        int spaceWidth=measure.applyAsInt(" ");
        for(String text:lines){
            var line=new StringBuilder();int lineWidth=0,start=0;
            while(start<text.length()){
                int cp=text.codePointAt(start);
                if(Character.isWhitespace(cp)||Character.isSpaceChar(cp)){start+=Character.charCount(cp);continue;}
                int end=start;
                while(end<text.length()){
                    cp=text.codePointAt(end);
                    if(Character.isWhitespace(cp)||Character.isSpaceChar(cp))break;
                    end+=Character.charCount(cp);
                }
                String word=text.substring(start,end);int wordWidth=measure.applyAsInt(word);
                if(!line.isEmpty()&&lineWidth+spaceWidth+wordWidth>pixels){
                    wrapped.add(line.toString());line.setLength(0);lineWidth=0;
                }
                if(!line.isEmpty()){line.append(' ');lineWidth+=spaceWidth;}
                line.append(word);lineWidth+=wordWidth;start=end;
            }
            if(!line.isEmpty())wrapped.add(line.toString());
        }
        return wrapped.toArray(String[]::new);
    }

    public NowPlayingInfo(Track track, String title, String playlist, Settings settings) {
        String[] selected=new String[5];int count=0;
        if(settings.widgetTitle)selected[count++]=title;
        if(settings.widgetArtist&&!track.artist.isBlank())selected[count++]=track.artist;
        if(settings.widgetAlbum&&!track.album.isBlank())selected[count++]=track.album;
        if(settings.widgetDuration&&Double.isFinite(track.durationSeconds)&&track.durationSeconds>0){
            int seconds=(int)track.durationSeconds;
            selected[count++]="Length: "+seconds/60+":"+(seconds%60<10?"0":"")+seconds%60;
        }
        if(settings.widgetPlaylist&&!playlist.isBlank())selected[count++]="Playlist: "+playlist;
        lines=java.util.Arrays.copyOf(selected,count);
    }
}

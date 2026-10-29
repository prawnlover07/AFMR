package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import java.util.ArrayList;
import java.util.List;

/** Bounded immutable metadata snapshots. No audio files or absolute paths are stored here. */
public final class ListeningHistory {
    public static final int LIMIT=100;
    public enum Origin { AUTOMATIC, OVERRIDE, QUEUE, PREVIEW }
    public static final class Entry {
        // Immutable fields are readable by Minecraft's bundled Gson 2.8.9.
        private final long id;
        private final long startedAt;
        private final String trackId;
        private final String title;
        private final String artist;
        private final String album;
        private final String playlistId;
        private final String playlistName;
        private final Origin origin;
        private final String reason;
        public Entry(long id,long startedAt,String trackId,String title,String artist,String album,String playlistId,String playlistName,Origin origin,String reason){
            this.id=id;
            this.startedAt=startedAt;
            this.trackId=trackId;
            this.title=title;
            this.artist=artist;
            this.album=album;
            this.playlistId=playlistId;
            this.playlistName=playlistName;
            this.origin=origin;
            this.reason=reason;
        }
        public long id(){return id;}
        public long startedAt(){return startedAt;}
        public String trackId(){return trackId;}
        public String title(){return title;}
        public String artist(){return artist;}
        public String album(){return album;}
        public String playlistId(){return playlistId;}
        public String playlistName(){return playlistName;}
        public Origin origin(){return origin;}
        public String reason(){return reason;}
        @Override public boolean equals(Object value){
            return value instanceof Entry other
                &&id==other.id
                &&startedAt==other.startedAt
                &&java.util.Objects.equals(trackId,other.trackId)
                &&java.util.Objects.equals(title,other.title)
                &&java.util.Objects.equals(artist,other.artist)
                &&java.util.Objects.equals(album,other.album)
                &&java.util.Objects.equals(playlistId,other.playlistId)
                &&java.util.Objects.equals(playlistName,other.playlistName)
                &&java.util.Objects.equals(origin,other.origin)
                &&java.util.Objects.equals(reason,other.reason);
        }
        @Override public int hashCode(){return java.util.Objects.hash(id,startedAt,trackId,title,artist,album,playlistId,playlistName,origin,reason);}
    }
    private final Entry[] entries=new Entry[LIMIT];
    private int next,size;
    private long sequence,revision;

    public void record(Track track,Playlist playlist,Origin origin,String reason){
        entries[next]=new Entry(++sequence,System.currentTimeMillis(),track.id,text(track.title,512),text(track.artist,512),text(track.album,512),
            playlist==null?"":playlist.id,playlist==null?"":text(playlist.name,80),origin,text(reason,1024));
        next=(next+1)%LIMIT;size=Math.min(LIMIT,size+1);revision++;
    }
    public long revision(){return revision;}
    public int size(){return size;}
    public List<Entry> snapshot(){
        var result=new ArrayList<Entry>(size);
        for(int i=0;i<size;i++)result.add(entries[Math.floorMod(next-1-i,LIMIT)]);
        return List.copyOf(result);
    }
    public void restore(List<Entry> saved){
        clear();sequence=0;
        int count=Math.min(saved.size(),LIMIT);
        for(int i=count-1;i>=0;i--){
            Entry entry=saved.get(i);
            if(!valid(entry))throw new IllegalArgumentException("Invalid listening-history entry");
            entries[next]=entry;next=(next+1)%LIMIT;size++;sequence=Math.max(sequence,entry.id());
        }
        revision++;
    }
    public void clear(){
        java.util.Arrays.fill(entries,null);size=next=0;revision++;
    }
    public void relink(java.util.Map<String,String> ids){
        boolean changed=false;
        for(int i=0;i<entries.length;i++){
            var e=entries[i];if(e==null)continue;String next=ids.get(e.trackId());if(next==null)continue;
            entries[i]=new Entry(e.id(),e.startedAt(),next,e.title(),e.artist(),e.album(),e.playlistId(),e.playlistName(),e.origin(),e.reason());changed=true;
        }
        if(changed)revision++;
    }
    public static boolean valid(Entry e){
        return e!=null&&e.id()>0&&e.startedAt()>0&&e.origin()!=null&&e.trackId()!=null&&!e.trackId().isBlank()
            &&e.title()!=null&&e.title().length()<=512&&e.artist()!=null&&e.artist().length()<=512&&e.album()!=null&&e.album().length()<=512
            &&e.playlistId()!=null&&e.playlistName()!=null&&e.playlistName().length()<=80&&e.reason()!=null&&e.reason().length()<=1024;
    }
    private static String text(String value,int limit){return value==null?"":value.substring(0,Math.min(value.length(),limit));}
}

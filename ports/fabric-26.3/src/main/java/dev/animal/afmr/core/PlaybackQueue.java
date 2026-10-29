package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import java.util.ArrayList;
import java.util.List;

/** Client-owned FIFO requests. Bounded mutations occur only through controls and song transitions. */
public final class PlaybackQueue {
    public static final int LIMIT=100;
    public record Entry(long id,String trackId,String playlistId,String playlistName,String title,String artist) {}
    private final ArrayList<Entry> entries=new ArrayList<>(LIMIT);
    private long sequence,revision;

    public boolean add(Track track,Playlist playlist){
        if(entries.size()==LIMIT)return false;
        entries.add(new Entry(++sequence,track.id,playlist==null?"":playlist.id,playlist==null?"Library":playlist.name,track.title,track.artist));
        revision++;return true;
    }
    public Entry first(){return entries.isEmpty()?null:entries.getFirst();}
    public boolean empty(){return entries.isEmpty();}
    public int size(){return entries.size();}
    public long revision(){return revision;}
    public List<Entry> snapshot(){return List.copyOf(entries);}
    public void removeFirst(){if(!entries.isEmpty()){entries.removeFirst();revision++;}}
    public boolean remove(long id){
        for(int i=0;i<entries.size();i++)if(entries.get(i).id()==id){entries.remove(i);revision++;return true;}
        return false;
    }
    public boolean move(long id,int destination){
        for(int i=0;i<entries.size();i++)if(entries.get(i).id()==id){
            int next=Math.clamp(destination,0,entries.size()-1);if(i==next)return false;
            entries.add(next,entries.remove(i));revision++;return true;
        }
        return false;
    }
    public void clear(){if(!entries.isEmpty()){entries.clear();revision++;}}
    public void relink(java.util.Map<String,String> ids){
        boolean changed=false;
        for(int i=0;i<entries.size();i++){
            var e=entries.get(i);String next=ids.get(e.trackId());if(next==null)continue;
            entries.set(i,new Entry(e.id(),next,e.playlistId(),e.playlistName(),e.title(),e.artist()));changed=true;
        }
        if(changed)revision++;
    }
}

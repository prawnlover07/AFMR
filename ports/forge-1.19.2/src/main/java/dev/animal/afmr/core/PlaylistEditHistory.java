package dev.animal.afmr.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.animal.afmr.core.Library.*;
import java.util.*;

/** Client-owned, session-only playlist edits. Never snapshots the shared track catalog. */
public final class PlaylistEditHistory {
    private static final Gson JSON=new Gson();
    private static final int MAX_STEPS=128,MAX_WEIGHT=500_000;
    private final Map<String,Timeline> profiles=new HashMap<>();
    private static final class Timeline {final List<Entry> entries=new ArrayList<>();int cursor;}
    private record Entry(long serial,String label,Change change,int weight,String merge,long time){}
    private interface Change {boolean apply(Library lib,boolean redo);}
    private String watchedProfile="",watchedId="",merge="";
    private Playlist watched;
    private JsonElement baseline;
    private long serial;

    public void watch(Library lib,Playlist playlist){
        checkpoint(lib);
        if(watched==playlist&&watchedProfile.equals(lib.profiles.activeId))return;
        watched=playlist;watchedId=playlist==null?"":playlist.id;watchedProfile=lib.profiles.activeId;
        baseline=playlist==null?null:settings(playlist);merge="";
    }
    public void separate(){merge="";}
    public void coalesce(String field){merge=field;}
    public void checkpoint(Library lib){
        if(watched==null||!watchedProfile.equals(lib.profiles.activeId)||lib.playlist(watchedId)!=watched)return;
        JsonElement next=settings(watched);if(next.equals(baseline))return;
        var before=baseline;baseline=next;
        var change=new SettingsChange(watchedId,before,next);
        var timeline=timeline(lib);long now=System.nanoTime();
        if(!merge.isEmpty()&&timeline.cursor==timeline.entries.size()&&!timeline.entries.isEmpty()){
            var previous=timeline.entries.get(timeline.cursor-1);
            if(previous.merge.equals(merge)&&now-previous.time<2_000_000_000L&&previous.change instanceof SettingsChange old&&old.id.equals(watchedId)){
                timeline.entries.remove(--timeline.cursor);change=new SettingsChange(watchedId,old.before,next);
            }
        }
        if(!change.before.equals(change.after))push(lib,"Edit playlist settings",change,metadataWeight(change.before)+metadataWeight(next),merge,now);
    }
    private static JsonElement settings(Playlist p){return JSON.toJsonTree(Library.copyPlaylist(p,false));}
    private static int metadataWeight(JsonElement value){return 1+value.toString().length()/32;}
    private record SettingsChange(String id,JsonElement before,JsonElement after) implements Change {
        public boolean apply(Library lib,boolean redo){
            var p=lib.playlist(id);if(p==null||!settings(p).equals(redo?before:after))return false;
            Library.applyPlaylistSettings(JSON.fromJson(redo?after:before,Playlist.class),p);return true;
        }
    }
    /** Null restores automatic/unassigned membership, distinct from explicit off. */
    public void assign(Library lib,Playlist playlist,Collection<String> ids,Boolean value){
        checkpoint(lib);separate();var before=new LinkedHashMap<String,Boolean>();var after=new LinkedHashMap<String,Boolean>();
        for(String id:ids){Boolean old=playlist.tracks.get(id);if(Objects.equals(old,value))continue;before.put(id,old);after.put(id,value);if(value==null)playlist.tracks.remove(id);else playlist.tracks.put(id,value);}
        if(!before.isEmpty())push(lib,value==null?"Remove song assignments":value?"Enable / add songs":"Disable songs",new TracksChange(playlist.id,before,after),before.size()*2,"",System.nanoTime());
    }
    private record TracksChange(String id,Map<String,Boolean> before,Map<String,Boolean> after) implements Change {
        public boolean apply(Library lib,boolean redo){
            var p=lib.playlist(id);if(p==null)return false;var expected=redo?before:after;
            for(var e:expected.entrySet())if(!Objects.equals(p.tracks.get(e.getKey()),e.getValue()))return false;
            for(var e:(redo?after:before).entrySet())if(e.getValue()==null)p.tracks.remove(e.getKey());else p.tracks.put(e.getKey(),e.getValue());return true;
        }
    }
    public boolean move(Library lib,String id,int target){
        checkpoint(lib);separate();var p=lib.playlist(id);if(p==null)return false;int old=lib.playlists.indexOf(p);
        if(!lib.movePlaylist(id,target))return false;
        push(lib,"Change playlist priority",new MoveChange(id,old,lib.playlists.indexOf(p)),1,"",System.nanoTime());return true;
    }
    private record MoveChange(String id,int before,int after) implements Change {
        public boolean apply(Library lib,boolean redo){var p=lib.playlist(id);if(p==null||lib.playlists.indexOf(p)!=(redo?before:after))return false;lib.movePlaylist(id,redo?after:before);return true;}
    }
    public void created(Library lib,Playlist playlist){
        checkpoint(lib);separate();var snapshot=Library.copyPlaylist(playlist,true);
        push(lib,"Create playlist",new Created(snapshot,lib.playlists.indexOf(playlist)),snapshot.tracks.size()+metadataWeight(settings(snapshot)),"",System.nanoTime());
    }
    private static boolean same(Playlist a,Playlist b){return a!=null&&settings(a).equals(settings(b))&&a.tracks.equals(b.tracks);}
    private record Created(Playlist snapshot,int index) implements Change {
        public boolean apply(Library lib,boolean redo){
            if(redo){if(lib.playlist(snapshot.id)!=null)return false;lib.playlists.add(Math.min(index,lib.playlists.size()),Library.copyPlaylist(snapshot,true));}
            else{if(!same(lib.playlist(snapshot.id),snapshot)||lib.playlists.size()==1)return false;lib.removePlaylist(snapshot.id);}return true;
        }
    }
    public boolean delete(Library lib,String id){
        checkpoint(lib);separate();var p=lib.playlist(id);if(p==null)return false;
        int index=lib.playlists.indexOf(p);var snapshot=Library.copyPlaylist(p,true);var incoming=new LinkedHashMap<String,TransitionRule>();
        for(var other:lib.playlists){var rule=other.transitions.get(id);if(rule!=null)incoming.put(other.id,rule.copy());}
        var folders=new ArrayList<String>();for(var folder:lib.folders)if(folder.playlistId.equals(id))folders.add(folder.path);
        lib.removePlaylist(id);Playlist replacement=lib.playlists.isEmpty()?Library.copyPlaylist(lib.createPlaylist("New playlist"),true):null;
        push(lib,"Delete playlist",new Deleted(snapshot,index,incoming,folders,replacement),snapshot.tracks.size()+metadataWeight(settings(snapshot))+incoming.size()+folders.size(),"",System.nanoTime());return true;
    }
    private record Deleted(Playlist snapshot,int index,Map<String,TransitionRule> incoming,List<String> folders,Playlist replacement) implements Change {
        public boolean apply(Library lib,boolean redo){
            if(redo){if(!same(lib.playlist(snapshot.id),snapshot))return false;lib.removePlaylist(snapshot.id);if(replacement!=null)lib.playlists.add(Library.copyPlaylist(replacement,true));}
            else{
                if(lib.playlist(snapshot.id)!=null||replacement!=null&&!same(lib.playlist(replacement.id),replacement))return false;
                if(replacement!=null)lib.removePlaylist(replacement.id);
                lib.playlists.add(Math.min(index,lib.playlists.size()),Library.copyPlaylist(snapshot,true));
                for(var e:incoming.entrySet()){var source=lib.playlist(e.getKey());if(source!=null)source.transitions.putIfAbsent(snapshot.id,e.getValue().copy());}
                for(var folder:lib.folders)if(folders.contains(folder.path)&&folder.playlistId.equals("library"))folder.playlistId=snapshot.id;
            }return true;
        }
    }
    private Timeline timeline(Library lib){return profiles.computeIfAbsent(lib.profiles.activeId,id->new Timeline());}
    private void push(Library lib,String label,Change change,int weight,String merge,long now){
        var timeline=timeline(lib);timeline.entries.subList(timeline.cursor,timeline.entries.size()).clear();
        timeline.entries.add(new Entry(++serial,label,change,weight,merge,now));timeline.cursor++;trim();
    }
    private void trim(){
        while(true){int count=0,weight=0;Timeline oldest=null;long first=Long.MAX_VALUE;
            for(var t:profiles.values()){count+=t.entries.size();for(var entry:t.entries)weight+=entry.weight;if(!t.entries.isEmpty()&&t.entries.get(0).serial<first){first=t.entries.get(0).serial;oldest=t;}}
            if(count<=1||count<=MAX_STEPS&&weight<=MAX_WEIGHT)return;
            oldest.entries.remove(0);oldest.cursor=Math.max(0,oldest.cursor-1);
        }
    }
    public boolean canUndo(Library lib){var t=profiles.get(lib.profiles.activeId);return t!=null&&t.cursor>0;}
    public boolean canRedo(Library lib){var t=profiles.get(lib.profiles.activeId);return t!=null&&t.cursor<t.entries.size();}
    public String undoLabel(Library lib){var t=profiles.get(lib.profiles.activeId);return canUndo(lib)?t.entries.get(t.cursor-1).label:"";}
    public String redoLabel(Library lib){var t=profiles.get(lib.profiles.activeId);return canRedo(lib)?t.entries.get(t.cursor).label:"";}
    public boolean undo(Library lib){return restore(lib,false);}
    public boolean redo(Library lib){return restore(lib,true);}
    private boolean restore(Library lib,boolean redo){
        checkpoint(lib);separate();if(redo?!canRedo(lib):!canUndo(lib))return false;
        var timeline=timeline(lib);var entry=timeline.entries.get(redo?timeline.cursor:timeline.cursor-1);
        if(!entry.change.apply(lib,redo)){timeline.entries.clear();timeline.cursor=0;resetBaseline(lib);return false;}
        timeline.cursor+=redo?1:-1;resetBaseline(lib);return true;
    }
    private void resetBaseline(Library lib){watched=lib.playlist(watchedId);watchedProfile=lib.profiles.activeId;baseline=watched==null?null:settings(watched);}
}

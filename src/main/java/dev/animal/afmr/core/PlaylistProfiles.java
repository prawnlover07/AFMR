package dev.animal.afmr.core;

import java.util.*;
import dev.animal.afmr.core.Library.*;

/** The library owns active playlists/settings; inactive profiles own their data exclusively. */
public final class PlaylistProfiles {
    public static final String DEFAULT="default";
    public static final int LIMIT=32;
    public String activeId=DEFAULT,activeName="Default";
    public List<Saved> inactive=new ArrayList<>();
    /** Exact world folder or server address, never a display name. */
    public Map<String,String> bindings=new LinkedHashMap<>();
    public record Info(String id,String name){}
    public static final class Saved {
        public String id,name;
        public List<Playlist> playlists=new ArrayList<>();
        public ProfilePlayback playback=new ProfilePlayback();
        public Map<String,String> folderDestinations=new LinkedHashMap<>();
    }
    public List<Info> entries(){
        var result=new ArrayList<Info>(inactive.size()+1);result.add(new Info(activeId,activeName));
        for(var p:inactive)result.add(new Info(p.id,p.name));
        result.sort(Comparator.comparing((Info p)->!p.id.equals(DEFAULT)).thenComparing(Info::name,String.CASE_INSENSITIVE_ORDER));
        return result;
    }
    public boolean exists(String id){return activeId.equals(id)||inactive.stream().anyMatch(p->p.id.equals(id));}
    public List<Playlist> playlists(Library lib,String id){
        if(activeId.equals(id))return lib.playlists;
        for(var p:inactive)if(p.id.equals(id))return p.playlists;
        return List.of();
    }
    public String create(Library lib,String name,String copyFrom){
        if(inactive.size()+1>=LIMIT)throw new IllegalArgumentException("Maximum "+LIMIT+" profiles.");
        String clean=cleanName(name);var p=new Saved();p.id="profile:"+UUID.randomUUID();p.name=clean;
        if(copyFrom==null){p.playlists.addAll(PlaylistTemplates.create());}
        else{
            if(!exists(copyFrom))throw new IllegalArgumentException("Profile no longer exists.");
            for(var playlist:playlists(lib,copyFrom))p.playlists.add(Library.copyPlaylist(playlist,true));
            if(copyFrom.equals(activeId)){
                p.playback=ProfilePlayback.capture(lib.settings);
                for(var f:lib.folders)p.folderDestinations.put(f.path,f.playlistId);
            }else for(var source:inactive)if(source.id.equals(copyFrom)){
                var settings=new Settings();source.playback.apply(settings);p.playback=ProfilePlayback.capture(settings);
                p.folderDestinations.putAll(source.folderDestinations);break;
            }
        }
        inactive.add(p);return p.id;
    }
    public boolean activate(Library lib,String id){
        if(activeId.equals(id))return false;
        Saved next=null;for(var p:inactive)if(p.id.equals(id)){next=p;break;}
        if(next==null)throw new IllegalArgumentException("Profile no longer exists.");
        var old=new Saved();old.id=activeId;old.name=activeName;old.playlists=lib.playlists;
        old.playback=ProfilePlayback.capture(lib.settings);
        for(var f:lib.folders)old.folderDestinations.put(f.path,f.playlistId);
        inactive.remove(next);inactive.add(old);
        lib.playlists=next.playlists;next.playback.apply(lib.settings);
        for(var f:lib.folders)f.playlistId=next.folderDestinations.getOrDefault(f.path,"library");
        activeId=next.id;activeName=next.name;return true;
    }
    public void rename(String id,String name){
        String clean=cleanName(name);
        if(activeId.equals(id)){activeName=clean;return;}
        for(var p:inactive)if(p.id.equals(id)){p.name=clean;return;}
        throw new IllegalArgumentException("Profile no longer exists.");
    }
    public boolean delete(String id){
        if(id.equals(DEFAULT)||id.equals(activeId))return false;
        boolean removed=inactive.removeIf(p->p.id.equals(id));
        if(removed)bindings.values().removeIf(id::equals);return removed;
    }
    public void bind(String world,String id){
        if(world==null||world.isBlank())throw new IllegalArgumentException("Join a world or server first.");
        if(!exists(id))throw new IllegalArgumentException("Profile no longer exists.");
        bindings.put(world,id);
    }
    public String forWorld(String world){return bindings.getOrDefault(world,DEFAULT);}
    private static String cleanName(String name){
        String clean=name==null?"":name.replaceAll("\\p{Cc}","").strip();
        if(clean.isBlank()||clean.length()>80)throw new IllegalArgumentException("Use a profile name of 1-80 characters.");return clean;
    }
    public void normalize(){
        if(activeId==null||activeId.isBlank()||inactive==null||bindings==null||inactive.size()>=LIMIT)
            throw new IllegalArgumentException("Invalid profiles");
        activeName=cleanName(activeName);var ids=new HashSet<String>();ids.add(activeId);
        for(var p:inactive){
            if(p==null||p.id==null||p.id.isBlank()||!ids.add(p.id)||p.playlists==null||p.playback==null||p.folderDestinations==null)
                throw new IllegalArgumentException("Invalid saved profile");
            p.name=cleanName(p.name);for(var playlist:p.playlists)Library.normalizePlaylist(playlist);
            if(p.folderDestinations.entrySet().stream().anyMatch(e->e.getKey()==null||e.getValue()==null))
                throw new IllegalArgumentException("Invalid profile folder destination");
            // Reuse the same playback bounds as the active setup without copying any music.
            var temporary=new Settings();p.playback.apply(temporary);temporary.normalizePlayback();
            p.playback=ProfilePlayback.capture(temporary);
        }
        if(!ids.contains(DEFAULT)||bindings.entrySet().stream().anyMatch(e->e.getKey()==null||e.getKey().isBlank()||!ids.contains(e.getValue())))
            throw new IllegalArgumentException("Invalid profile link");
    }
    public PlaylistProfiles copy(){
        var copy=new PlaylistProfiles();copy.activeId=activeId;copy.activeName=activeName;copy.bindings.putAll(bindings);
        for(var p:inactive){
            var next=new Saved();next.id=p.id;next.name=p.name;
            for(var playlist:p.playlists)next.playlists.add(Library.copyPlaylist(playlist,true));
            var s=new Settings();p.playback.apply(s);next.playback=ProfilePlayback.capture(s);
            next.folderDestinations.putAll(p.folderDestinations);copy.inactive.add(next);
        }
        return copy;
    }
}

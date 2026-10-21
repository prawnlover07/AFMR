package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import java.util.*;

/** Diagnostic strings are built only by the inspector, never by steady playback. */
public final class PlaybackInspector {
    private PlaybackInspector(){}
    public record Entry(String id,int rank,String name,String status,List<String> details){}
    public static List<Entry> inspect(Library lib,Context c,Set<String> vanilla,PlaybackEngine engine){
        var result=new ArrayList<Entry>();var automatic=lib.selectPlayable(c,vanilla,engine.failedTracks());
        for(int i=0;i<lib.playlists.size();i++){
            var p=lib.playlists.get(i);String status;
            if(!p.enabled)status="Automatic selection disabled";
            else if(p.manualOnly)status="Manual only; excluded from automatic selection";
            else if(!p.matches(c,lib.settings))status=unmatched(p,c,lib);
            else if(!lib.hasPlayable(p,vanilla,engine.failedTracks()))status="No playable songs: disabled, missing, failed or no applicable vanilla music";
            else if(p==automatic)status="Automatic winner";
            else status="Lower priority than "+(automatic==null?"the active choice":automatic.name);
            if(p.id.equals(engine.overrideId()))status="OVERRIDE / "+status;
            var details=new ArrayList<String>();details.add("#"+(i+1)+" "+p.name);details.add(status);
            details.add("Conditions: "+(p.custom()?p.conditions.summary():p.rule.name().toLowerCase(Locale.ROOT).replace('_',' ')));
            details.add("Finish on entry: "+p.finishCurrentSong+"; on exit: "+p.finishCurrentSongOnExit);
            details.add("Biome: "+c.biome());details.add("Dimension: "+c.dimension());
            details.add("Time: "+c.timeOfDay()+"; block Y: "+(int)Math.floor(c.y()));
            details.add("Covered: "+c.covered()+"; cover depth: "+String.format(Locale.ROOT,"%.1f",c.coverDepth()));
            details.add("Light: "+c.lightLevel()+"; temperature: "+String.format(Locale.ROOT,"%.3f",c.biomeTemperature()));
            details.add("Combat: "+c.combat()+" (global switch: "+lib.settings.combatEnabled+"); underwater: "+c.underwater());
            details.add("At Home: "+c.atHome()+"; underground: "+c.underground()+"; dead: "+c.dead());
            details.add("Position: "+String.format(Locale.ROOT,"%.1f, %.1f, %.1f",c.x(),c.y(),c.z()));
            if(p.custom()&&p.conditions.areaEnabled){var a=p.conditions.area;details.add("Area configured: "+a.configured+"; radius: "+a.radius);details.add("Area dimension: "+a.dimension);details.add("Area world matches: "+a.world.equals(c.world()));}
            if(p.custom()&&p.conditions.biomeEnabled)for(String id:p.conditions.biomes)details.add("Allowed biome: "+id);
            result.add(new Entry(p.id,i+1,p.name,status,List.copyOf(details)));
        }
        return result;
    }
    private static String unmatched(Playlist p,Context c,Library lib){
        return switch(p.rule){
            case CUSTOM->"Condition not met: "+p.conditions.failure(c,lib.settings).name().toLowerCase(Locale.ROOT);
            case COMBAT->lib.settings.combatEnabled?"Not currently in combat":"Combat music disabled globally";
            case HOME->!lib.home.configured?"Home location not configured":"Outside Home radius, world or dimension";
            case UNDERGROUND->"Needs solid cover, depth >= "+lib.settings.undergroundDepth+" and Y <= "+lib.settings.undergroundMaxY;
            case DARK_EXPLORATION->"Needs darkness + solid cover, or an enabled Nether/End switch";
            case DAY_OUTSIDE->"Needs daytime outside the underground condition";
            case NIGHT_OUTSIDE->"Needs nighttime outside the underground condition";
            default->"Environment does not match "+p.rule.name().toLowerCase(Locale.ROOT);
        };
    }
    public static String waiting(PlaybackEngine e,Library lib,Context c,double now){
        if(e.paused())return "Music paused; song position, silent gap, fades and context buffer are held";
        if(!e.error().isBlank())return e.error();
        if(e.previewing())return "Manual preview: finishes before automatic scheduling resumes";
        if(e.phase()==PlaybackEngine.Phase.FADING){var p=lib.playlist(e.overrideId());return e.overridePending()?"Fading into override: "+(p==null?"None":p.name)+"; no gap":"Fading out; normal silence follows";}
        if(e.current()==null){
            if(c.dead()&&lib.settings.preventMusicWhileDead)return "Waiting for respawn";
            if(e.selected()==null)return "No available playlist for this environment";
            if(e.remainingBuffer(now,lib.settings)>0)return "Context buffer: "+seconds(e.remainingBuffer(now,lib.settings));
            return "Silent gap: "+seconds(e.remainingSilence(now));
        }
        var source=lib.playlist(e.sourcePlaylistId());var selected=e.selected();
        if(e.finishingAfterDeath())return "Finishing combat song: death policy";
        if(e.remainingBuffer(now,lib.settings)>0)return "Context buffer: "+seconds(e.remainingBuffer(now,lib.settings));
        if(source!=selected){
            if(source!=null&&source.finishCurrentSongOnExit)return "Finishing "+source.name+" song: its exit setting is on";
            if(selected!=null&&selected.finishCurrentSong)return "Finishing current song: "+selected.name+" entry setting is on";
        }
        if(e.overrideMode()!=null)return e.overrideMode()==OverrideMode.ONE_SONG?"One-song override: automatic selection resumes after this song":"Playlist override held until released";
        return "Playing normally";
    }
    /** Count assignments only when the user opens details, not on each inspector refresh. */
    public static List<String> songCounts(Library lib,Playlist p,Set<String> vanilla,Set<String> failed){
        int available=0,disabled=0,missing=0,broken=0,automatic=0;
        for(var item:p.tracks.entrySet()){
            if(!Boolean.TRUE.equals(item.getValue()))disabled++;
            else if(lib.track(item.getKey())==null)missing++;
            else if(failed.contains(item.getKey()))broken++;
            else available++;
        }
        if(p.includeVanilla)for(String id:vanilla)if(!p.tracks.containsKey(id)){
            var t=lib.track(id);if(t!=null&&t.vanilla){if(failed.contains(id))broken++;else{available++;automatic++;}}
        }
        return List.of("Available songs: "+available+" (automatic vanilla: "+automatic+")","Disabled: "+disabled+"; missing: "+missing+"; failed this session: "+broken);
    }
    private static String seconds(double value){return String.format(Locale.ROOT,"%.1fs",value);}
}

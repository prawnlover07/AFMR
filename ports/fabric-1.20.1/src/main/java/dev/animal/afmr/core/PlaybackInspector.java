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
            details.add("Conditions: "+p.conditions.summary());
            details.add("Condition matching: "+(p.conditions.matchAny?"any enabled filter":"all enabled filters"));
            details.add("Finish on entry: "+p.finishCurrentSong+"; on exit: "+p.finishCurrentSongOnExit);
            details.add("Play on trigger: "+p.playOnTrigger);
            details.add("Outgoing transition rules: "+p.transitions.size());
            if(p==engine.selected()&&engine.transitionRule()!=null)details.add("Active transition: "+engine.transitionRule().summary());
            details.add("Songs before silence: "+PlaybackEngine.songsBeforeSilence(p,lib.settings)+(p.customSongCount?" (custom)":" (global)"));
            details.add("Silence: "+(p.customSilence?"custom "+p.silenceMinSeconds+"-"+p.silenceMaxSeconds:"default "+lib.settings.silenceMinSeconds+"-"+lib.settings.silenceMaxSeconds)+" seconds");
            details.add("Biome: "+c.biome());details.add("Dimension: "+c.dimension());
            details.add("Time: "+c.timeOfDay()+"; block Y: "+(int)Math.floor(c.y()));
            details.add("Covered: "+c.covered()+"; cover depth: "+String.format(Locale.ROOT,"%.1f",c.coverDepth()));
            details.add("Light: "+c.lightLevel()+"; temperature: "+String.format(Locale.ROOT,"%.3f",c.biomeTemperature()));
            details.add("Rain/snow weather: "+c.raining()+"; thunderstorm: "+c.thundering()+"; local precipitation: "+c.precipitation().name().toLowerCase(Locale.ROOT));
            details.add("Combat: "+c.combat()+" (global switch: "+lib.settings.combatEnabled+"); underwater: "+c.underwater());
            details.add("PvP: "+c.playerCombat()+"; health: "+String.format(Locale.ROOT,"%.2f red hearts",c.healthHearts()));
            details.add("Main hand: "+(c.mainHandItem().isEmpty()?"empty":c.mainHandItem())+"; off-hand: "+(c.offHandItem().isEmpty()?"empty":c.offHandItem()));
            details.add("Dead: "+c.dead());
            details.add("Gamemode: "+(c.gameMode()==null?"unavailable":c.gameMode().label()));
            details.add("Riding animal: "+c.ridingAnimal());
            details.add("Position: "+String.format(Locale.ROOT,"%.1f, %.1f, %.1f",c.x(),c.y(),c.z()));
            if(p.conditions.areaEnabled){var a=p.conditions.area;details.add("Area configured: "+a.configured+"; radius: "+a.radius);details.add("Area dimension: "+a.dimension);details.add("Area world matches: "+a.world.equals(c.world()));}
            if(p.conditions.nearbyPlayerEnabled)details.add("Nearby player: "+p.conditions.nearbyPlayerName+"; radius: "+p.conditions.nearbyPlayerRadius+"; matches: "+c.nearbyPlayers().matches(p.conditions.nearbyPlayerUuid,p.conditions.nearbyPlayerName,p.conditions.nearbyPlayerRadius));
            if(p.conditions.nearbyPlayerEnabled&&p.conditions.proximityVolume.enabled){var v=p.conditions.proximityVolume;details.add("Proximity volume: "+v.nearDistance+" blocks="+Math.round(v.nearVolume*100)+"%; "+v.farDistance+" blocks="+Math.round(v.farVolume*100)+"%");}
            if(p.conditions.biomeEnabled)for(String id:p.conditions.biomes)details.add("Allowed biome: "+id);
            if(p.conditions.heldItemEnabled)details.add("Held items ("+p.conditions.heldItemHand.label()+"): "+String.join(", ",p.conditions.heldItems));
            if(p.conditions.structureEnabled){
                details.add(c.structures().available()?"Structure data: loaded server structures":"Structure data unavailable: server companion missing, or observation pending");
                details.add("Radius outside structure bounds: "+p.conditions.structureRadius);
                for(String id:p.conditions.structures)details.add("Allowed structure: "+id);
                for(var box:c.structures().bounds())if(p.conditions.structures.contains(box.id()))
                    details.add("Nearby structure: "+box.id()+"; distance: "+String.format(Locale.ROOT,"%.1f",Math.sqrt(box.distanceSquared(c.x(),c.y(),c.z()))));
            }
            if(p.id.equals(engine.sourcePlaylistId()))details.add("Source proximity multiplier: "+Math.round(engine.proximityGain()*100)+"%");
            describeGroups(details,p.conditions,c,lib.settings,"");
            result.add(new Entry(p.id,i+1,p.name,status,List.copyOf(details)));
        }
        return result;
    }
    private static void describeGroups(List<String> details,PlaylistConditions parent,Context context,Settings settings,String path){
        for(int i=0;i<parent.groups.size();i++){
            var group=parent.groups.get(i);String key=path+(i+1);
            details.add("Group "+key+" / "+group.label+": "+group.summary()+" / "+group.failure(context,settings).name().toLowerCase(Locale.ROOT));
            if(group.areaEnabled)details.add("Group "+key+" area: "+group.area.world+" / "+group.area.dimension+" / radius "+group.area.radius+" / configured "+group.area.configured);
            if(group.biomeEnabled)details.add("Group "+key+" biomes: "+String.join(", ",group.biomes));
            if(group.heldItemEnabled)details.add("Group "+key+" held items ("+group.heldItemHand.label()+"): "+String.join(", ",group.heldItems));
            if(group.structureEnabled)details.add("Group "+key+" structures: "+String.join(", ",group.structures)+" / radius "+group.structureRadius);
            describeGroups(details,group,context,settings,key+".");
        }
    }
    private static String unmatched(Playlist p,Context c,Library lib){
        var failure=p.conditions.failure(c,lib.settings);
        if(failure==PlaylistConditions.Failure.COMBAT){
            if(!lib.settings.combatEnabled)return "Combat music disabled globally";
            return p.conditions.combatPlayersOnly?(p.conditions.combat==PlaylistConditions.State.YES?"Not currently in PvP":"Currently in PvP")
                :(p.conditions.combat==PlaylistConditions.State.YES?"Not currently in combat":"Currently in combat");
        }
        if(failure==PlaylistConditions.Failure.HEALTH)return "Health is not "+(p.conditions.healthComparison==PlaylistConditions.HealthComparison.BELOW?"below ":"above ")+p.conditions.healthThreshold+" red hearts";
        if(failure==PlaylistConditions.Failure.HELD_ITEM)return "No selected item held in "+p.conditions.heldItemHand.label().toLowerCase(Locale.ROOT);
        if(failure==PlaylistConditions.Failure.GAME_MODE)return "Gamemode not selected: "+(c.gameMode()==null?"unavailable":c.gameMode().label());
        if(failure==PlaylistConditions.Failure.RIDING_ANIMAL)return p.conditions.ridingAnimal==PlaylistConditions.State.YES?"Not riding an animal":"Currently riding an animal";
        if(failure==PlaylistConditions.Failure.STRUCTURE_DATA)return "Structure data unavailable: server companion missing, or observation pending";
        if(failure==PlaylistConditions.Failure.NO_CONDITIONS_MATCH)return "None of the enabled conditions match";
        return "Condition not met: "+failure.name().toLowerCase(Locale.ROOT);
    }
    /** A shared, name/path-free reason for the UI and copied diagnostics. */
    public enum Reason {
        PAUSED, JUKEBOX_HELD, JUKEBOX_FADING, PLAYBACK_ERROR, QUEUED_PLAYING, QUEUED_WAITING,
        TIME_WINDOW, PREVIEW, OVERRIDE_FADING, TRIGGER_FADING, FADING, RESPAWN,
        NO_PLAYLIST, CONTEXT_BUFFER, SILENT_GAP, FINISH_AFTER_DEATH, FINISH_ON_EXIT,
        FINISH_ON_ENTRY, CONTINUE_ON_MATCH, ONE_SONG_OVERRIDE, HELD_OVERRIDE, PLAYING
    }
    public static Reason reason(PlaybackEngine e,Library lib,Context c,Set<String> vanilla,double now){
        if(e.paused())return Reason.PAUSED;
        if(e.jukeboxActive())return e.jukeboxHolding()?Reason.JUKEBOX_HELD:Reason.JUKEBOX_FADING;
        if(!e.error().isBlank())return Reason.PLAYBACK_ERROR;
        if(e.queuedPlaying())return Reason.QUEUED_PLAYING;
        if(e.current()==null&&!e.queue().empty()&&!e.queueWaitingForOverride())return Reason.QUEUED_WAITING;
        if(e.waitingForTimeFit())return Reason.TIME_WINDOW;
        if(e.previewing())return Reason.PREVIEW;
        if(e.phase()==PlaybackEngine.Phase.FADING){
            if(e.overridePending())return Reason.OVERRIDE_FADING;
            if(e.triggerPending())return Reason.TRIGGER_FADING;
            return Reason.FADING;
        }
        if(e.current()==null){
            if(c.dead()&&lib.settings.preventMusicWhileDead)return Reason.RESPAWN;
            if(e.selected()==null)return Reason.NO_PLAYLIST;
            if(e.remainingBuffer(now,lib.settings)>0)return Reason.CONTEXT_BUFFER;
            return Reason.SILENT_GAP;
        }
        var source=lib.playlist(e.sourcePlaylistId());var selected=e.selected();
        if(e.finishingAfterDeath())return Reason.FINISH_AFTER_DEATH;
        if(e.remainingBuffer(now,lib.settings)>0)return Reason.CONTEXT_BUFFER;
        if(source!=selected){
            if(source!=null&&source.finishCurrentSongOnExit)return Reason.FINISH_ON_EXIT;
            if(selected!=null&&selected.finishCurrentSong)return Reason.FINISH_ON_ENTRY;
            if(e.overrideId()==null&&lib.settings.continuePlayingOnMatch&&lib.isPlayable(selected,e.current().id,vanilla,e.failedTracks()))return Reason.CONTINUE_ON_MATCH;
        }
        if(e.overrideMode()!=null)return e.overrideMode()==OverrideMode.ONE_SONG?Reason.ONE_SONG_OVERRIDE:Reason.HELD_OVERRIDE;
        return Reason.PLAYING;
    }
    public static String waiting(PlaybackEngine e,Library lib,Context c,Set<String> vanilla,double now){
        return switch(reason(e,lib,c,vanilla,now)){
            case PAUSED -> "Music paused; song position, silent gap, fades and context buffer are held";
            case JUKEBOX_HELD -> "Jukebox playing; AFMR is held";
            case JUKEBOX_FADING -> "Fading AFMR out for jukebox music";
            case PLAYBACK_ERROR -> e.error();
            case QUEUED_PLAYING -> "Manual queued song; "+e.queue().size()+" remaining; normal silence follows";
            case QUEUED_WAITING -> "Queued song waiting for normal silence: "+seconds(e.remainingSilence(now));
            case TIME_WINDOW -> "Waiting for a song that fits the remaining time window; unknown lengths are checked automatically";
            case PREVIEW -> "Manual preview: finishes before automatic scheduling resumes";
            case OVERRIDE_FADING -> {var p=lib.playlist(e.overrideId());yield "Fading into override: "+(p==null?"None":p.name)+"; no gap";}
            case TRIGGER_FADING -> "Fading into trigger: "+(e.selected()==null?"None":e.selected().name)+"; no gap";
            case FADING -> "Fading out; normal silence follows";
            case RESPAWN -> "Waiting for respawn";
            case NO_PLAYLIST -> "No available playlist for this environment";
            case CONTEXT_BUFFER -> "Context buffer: "+seconds(e.remainingBuffer(now,lib.settings));
            case SILENT_GAP -> "Silent gap: "+seconds(e.remainingSilence(now));
            case FINISH_AFTER_DEATH -> "Finishing combat song: death policy";
            case FINISH_ON_EXIT -> "Finishing "+lib.playlist(e.sourcePlaylistId()).name+" song: its exit setting is on";
            case FINISH_ON_ENTRY -> "Finishing current song: "+e.selected().name+" entry setting is on";
            case CONTINUE_ON_MATCH -> "Continue Playing on Match: current song is eligible in "+e.selected().name;
            case ONE_SONG_OVERRIDE -> "One-song override: automatic selection resumes after this song";
            case HELD_OVERRIDE -> "Playlist override held until released";
            case PLAYING -> "Playing normally";
        };
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

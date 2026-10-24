package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/** On-demand, allowlisted clipboard report. Never append raw inspector/error/config text. */
public final class DiagnosticSummary {
    public static final int MAX_CHARACTERS=48_000,MAX_PLAYLISTS=64;
    private static final Pattern TOKEN=Pattern.compile("[A-Za-z0-9 ._+()\\-]{1,96}");
    private static final Set<String> PRIVATE_NAMESPACES=Set.of("file","jar","http","https","ftp","smb","server","singleplayer");
    private static final Pattern RESOURCE=Pattern.compile("[a-z0-9_.-]{2,64}:[a-z0-9_][a-z0-9_./-]{0,191}");
    public record SystemInfo(String modVersion,String minecraftVersion,String loader,String loaderVersion,
                             String javaVersion,String os,String architecture,double masterVolume,double musicVolume,
                             long heapUsed,long heapCommitted,long heapMax){}
    public record Session(boolean inWorld,boolean singleplayer,boolean enabled,boolean scanning,
                          String scanStage,int processed,int discovered,int sourceIssues){}
    private DiagnosticSummary(){}

    public static String create(SystemInfo system,Session session,Library lib,PlaybackEngine engine,
                                Context context,Set<String> vanilla,double now,String inspectedId){
        var report=new Report();var settings=lib.settings;
        report.line("AFMR diagnostic summary — "+Instant.now());
        report.line("AFMR: "+token(system.modVersion())+"; Minecraft: "+token(system.minecraftVersion()));
        report.line("Loader: "+token(system.loader())+" "+token(system.loaderVersion()));
        report.line("Java: "+token(system.javaVersion())+"; OS: "+token(system.os())+"; architecture: "+token(system.architecture()));
        report.line("Heap used / committed / max MiB: "+mib(system.heapUsed())+" / "+mib(system.heapCommitted())+" / "+mib(system.heapMax())+" (single snapshot)");
        report.line("Privacy: names, song metadata, file paths, world/server identifiers, X/Z coordinates, saved locations and raw error text omitted.");
        report.line("Playlist # numbers refer to the current profile's priority list.");
        report.line("");
        report.line("Session: "+(!session.inWorld()?"title screen":session.singleplayer()?"singleplayer":"multiplayer")+"; runtime enabled: "+session.enabled());
        report.line("Profile: "+(PlaylistProfiles.DEFAULT.equals(lib.profiles.activeId)?"default":"custom (name omitted)")+"; profiles: "+(lib.profiles.inactive.size()+1));
        report.line("Library: "+lib.tracks.size()+" tracks; "+lib.folders.size()+" linked folders; "+lib.playlists.size()+" active-profile playlists");
        report.line("Scan: "+(session.scanning()?token(session.scanStage())+"; processed "+session.processed()+" / discovered "+session.discovered():"idle")
            +"; missing/unavailable sources: "+(session.sourceIssues()<0?"not checked":session.sourceIssues()));
        report.line("Volume master / music: "+percent(system.masterVolume())+" / "+percent(system.musicVolume()));
        report.line("Playback phase: "+engine.phase()+"; reason: "+(session.inWorld()?PlaybackInspector.reason(engine,lib,context,vanilla,now):"NO_WORLD")
            +"; paused: "+engine.paused());
        report.line("Current track: "+(engine.current()==null?"none":engine.current().vanilla?"Minecraft resource":"local file")
            +"; source: "+reference(lib,engine.sourcePlaylistId())+"; selected: "+reference(lib,engine.selected()==null?null:engine.selected().id)
            +"; automatic choice: "+reference(lib,engine.automaticSelected()==null?null:engine.automaticSelected().id));
        report.line("Override: "+reference(lib,engine.overrideId())+"; mode: "+engine.overrideMode()+"; preview: "+engine.previewing()
            +"; queued playback: "+engine.queuedPlaying()+"; queue size: "+engine.queue().size()+"; queue waiting for override: "+engine.queueWaitingForOverride());
        report.line("Jukebox active / held: "+engine.jukeboxActive()+" / "+engine.jukeboxHolding()+"; time-window wait: "+engine.waitingForTimeFit());
        report.line("Remaining silence / buffer: "+number(engine.remainingSilence(now))+"s / "+number(engine.remainingBuffer(now,settings))+"s");
        report.line("Playback error: "+errorCategory(engine.error())+"; failed songs this session: "+engine.failedTracks().size());
        report.line("Timing: silence "+number(settings.silenceMinSeconds)+"–"+number(settings.silenceMaxSeconds)+"s; buffer "+number(settings.bufferSeconds)
            +"s; fade in/out "+number(settings.fadeInSeconds)+"/"+number(settings.fadeOutSeconds)+"s; immediate on join "+settings.playImmediatelyOnJoin);
        report.line("Playback settings: combat "+settings.combatEnabled+"; combat quiet "+number(settings.combatQuietSeconds)+"s; death policy "+settings.combatDeathMusic
            +"; death fade "+number(settings.combatDeathFadeSeconds)+"s; prevent music while dead "+settings.preventMusicWhileDead
            +"; continue on match "+settings.continuePlayingOnMatch+"; ReplayGain "+settings.replayGainEnabled+"; jukebox "+settings.jukeboxMode);
        report.line("");
        if(session.inWorld())environment(report,context);
        else report.line("Environment: unavailable outside a world; condition matches are not evaluated.");
        report.line("");report.line("Playlists (priority order):");
        int count=Math.min(MAX_PLAYLISTS,lib.playlists.size());
        for(int i=0;i<count;i++)playlist(report,lib.playlists.get(i),i+1,lib,engine,session.inWorld()?context:null,vanilla);
        if(count<lib.playlists.size())report.line("Remaining playlists omitted: "+(lib.playlists.size()-count));
        var inspected=lib.playlist(inspectedId);
        if(inspected!=null){
            report.line("");report.line("Inspected playlist: "+reference(lib,inspected.id));
            if(lib.playlists.indexOf(inspected)>=count)playlist(report,inspected,lib.playlists.indexOf(inspected)+1,lib,engine,session.inWorld()?context:null,vanilla);
            conditions(report,inspected.conditions,session.inWorld()?context:null,settings,"root",0,new int[1]);
        }
        return report.finish();
    }
    private static void environment(Report r,Context c){
        r.line("Environment: dimension "+resource(c.dimension())+"; biome "+resource(c.biome())+"; gamemode "+c.gameMode());
        r.line("Time: "+c.timeOfDay()+" ("+(c.day()?"day":"night")+"); block Y: "+number(Math.floor(c.y()))+"; covered: "+c.covered()+"; cover depth: "+number(c.coverDepth()));
        r.line("Light: "+c.lightLevel()+"; biome temperature: "+number(c.biomeTemperature())+"; underwater: "+c.underwater());
        r.line("Combat: "+c.combat()+"; PvP: "+c.playerCombat()+"; red hearts: "+number(c.healthHearts())+"; dead: "+c.dead());
        r.line("Weather: raining "+c.raining()+"; thunder "+c.thundering()+"; precipitation "+c.precipitation()
            +"; structure data available: "+c.structures().available());
        r.line("Held items: main "+resource(c.mainHandItem())+"; off-hand "+resource(c.offHandItem()));
    }
    private static void playlist(Report r,Playlist p,int rank,Library lib,PlaybackEngine e,Context c,Set<String> vanilla){
        r.line("#"+rank+": automatic="+p.enabled+"; manualOnly="+p.manualOnly+"; conditions="+(c==null?"not evaluated":p.conditions.failure(c,lib.settings))
            +"; playable="+(c==null?"not evaluated":lib.hasPlayable(p,vanilla,e.failedTracks()))+"; explicit assignments="+p.tracks.size()+"; vanilla="+p.includeVanilla
            +"; volume="+percent(p.volume)+"; finish entry/exit="+p.finishCurrentSong+"/"+p.finishCurrentSongOnExit+"; play on trigger="+p.playOnTrigger+"; silence="+(p.customSilence?number(p.silenceMinSeconds)+"–"+number(p.silenceMaxSeconds)+"s":"default"));
    }
    private static void conditions(Report r,PlaylistConditions p,Context c,Settings settings,String group,int depth,int[] nodes){
        if(depth>PlaylistConditions.MAX_DEPTH||++nodes[0]>PlaylistConditions.MAX_NODES){r.line("Further condition groups omitted.");return;}
        r.line("  "+group+": enabled="+p.enabled+"; match="+(p.matchAny?"ANY":"ALL")+"; negated="+p.negated+"; result="+(c==null?"not evaluated":p.failure(c,settings)));
        if(p.areaEnabled)r.line("    Location: configured="+p.area.configured+"; matches="+(c==null?"not evaluated":p.area.contains(c))+" (location details omitted)");
        if(p.dimensionEnabled)r.line("    Dimension: "+resource(p.dimension));
        if(p.biomeEnabled)r.line("    Biome: "+p.biomes.size()+" selected; matches="+(c==null?"not evaluated":p.biomes.contains(c.biome())));
        if(p.structureEnabled)r.line("    Structure: "+p.structures.size()+" selected; radius="+number(p.structureRadius)+"; data available="+(c==null?"not evaluated":c.structures().available()));
        if(p.timeEnabled)r.line("    Time: "+p.startTime+"–"+p.endTime+"; fit songs="+p.fitSongsToTimeWindow);
        if(p.elevationEnabled)r.line("    Elevation: "+p.minY+"–"+p.maxY);
        if(p.temperatureEnabled)r.line("    Biome temperature: "+number(p.minTemperature)+"–"+number(p.maxTemperature));
        if(p.lightEnabled)r.line("    Light: "+p.minLight+"–"+p.maxLight);
        if(p.cover!=PlaylistConditions.State.ANY)r.line("    Cover: "+p.cover+"; minimum depth="+number(p.minCoverDepth));
        if(p.combat!=PlaylistConditions.State.ANY)r.line("    Combat: "+p.combat+"; players only="+p.combatPlayersOnly);
        if(p.water!=PlaylistConditions.State.ANY)r.line("    Underwater: "+p.water);
        if(p.healthEnabled)r.line("    Health: "+p.healthComparison+" "+number(p.healthThreshold)+" red hearts");
        if(p.weatherEnabled)r.line("    Weather: "+p.weather);
        if(p.heldItemEnabled)r.line("    Held items: "+p.heldItems.size()+" selected; hand="+p.heldItemHand);
        if(p.gameModeEnabled)r.line("    Gamemodes: "+p.gameModes);
        for(int i=0;i<p.groups.size()&&nodes[0]<PlaylistConditions.MAX_NODES;i++)conditions(r,p.groups.get(i),c,settings,group+"."+(i+1),depth+1,nodes);
    }
    private static String reference(Library lib,String id){
        if(id==null)return "none";
        for(int i=0;i<lib.playlists.size();i++)if(lib.playlists.get(i).id.equals(id))return "#"+(i+1);
        return "unavailable";
    }
    /** Categories only: matching arbitrary exception prose never copies its content. */
    static String errorCategory(String message){
        if(message==null||message.isBlank())return "none";
        String lower=message.substring(0,Math.min(message.length(),4096)).toLowerCase(Locale.ROOT);
        if(lower.contains("outofmemory")||lower.contains("out of memory")||lower.contains("out of stack space"))return "memory allocation failure (details omitted)";
        if(lower.contains("accessdenied")||lower.contains("access denied")||lower.contains("permission denied"))return "access denied (details omitted)";
        if(lower.contains("filenotfound")||lower.contains("nosuchfile")||lower.contains("no such file")||lower.contains("file not found"))return "file unavailable (details omitted)";
        if(lower.contains("unsupported")||lower.contains("not supported"))return "unsupported audio/input (details omitted)";
        return "audio decoding/playback failure (details omitted)";
    }
    private static String token(String value){return value!=null&&TOKEN.matcher(value).matches()?value:"[omitted]";}
    private static String resource(String value){
        if(value==null||value.isEmpty())return "empty";
        if(!RESOURCE.matcher(value).matches()||value.contains(".."))return "[omitted]";
        int colon=value.indexOf(':');
        if(PRIVATE_NAMESPACES.contains(value.substring(0,colon)))return "[omitted]";
        // host:port also satisfies Minecraft's identifier grammar; do not expose it.
        boolean numeric=true;
        for(int i=colon+1;i<value.length();i++)if(value.charAt(i)<'0'||value.charAt(i)>'9'){numeric=false;break;}
        return numeric?"[omitted]":value;
    }
    private static String number(double value){return Double.isFinite(value)?String.format(Locale.ROOT,"%.2f",value):"unavailable";}
    private static String percent(double value){return number(value*100)+"%";}
    private static long mib(long value){return Math.max(0,value)/(1024*1024);}
    private static final class Report {
        final StringBuilder text=new StringBuilder(4096);
        boolean truncated;
        void line(String line){
            if(text.length()+line.length()+1>MAX_CHARACTERS-64){truncated=true;return;}
            if(!truncated)text.append(line).append('\n');
        }
        String finish(){if(truncated)text.append("[Report truncated to keep clipboard output bounded.]\n");return text.toString();}
    }
}

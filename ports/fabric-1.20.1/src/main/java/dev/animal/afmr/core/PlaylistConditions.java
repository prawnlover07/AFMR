package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.EnumSet;

/** Editable filters and nested groups. Evaluation allocates nothing during playback. */
public final class PlaylistConditions {
    public enum State { ANY, YES, NO }
    public enum HealthComparison { BELOW, ABOVE }
    public enum Hand {
        EITHER("Either hand"),MAIN_HAND("Main hand"),OFF_HAND("Off-hand");
        private final String label;
        Hand(String label){this.label=label;}
        public String label(){return label;}
    }
    public enum Weather {
        ANY_WEATHER("Any weather (rain / snow / storms)"),CLEAR("Clear"),RAIN("Rain"),SNOW("Snow"),THUNDERSTORM("Thunderstorm");
        private final String label;
        Weather(String label){this.label=label;}
        public String label(){return label;}
    }
    public enum Failure {
        NONE, LOCATION, DIMENSION, BIOME, STRUCTURE_DATA, STRUCTURE, TIME, ELEVATION, TEMPERATURE, LIGHT, COVER, COMBAT, WATER, WEATHER, HEALTH, HELD_ITEM, GAME_MODE, NO_CONDITIONS_MATCH, GROUP, NEGATED, DISABLED;
        private final int bit=1<<ordinal();
    }
    private static final Failure[] FAILURES=Failure.values();
    public static final int MAX_DEPTH=4,MAX_GROUPS=8,MAX_NODES=64;
    public boolean enabled=true,negated;
    public String label="";
    public java.util.List<PlaylistConditions> groups=new ArrayList<>();
    public boolean matchAny,weatherEnabled;
    public Weather weather=Weather.ANY_WEATHER;
    public boolean areaEnabled,dimensionEnabled,timeEnabled,elevationEnabled,temperatureEnabled,lightEnabled,biomeEnabled;
    public boolean fitSongsToTimeWindow;
    public Set<String> biomes=new LinkedHashSet<>();
    public boolean heldItemEnabled;
    public Set<String> heldItems=new LinkedHashSet<>();
    public Hand heldItemHand=Hand.EITHER;
    public boolean gameModeEnabled;
    public Set<GameMode> gameModes=EnumSet.of(GameMode.SURVIVAL);
    public boolean structureEnabled;
    public Set<String> structures=new LinkedHashSet<>();
    public double structureRadius;
    public Home area=new Home();
    public String dimension="minecraft:overworld";
    public int startTime=0,endTime=11999,minY=-64,maxY=320,minLight=0,maxLight=4;
    public double minTemperature=-1,maxTemperature=0.15,minCoverDepth=1;
    public State cover=State.ANY,combat=State.ANY,water=State.ANY;
    public boolean combatPlayersOnly,healthEnabled;
    public HealthComparison healthComparison=HealthComparison.BELOW;
    public double healthThreshold=5;

    public boolean matches(Context c,Settings settings){
        return failure(c,settings)==Failure.NONE;
    }
    /** Shared predicate for playback and the inspector; failed comparisons allocate nothing. */
    public Failure failure(Context c,Settings settings){
        return evaluate(c,settings,-1);
    }
    private Failure evaluate(Context c,Settings settings,int projectedTime){
        if(!enabled)return Failure.DISABLED;
        // Track the filters checked and failed without allocating per-filter objects or arrays.
        int checked=0,failed=0,unknown=0;
        if(areaEnabled){checked|=Failure.LOCATION.bit;if(!area.contains(c))failed|=Failure.LOCATION.bit;}
        if(dimensionEnabled){checked|=Failure.DIMENSION.bit;if(!dimension.equals(c.dimension()))failed|=Failure.DIMENSION.bit;}
        if(biomeEnabled){checked|=Failure.BIOME.bit;if(!biomes.contains(c.biome()))failed|=Failure.BIOME.bit;}
        if(structureEnabled){
            checked|=Failure.STRUCTURE.bit;
            if(!c.structures().available())unknown|=Failure.STRUCTURE.bit;
            else if(!c.structures().matches(structures,structureRadius,c.x(),c.y(),c.z()))failed|=Failure.STRUCTURE.bit;
        }
        if(timeEnabled){checked|=Failure.TIME.bit;int time=Math.floorMod(projectedTime>=0&&fitSongsToTimeWindow?projectedTime:c.timeOfDay(),24000);
            if(startTime<=endTime?(time<startTime||time>endTime):(time<startTime&&time>endTime))failed|=Failure.TIME.bit;}
        double y=Math.floor(c.y());
        if(elevationEnabled){checked|=Failure.ELEVATION.bit;if(y<minY||y>maxY)failed|=Failure.ELEVATION.bit;}
        // Minecraft stores biome temperatures as floats; decimal limits must share that precision.
        if(temperatureEnabled){checked|=Failure.TEMPERATURE.bit;
            if(!Double.isFinite(c.biomeTemperature())||c.biomeTemperature()<(float)minTemperature||c.biomeTemperature()>(float)maxTemperature)failed|=Failure.TEMPERATURE.bit;}
        if(lightEnabled){checked|=Failure.LIGHT.bit;if(c.lightLevel()<minLight||c.lightLevel()>maxLight)failed|=Failure.LIGHT.bit;}
        if(cover!=State.ANY){checked|=Failure.COVER.bit;
            if(cover==State.YES&&(!c.covered()||c.coverDepth()<minCoverDepth)||cover==State.NO&&c.covered())failed|=Failure.COVER.bit;}
        boolean inCombat=settings.combatEnabled&&(combatPlayersOnly?c.playerCombat():c.combat());
        if(combat!=State.ANY){checked|=Failure.COMBAT.bit;if(combat==State.YES&&!inCombat||combat==State.NO&&inCombat)failed|=Failure.COMBAT.bit;}
        if(water!=State.ANY){checked|=Failure.WATER.bit;if(water==State.YES&&!c.underwater()||water==State.NO&&c.underwater())failed|=Failure.WATER.bit;}
        if(healthEnabled){
            checked|=Failure.HEALTH.bit;
            double hearts=c.healthHearts();
            if(!Double.isFinite(hearts)||hearts<0||(healthComparison==HealthComparison.BELOW
                ?(float)hearts>=(float)healthThreshold:(float)hearts<=(float)healthThreshold))failed|=Failure.HEALTH.bit;
        }
        if(weatherEnabled){
            checked|=Failure.WEATHER.bit;
            boolean matches=switch(weather){
                case ANY_WEATHER->c.raining()||c.thundering();
                case CLEAR->!c.raining()&&!c.thundering();
                case RAIN->c.raining()&&c.precipitation()==Precipitation.RAIN;
                case SNOW->c.raining()&&c.precipitation()==Precipitation.SNOW;
                case THUNDERSTORM->c.thundering();
            };
            if(!matches)failed|=Failure.WEATHER.bit;
        }
        if(heldItemEnabled){
            checked|=Failure.HELD_ITEM.bit;
            boolean matches=heldItemHand!=Hand.OFF_HAND&&!c.mainHandItem().isEmpty()&&heldItems.contains(c.mainHandItem())
                ||heldItemHand!=Hand.MAIN_HAND&&!c.offHandItem().isEmpty()&&heldItems.contains(c.offHandItem());
            if(!matches)failed|=Failure.HELD_ITEM.bit;
        }
        if(gameModeEnabled){
            checked|=Failure.GAME_MODE.bit;
            if(c.gameMode()==null||!gameModes.contains(c.gameMode()))failed|=Failure.GAME_MODE.bit;
        }
        boolean groupMatch=false,groupFail=false,groupUnknown=false,hasGroup=false;
        for(var group:groups)if(group.enabled){
            hasGroup=true;var result=group.evaluate(c,settings,projectedTime);
            if(result==Failure.NONE)groupMatch=true;
            else if(result==Failure.STRUCTURE_DATA)groupUnknown=true;
            else groupFail=true;
        }
        boolean positive=(checked&~(failed|unknown))!=0||groupMatch;
        boolean negative=failed!=0||groupFail;
        boolean unavailable=unknown!=0||groupUnknown;
        if(unavailable&&(matchAny?!positive:!negative))return Failure.STRUCTURE_DATA;
        boolean matches=matchAny?checked==0&&!hasGroup||positive:!negative;
        if(negated)return matches?Failure.NEGATED:Failure.NONE;
        if(matches)return Failure.NONE;
        if(matchAny)return Failure.NO_CONDITIONS_MATCH;
        if(failed==0)return Failure.GROUP;
        var first=FAILURES[Integer.numberOfTrailingZeros(failed)];
        return first;
    }
    /** Natural 20-tick/second budget until the opted-in time branches stop matching.
     * Other observations stay fixed; All/Any/Not and overlapping windows retain their meaning.
     * At most two boundaries per condition node are visited, only when choosing a song.
     */
    public double songTimeBudget(Context c,Settings settings){
        if(!matches(c,settings))return 0;
        int time=Math.floorMod(c.timeOfDay(),24000),elapsed=0;
        while(elapsed<24000){
            int step=nextFitBoundary(Math.floorMod(time+elapsed,24000));
            if(step>=24000-elapsed)return Double.POSITIVE_INFINITY;
            elapsed+=step;
            if(evaluate(c,settings,(time+elapsed)%24000)!=Failure.NONE)return elapsed/20.0;
        }
        return Double.POSITIVE_INFINITY;
    }
    private int nextFitBoundary(int time){
        if(!enabled)return 24000;
        int next=24000;
        if(timeEnabled&&fitSongsToTimeWindow&&!(startTime==0&&endTime==23999)){
            int start=Math.floorMod(startTime-time,24000),end=Math.floorMod(endTime+1-time,24000);
            if(start>0)next=start;if(end>0)next=Math.min(next,end);
        }
        for(var group:groups)next=Math.min(next,group.nextFitBoundary(time));
        return next;
    }
    public boolean combatDriven(Context c,Settings settings){
        if(!enabled||negated)return false;
        if(combat==State.YES&&(!matchAny||settings.combatEnabled&&(combatPlayersOnly?c.playerCombat():c.combat())))return true;
        for(var group:groups)if(group.enabled&&(!matchAny||group.matches(c,settings))&&group.combatDriven(c,settings))return true;
        return false;
    }
    public int structureSearchRadius(){
        if(!enabled)return -1;
        int radius=structureEnabled?(int)Math.ceil(structureRadius):-1;
        for(var group:groups)radius=Math.max(radius,group.structureSearchRadius());return radius;
    }
    public int nodeCount(){int count=1;for(var group:groups)count+=group.nodeCount();return count;}
    public String summary(){
        var parts=new ArrayList<String>();
        if(areaEnabled)parts.add("location");if(dimensionEnabled)parts.add(dimension);
        if(biomeEnabled)parts.add("biomes: "+biomes.size()+" selected");
        if(structureEnabled)parts.add("structures: "+structures.size()+" selected, radius "+structureRadius);
        if(timeEnabled)parts.add("time "+startTime+"-"+endTime+(fitSongsToTimeWindow?" / fit songs":""));
        if(elevationEnabled)parts.add("Y "+minY+" to "+maxY);
        if(cover!=State.ANY)parts.add(cover==State.YES?"covered":"open sky");
        if(temperatureEnabled)parts.add("temperature "+minTemperature+" to "+maxTemperature);
        if(lightEnabled)parts.add("light "+minLight+"-"+maxLight);
        if(combat!=State.ANY)parts.add(combatPlayersOnly?(combat==State.YES?"PvP":"not in PvP"):(combat==State.YES?"combat":"peaceful"));
        if(healthEnabled)parts.add("health "+(healthComparison==HealthComparison.BELOW?"below ":"above ")+healthThreshold+" hearts");
        if(water!=State.ANY)parts.add(water==State.YES?"underwater":"out of water");
        if(weatherEnabled)parts.add("weather: "+weather.label());
        if(heldItemEnabled)parts.add("held items: "+heldItems.size()+" selected / "+heldItemHand.label());
        if(gameModeEnabled)parts.add("gamemode: "+(gameModes.isEmpty()?"none selected":gameModes.stream().map(GameMode::label).collect(java.util.stream.Collectors.joining(", "))));
        for(var group:groups)if(group.enabled)parts.add((group.negated?"NOT ":"")+(group.label.isBlank()?"group":group.label));
        String result=parts.isEmpty()?"Any environment":(matchAny?"Any of: ":"")+String.join(matchAny?" OR ":" + ",parts);
        return (enabled?"":"Off / ")+(negated?"NOT ("+result+")":result);
    }
    public void normalize(){
        normalize(0,new int[1]);
    }
    private void normalize(int depth,int[] nodes){
        if(depth>MAX_DEPTH||++nodes[0]>MAX_NODES)throw new IllegalArgumentException("Too many nested conditions");
        if(label==null)label="";label=label.replaceAll("\\p{Cc}","").strip();if(label.length()>80)label=label.substring(0,80);
        if(groups==null)groups=new ArrayList<>();
        if(groups.size()>MAX_GROUPS)throw new IllegalArgumentException("Too many condition groups");
        for(var group:groups){if(group==null)throw new IllegalArgumentException("Null condition group");group.normalize(depth+1,nodes);}
        if(area==null)area=new Home();
        if(area.world==null)area.world="";if(area.dimension==null)area.dimension="minecraft:overworld";
        if(dimension==null)dimension="minecraft:overworld";
        if(weather==null)weather=Weather.ANY_WEATHER;
        if(heldItemHand==null)heldItemHand=Hand.EITHER;
        var modes=EnumSet.noneOf(GameMode.class);
        if(gameModes!=null)for(var mode:gameModes)if(mode!=null)modes.add(mode);
        gameModes=modes;
        if(heldItems==null)heldItems=new LinkedHashSet<>();
        else{var clean=new LinkedHashSet<String>();for(String id:heldItems)if(id!=null&&!id.isBlank())clean.add(id.strip());heldItems=clean;}
        if(healthComparison==null)healthComparison=HealthComparison.BELOW;
        if(!Double.isFinite(healthThreshold)||healthThreshold<0)healthThreshold=5;
        if(biomes==null)biomes=new LinkedHashSet<>();
        else{var clean=new LinkedHashSet<String>();for(String id:biomes)if(id!=null&&!id.isBlank())clean.add(id.strip());biomes=clean;}
        if(structures==null)structures=new LinkedHashSet<>();
        else{var clean=new LinkedHashSet<String>();for(String id:structures)if(id!=null&&!id.isBlank())clean.add(id.strip());structures=clean;}
        if(!Double.isFinite(structureRadius))structureRadius=0;
        structureRadius=Math.max(0,Math.min(structureRadius,StructureContext.MAX_RADIUS));
        if(cover==null)cover=State.ANY;if(combat==null)combat=State.ANY;if(water==null)water=State.ANY;
        startTime=Math.max(0,Math.min(startTime,23999));endTime=Math.max(0,Math.min(endTime,23999));
        if(minY>maxY){int swap=minY;minY=maxY;maxY=swap;}
        minLight=Math.max(0,Math.min(minLight,15));maxLight=Math.max(minLight,Math.min(maxLight,15));
        if(!Double.isFinite(minTemperature))minTemperature=-1;if(!Double.isFinite(maxTemperature))maxTemperature=2;
        if(minTemperature>maxTemperature){double swap=minTemperature;minTemperature=maxTemperature;maxTemperature=swap;}
        if(!Double.isFinite(minCoverDepth))minCoverDepth=1;minCoverDepth=Math.max(0,Math.min(minCoverDepth,4096));
        if(!Double.isFinite(area.radius))area.radius=64;area.radius=Math.max(1,Math.min(area.radius,100000));
    }
    public PlaylistConditions copy(){
        var c=new PlaylistConditions();c.areaEnabled=areaEnabled;c.dimensionEnabled=dimensionEnabled;c.timeEnabled=timeEnabled;
        c.fitSongsToTimeWindow=fitSongsToTimeWindow;
        c.enabled=enabled;c.negated=negated;c.label=label;
        for(var group:groups)c.groups.add(group.copy());
        c.matchAny=matchAny;c.weatherEnabled=weatherEnabled;c.weather=weather;
        c.elevationEnabled=elevationEnabled;c.temperatureEnabled=temperatureEnabled;c.lightEnabled=lightEnabled;
        c.biomeEnabled=biomeEnabled;c.biomes=new LinkedHashSet<>(biomes);
        c.heldItemEnabled=heldItemEnabled;c.heldItems=new LinkedHashSet<>(heldItems);c.heldItemHand=heldItemHand;
        c.gameModeEnabled=gameModeEnabled;c.gameModes=EnumSet.noneOf(GameMode.class);c.gameModes.addAll(gameModes);
        c.structureEnabled=structureEnabled;c.structures=new LinkedHashSet<>(structures);c.structureRadius=structureRadius;
        c.dimension=dimension;c.startTime=startTime;c.endTime=endTime;c.minY=minY;c.maxY=maxY;
        c.minLight=minLight;c.maxLight=maxLight;c.minTemperature=minTemperature;c.maxTemperature=maxTemperature;
        c.minCoverDepth=minCoverDepth;c.cover=cover;c.combat=combat;c.water=water;
        c.combatPlayersOnly=combatPlayersOnly;c.healthEnabled=healthEnabled;c.healthComparison=healthComparison;c.healthThreshold=healthThreshold;
        c.area.configured=area.configured;c.area.world=area.world;c.area.dimension=area.dimension;
        c.area.x=area.x;c.area.y=area.y;c.area.z=area.z;c.area.radius=area.radius;
        return c;
    }
}

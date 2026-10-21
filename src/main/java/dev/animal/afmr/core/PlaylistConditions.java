package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/** Optional filters for a custom playlist. Every enabled filter must match. */
public final class PlaylistConditions {
    public enum State { ANY, YES, NO }
    public enum Failure { NONE, LOCATION, DIMENSION, BIOME, TIME, ELEVATION, TEMPERATURE, LIGHT, COVER, COMBAT, WATER }
    public boolean areaEnabled,dimensionEnabled,timeEnabled,elevationEnabled,temperatureEnabled,lightEnabled,biomeEnabled;
    public Set<String> biomes=new LinkedHashSet<>();
    public Home area=new Home();
    public String dimension="minecraft:overworld";
    public int startTime=0,endTime=11999,minY=-64,maxY=320,minLight=0,maxLight=4;
    public double minTemperature=-1,maxTemperature=0.15,minCoverDepth=1;
    public State cover=State.ANY,combat=State.ANY,water=State.ANY;

    public boolean matches(Context c,Settings settings){
        return failure(c,settings)==Failure.NONE;
    }
    /** Shared predicate for playback and the inspector; failed comparisons allocate nothing. */
    public Failure failure(Context c,Settings settings){
        if(areaEnabled&&!area.contains(c))return Failure.LOCATION;
        if(dimensionEnabled&&!dimension.equals(c.dimension()))return Failure.DIMENSION;
        if(biomeEnabled&&!biomes.contains(c.biome()))return Failure.BIOME;
        if(timeEnabled){int time=Math.floorMod(c.timeOfDay(),24000);
            if(startTime<=endTime?(time<startTime||time>endTime):(time<startTime&&time>endTime))return Failure.TIME;}
        double y=Math.floor(c.y());
        if(elevationEnabled&&(y<minY||y>maxY))return Failure.ELEVATION;
        // Minecraft stores biome temperatures as floats; decimal limits must share that precision.
        if(temperatureEnabled&&(!Double.isFinite(c.biomeTemperature())||c.biomeTemperature()<(float)minTemperature||c.biomeTemperature()>(float)maxTemperature))return Failure.TEMPERATURE;
        if(lightEnabled&&(c.lightLevel()<minLight||c.lightLevel()>maxLight))return Failure.LIGHT;
        if(cover==State.YES&&(!c.covered()||c.coverDepth()<minCoverDepth))return Failure.COVER;
        if(cover==State.NO&&c.covered())return Failure.COVER;
        boolean inCombat=settings.combatEnabled&&c.combat();
        if(combat==State.YES&&!inCombat||combat==State.NO&&inCombat)return Failure.COMBAT;
        if(water==State.YES&&!c.underwater()||water==State.NO&&c.underwater())return Failure.WATER;
        return Failure.NONE;
    }
    public String summary(){
        var parts=new ArrayList<String>();
        if(areaEnabled)parts.add("location");if(dimensionEnabled)parts.add(dimension);
        if(biomeEnabled)parts.add("biomes: "+biomes.size()+" selected");
        if(timeEnabled)parts.add("time "+startTime+"-"+endTime);
        if(elevationEnabled)parts.add("Y "+minY+" to "+maxY);
        if(cover!=State.ANY)parts.add(cover==State.YES?"covered":"open sky");
        if(temperatureEnabled)parts.add("temperature "+minTemperature+" to "+maxTemperature);
        if(lightEnabled)parts.add("light "+minLight+"-"+maxLight);
        if(combat!=State.ANY)parts.add(combat==State.YES?"combat":"peaceful");
        if(water!=State.ANY)parts.add(water==State.YES?"underwater":"out of water");
        return parts.isEmpty()?"Any environment":String.join(" + ",parts);
    }
    public void normalize(){
        if(area==null)area=new Home();
        if(area.world==null)area.world="";if(area.dimension==null)area.dimension="minecraft:overworld";
        if(dimension==null)dimension="minecraft:overworld";
        if(biomes==null)biomes=new LinkedHashSet<>();
        else{var clean=new LinkedHashSet<String>();for(String id:biomes)if(id!=null&&!id.isBlank())clean.add(id.strip());biomes=clean;}
        if(cover==null)cover=State.ANY;if(combat==null)combat=State.ANY;if(water==null)water=State.ANY;
        startTime=Math.clamp(startTime,0,23999);endTime=Math.clamp(endTime,0,23999);
        if(minY>maxY){int swap=minY;minY=maxY;maxY=swap;}
        minLight=Math.clamp(minLight,0,15);maxLight=Math.clamp(maxLight,minLight,15);
        if(!Double.isFinite(minTemperature))minTemperature=-1;if(!Double.isFinite(maxTemperature))maxTemperature=2;
        if(minTemperature>maxTemperature){double swap=minTemperature;minTemperature=maxTemperature;maxTemperature=swap;}
        if(!Double.isFinite(minCoverDepth))minCoverDepth=1;minCoverDepth=Math.clamp(minCoverDepth,1,4096);
        if(!Double.isFinite(area.radius))area.radius=64;area.radius=Math.clamp(area.radius,1,100000);
    }
    public PlaylistConditions copy(){
        var c=new PlaylistConditions();c.areaEnabled=areaEnabled;c.dimensionEnabled=dimensionEnabled;c.timeEnabled=timeEnabled;
        c.elevationEnabled=elevationEnabled;c.temperatureEnabled=temperatureEnabled;c.lightEnabled=lightEnabled;
        c.biomeEnabled=biomeEnabled;c.biomes=new LinkedHashSet<>(biomes);
        c.dimension=dimension;c.startTime=startTime;c.endTime=endTime;c.minY=minY;c.maxY=maxY;
        c.minLight=minLight;c.maxLight=maxLight;c.minTemperature=minTemperature;c.maxTemperature=maxTemperature;
        c.minCoverDepth=minCoverDepth;c.cover=cover;c.combat=combat;c.water=water;
        c.area.configured=area.configured;c.area.world=area.world;c.area.dimension=area.dimension;
        c.area.x=area.x;c.area.y=area.y;c.area.z=area.z;c.area.radius=area.radius;
        return c;
    }
}

package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.PlaylistConditions.State;
import java.util.List;

/** Initial data only. Scheduling never distinguishes templates from other playlists. */
public final class PlaylistTemplates {
    private PlaylistTemplates(){}
    public static List<Playlist> create(){
        var combat=new Playlist("combat","Combat");combat.conditions.combat=State.YES;
        var home=new Playlist("home","Home");home.conditions.areaEnabled=true;
        var dark=new Playlist("dark_exploration","Dark exploration");dark.includeVanilla=false;
        var lowLight=new PlaylistConditions();lowLight.label="Covered darkness";lowLight.cover=State.YES;lowLight.minCoverDepth=0;lowLight.lightEnabled=true;
        lowLight.groups.add(dimension("minecraft:the_nether",true));
        lowLight.groups.add(dimension("minecraft:the_end",true));
        dark.conditions.matchAny=true;dark.conditions.groups.add(lowLight);
        var nether=dimension("minecraft:the_nether",false);nether.enabled=false;
        var end=dimension("minecraft:the_end",false);end.enabled=false;
        dark.conditions.groups.add(nether);dark.conditions.groups.add(end);
        var underground=new Playlist("underground","Underground");underground.conditions=underground();
        var night=outside("night_outside","Night outside",12000,23999);
        var day=outside("day_outside","Day outside",0,11999);
        return List.of(combat,home,dark,underground,night,day);
    }
    private static PlaylistConditions dimension(String id,boolean not){
        var c=new PlaylistConditions();c.label=id.equals("minecraft:the_nether")?"Nether":"End";c.dimensionEnabled=true;c.dimension=id;c.negated=not;return c;
    }
    private static PlaylistConditions underground(){
        var c=new PlaylistConditions();c.label="Underground";c.elevationEnabled=true;c.minY=Integer.MIN_VALUE;c.maxY=64;
        c.cover=State.YES;c.minCoverDepth=5;return c;
    }
    private static Playlist outside(String id,String name,int start,int end){
        var p=new Playlist(id,name);var c=p.conditions;c.timeEnabled=true;c.startTime=start;c.endTime=end;
        var below=underground();below.negated=true;c.groups.add(below);return p;
    }
}

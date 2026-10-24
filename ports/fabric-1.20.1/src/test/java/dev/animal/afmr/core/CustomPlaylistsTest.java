package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.State;
import dev.animal.afmr.library.LibraryStore;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomPlaylistsTest {
    @TempDir Path directory;
    private final Settings settings=new Settings();
    private Context context(String dimension,int time,double y,boolean covered,double depth,double temperature){
        return new Context(dimension,"minecraft:plains",false,false,time,covered,3,"world",0,y,0,depth,temperature,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false);
    }
    private Context outside(){return context("minecraft:overworld",18000,80,false,0,0.8);}
    @Test void unsetConditionsAllowAnyEnvironment(){assertTrue(new PlaylistConditions().matches(outside(),settings));}
    @Test void everyEnabledFilterMustMatch(){
        var c=new PlaylistConditions();c.dimensionEnabled=true;c.dimension="minecraft:overworld";c.timeEnabled=true;c.startTime=12000;c.endTime=23999;
        c.elevationEnabled=true;c.minY=80;c.maxY=120;c.temperatureEnabled=true;c.minTemperature=-1;c.maxTemperature=0.15;c.cover=State.NO;
        assertTrue(c.matches(context(c.dimension,18000,80,false,0,0),settings));
        assertFalse(c.matches(context("minecraft:the_end",18000,80,false,0,0),settings));
        assertFalse(c.matches(context(c.dimension,6000,80,false,0,0),settings));
        assertFalse(c.matches(context(c.dimension,18000,79,false,0,0),settings));
        assertFalse(c.matches(context(c.dimension,18000,80,false,0,0.8),settings));
        assertFalse(c.matches(context(c.dimension,18000,80,true,20,0),settings));
    }
    @Test void timeWindowsWrapMidnightAndIncludeBothBoundaries(){
        var c=new PlaylistConditions();c.timeEnabled=true;c.startTime=18000;c.endTime=6000;
        for(int tick:List.of(18000,23999,0,6000))assertTrue(c.matches(context("d",tick,0,false,0,0),settings));
        for(int tick:List.of(6001,12000,17999))assertFalse(c.matches(context("d",tick,0,false,0,0),settings));
        c.startTime=c.endTime=42;assertTrue(c.matches(context("d",42,0,false,0,0),settings));assertFalse(c.matches(context("d",43,0,false,0,0),settings));
    }
    @Test void dayNightUsesTheSameClockAsCustomTime(){
        assertTrue(context("d",11999,0,false,0,0).day());assertFalse(context("d",12000,0,false,0,0).day());
        assertTrue(context("d",24000,0,false,0,0).day());
    }
    @Test void elevationUsesInclusiveBlockYIncludingNegativeFloors(){
        var c=new PlaylistConditions();c.elevationEnabled=true;c.minY=-20;c.maxY=-10;
        assertTrue(c.matches(context("d",0,-20,false,0,0),settings));assertTrue(c.matches(context("d",0,-9.1,false,0,0),settings));
        assertFalse(c.matches(context("d",0,-20.1,false,0,0),settings));assertFalse(c.matches(context("d",0,-9,false,0,0),settings));
    }
    @Test void temperatureMatchesFloatBoundariesAndRejectsInvalidReadings(){
        var c=new PlaylistConditions();c.temperatureEnabled=true;c.minTemperature=-0.5;c.maxTemperature=0.15;
        assertTrue(c.matches(context("d",0,200,false,0,(double)0.15f),settings));
        assertTrue(c.matches(context("d",0,0,false,0,-0.5),settings));
        assertFalse(c.matches(context("d",0,0,false,0,Math.nextUp(0.15f)),settings));
        assertFalse(c.matches(context("d",0,0,false,0,Double.NaN),settings));
    }
    @Test void locationIsSphericalAndWorldAndDimensionScoped(){
        var c=new PlaylistConditions();c.areaEnabled=true;var h=c.area;h.configured=true;h.world="world";h.y=80;h.radius=10;
        assertTrue(c.matches(outside(),settings));assertTrue(c.matches(context(h.dimension,0,90,false,0,0),settings));
        assertFalse(c.matches(context(h.dimension,0,90.1,false,0,0),settings));
        assertFalse(c.matches(context("minecraft:the_end",0,80,false,0,0),settings));
        h.world="another-world";assertFalse(c.matches(outside(),settings));h.world="world";h.configured=false;assertFalse(c.matches(outside(),settings));
    }
    @Test void coverRequiresSolidCoverAndItsOwnDepthIndependentlyOfUndergroundY(){
        var c=new PlaylistConditions();c.cover=State.YES;c.minCoverDepth=5;
        assertTrue(c.matches(context("d",0,200,true,5,0),settings));
        assertFalse(c.matches(context("d",0,200,true,4.9,0),settings));assertFalse(c.matches(context("d",0,-50,false,100,0),settings));
        c.cover=State.NO;assertTrue(c.matches(outside(),settings));assertFalse(c.matches(context("d",0,200,true,5,0),settings));
    }
    @Test void waterLightAndCombatFiltersComposeAndRespectGlobalCombatSwitch(){
        var c=new PlaylistConditions();c.combat=State.YES;c.water=State.YES;c.lightEnabled=true;c.minLight=0;c.maxLight=4;
        var busy=new Context("d","b",true,true,0,true,4,"world",0,0,0,8,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false);
        assertTrue(c.matches(busy,settings));c.maxLight=3;assertFalse(c.matches(busy,settings));c.maxLight=4;
        settings.combatEnabled=false;assertFalse(c.matches(busy,settings));c.combat=State.NO;assertTrue(c.matches(busy,settings));
        c.water=State.NO;assertFalse(c.matches(busy,settings));
    }
    @Test void creationPreservesDefaultsAndStartsAboveBroadAmbientFallbacks(){
        Library lib=Library.defaults();var p=lib.createPlaylist("  Snowy nights  ");
        assertEquals("Snowy nights",p.name);assertEquals("Any environment",p.conditions.summary());assertFalse(p.includeVanilla);assertTrue(p.tracks.isEmpty());
        assertTrue(lib.playlists.indexOf(p)>lib.playlists.indexOf(lib.playlist("underground")));
        assertTrue(lib.playlists.indexOf(p)<lib.playlists.indexOf(lib.playlist("night_outside")));
        assertThrows(IllegalArgumentException.class,()->lib.createPlaylist("   "));
        assertThrows(IllegalArgumentException.class,()->lib.createPlaylist("x".repeat(81)));
        assertEquals(7,lib.playlists.size());
    }
    @Test void rankingChangesPlaybackAndDisabledOrEmptyCustomPlaylistsFallThrough(){
        Library lib=Library.defaults();var p=lib.createPlaylist("Custom");Track a=new Track("a","file","A",false),b=new Track("b","file","B",false);lib.tracks.addAll(List.of(a,b));
        lib.playlist("night_outside").tracks.put(a.id,true);assertEquals("night_outside",lib.selectPlayable(outside(),Set.of(),Set.of()).id);
        p.tracks.put(b.id,true);assertSame(p,lib.selectPlayable(outside(),Set.of(),Set.of()));
        p.tracks.put(b.id,false);assertEquals("night_outside",lib.selectPlayable(outside(),Set.of(),Set.of()).id);p.tracks.put(b.id,true);
        lib.movePlaylist(p.id,999);assertEquals("night_outside",lib.selectPlayable(outside(),Set.of(),Set.of()).id);
        lib.movePlaylist(p.id,-100);assertSame(p,lib.playlists.getFirst());p.enabled=false;assertEquals("night_outside",lib.selectPlayable(outside(),Set.of(),Set.of()).id);
    }
    @Test void deletingCustomPlaylistKeepsMusicDefaultsAndOtherAssignments(){
        Library lib=Library.defaults();var p=lib.createPlaylist("Delete me");var t=new Track("a","file","A",false);lib.tracks.add(t);p.tracks.put(t.id,true);
        lib.playlist("home").tracks.put(t.id,false);lib.folders.add(new Folder("folder",p.id));
        assertFalse(lib.removePlaylist("missing"));assertTrue(lib.removePlaylist(p.id));
        assertEquals(6,lib.playlists.size());assertSame(t,lib.tracks.getFirst());assertEquals(Boolean.FALSE,lib.playlist("home").tracks.get(t.id));
        assertEquals("library",lib.folders.getFirst().playlistId);assertFalse(lib.movePlaylist("missing",0));
    }
    @Test void saveSnapshotOwnsConditionsAreasSettingsAndPriority() throws Exception {
        Library lib=Library.defaults();var p=lib.createPlaylist("Cold home");p.conditions.areaEnabled=true;p.conditions.area.configured=true;p.conditions.area.world="world";
        p.conditions.temperatureEnabled=true;p.conditions.maxTemperature=0.15;p.conditions.area.radius=20;p.tracks.put("a",false);
        lib.settings.combatDeathMusic=DeathMusic.CUT;lib.settings.combatOnAttack=false;lib.settings.combatDeathFadeSeconds=0.5;lib.movePlaylist(p.id,0);
        var snapshot=lib.snapshot();p.conditions.area.radius=99;p.conditions.maxTemperature=2;lib.settings.combatOnAttack=true;
        var store=new LibraryStore(directory.resolve("library.json"));store.save(snapshot);var read=store.load();
        assertEquals(p.id,read.playlists.getFirst().id);var q=read.playlist(p.id);assertEquals(20,q.conditions.area.radius);assertEquals(0.15,q.conditions.maxTemperature);
        assertEquals(Boolean.FALSE,q.tracks.get("a"));assertEquals(DeathMusic.CUT,read.settings.combatDeathMusic);assertFalse(read.settings.combatOnAttack);assertEquals(0.5,read.settings.combatDeathFadeSeconds);
    }
    @Test void existingConfigurationRetainsDefaultsAndGainsNewSettingDefaults() throws Exception {
        Library lib=Library.defaults();lib.playlist("home").tracks.put("a",false);lib.movePlaylist("home",0);
        var json=new Gson().toJsonTree(lib).getAsJsonObject();for(var p:json.getAsJsonArray("playlists"))p.getAsJsonObject().remove("conditions");
        var settings=json.getAsJsonObject("settings");for(String key:List.of("combatDeathMusic","combatDeathFadeSeconds","combatOnAttack","combatOnDamage","preventMusicWhileDead"))settings.remove(key);
        Path file=directory.resolve("library.json");Files.writeString(file,json.toString());var read=new LibraryStore(file).load();
        assertEquals(6,read.playlists.size());assertEquals("home",read.playlists.getFirst().id);assertEquals(Boolean.FALSE,read.playlist("home").tracks.get("a"));
        assertTrue(read.settings.combatOnAttack);assertTrue(read.settings.combatOnDamage);assertTrue(read.settings.preventMusicWhileDead);assertEquals(DeathMusic.FADE,read.settings.combatDeathMusic);assertEquals(1,read.settings.combatDeathFadeSeconds);
    }
    @Test void normalizationKeepsInvalidFilterNumbersOutOfPlayback(){
        var c=new PlaylistConditions();c.minY=100;c.maxY=-100;c.startTime=-1;c.endTime=25000;c.minTemperature=Double.NaN;c.maxTemperature=Double.POSITIVE_INFINITY;
        c.minLight=-20;c.maxLight=30;c.minCoverDepth=Double.NaN;c.cover=null;c.area.radius=Double.NaN;c.normalize();
        assertEquals(-100,c.minY);assertEquals(100,c.maxY);assertEquals(0,c.startTime);assertEquals(23999,c.endTime);
        assertTrue(Double.isFinite(c.minTemperature));assertTrue(Double.isFinite(c.maxTemperature));assertEquals(0,c.minLight);assertEquals(15,c.maxLight);assertEquals(1,c.minCoverDepth);assertEquals(State.ANY,c.cover);assertEquals(64,c.area.radius);
    }
    @Test void hundredsOfCustomRulesStillUseTheTrackIndex(){
        Library lib=Library.defaults();var tracks=new LibraryIndexTest.GuardedTracks();lib.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("t"+i,"file","Song",false));
        for(int i=0;i<200;i++){var p=lib.createPlaylist("Custom "+i);p.conditions.dimensionEnabled=true;p.conditions.dimension="other:"+i;p.tracks.put("t69999",true);}
        var target=lib.createPlaylist("Matching");target.tracks.put("t69999",true);lib.rebuildTrackIndex();tracks.locked=true;
        for(int i=0;i<100;i++)assertSame(target,lib.selectPlayable(outside(),Set.of(),Set.of()));
    }
}

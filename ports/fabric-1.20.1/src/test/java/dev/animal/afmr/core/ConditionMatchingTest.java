package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class ConditionMatchingTest {
    private final Settings settings=new Settings();
    private final StructureContext structures=new StructureContext(true,List.of(new StructureContext.Bounds("test:village",-10,60,-10,10,80,10)));
    private Context context(StructureContext observation){
        return new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"world",0,70,0,0,0.8,false,observation,false,false,Precipitation.NONE,10,false,"minecraft:fishing_rod","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY);
    }
    private record Case(String name,Consumer<PlaylistConditions> matching,Consumer<PlaylistConditions> failing) {}
    @Test void everyFilterCanIndependentlySatisfyAnyModeAndFailAllMode(){
        var cases=List.of(
            new Case("location",c->{c.areaEnabled=true;c.area.configured=true;c.area.world="world";c.area.y=70;},c->c.area.y=1000),
            new Case("dimension",c->{c.dimensionEnabled=true;},c->c.dimension="test:other"),
            new Case("biome",c->{c.biomeEnabled=true;c.biomes.add("minecraft:plains");},c->c.biomes.clear()),
            new Case("structure",c->{c.structureEnabled=true;c.structures.add("test:village");},c->c.structures.clear()),
            new Case("time",c->{c.timeEnabled=true;},c->{c.startTime=12000;c.endTime=23999;}),
            new Case("elevation",c->{c.elevationEnabled=true;},c->c.maxY=0),
            new Case("temperature",c->{c.temperatureEnabled=true;c.minTemperature=0;c.maxTemperature=1;},c->c.maxTemperature=0),
            new Case("light",c->{c.lightEnabled=true;c.maxLight=15;},c->c.maxLight=0),
            new Case("cover",c->c.cover=State.NO,c->c.cover=State.YES),
            new Case("combat",c->c.combat=State.NO,c->c.combat=State.YES),
            new Case("health",c->{c.healthEnabled=true;c.healthComparison=HealthComparison.ABOVE;c.healthThreshold=5;},c->c.healthThreshold=15),
            new Case("water",c->c.water=State.NO,c->c.water=State.YES),
            new Case("held item",c->{c.heldItemEnabled=true;c.heldItems.add("minecraft:fishing_rod");},c->c.heldItems.clear()),
            new Case("weather",c->{c.weatherEnabled=true;c.weather=Weather.CLEAR;},c->c.weather=Weather.RAIN));
        for(var test:cases){
            var c=new PlaylistConditions();test.matching().accept(c);
            assertTrue(c.matches(context(structures),settings),test.name()+" alone");
            // Add one unrelated failing filter, leaving just the tested filter able to match.
            if(test.name().equals("biome")){c.dimensionEnabled=true;c.dimension="test:other";}
            else{c.biomeEnabled=true;c.biomes.clear();}
            assertFalse(c.matches(context(structures),settings),test.name()+" all");
            c.matchAny=true;assertTrue(c.matches(context(structures),settings),test.name()+" any");
            test.failing().accept(c);
            assertEquals(Failure.NO_CONDITIONS_MATCH,c.failure(context(structures),settings),test.name()+" none");
        }
    }
    @Test void noEnabledConditionsMatchEverywhereInEitherMode(){
        var c=new PlaylistConditions();assertTrue(c.matches(context(StructureContext.UNAVAILABLE),settings));
        c.matchAny=true;assertTrue(c.matches(context(StructureContext.UNAVAILABLE),settings));assertEquals("Any environment",c.summary());
    }
    @Test void disabledFiltersDoNotCountAsMatches(){
        var c=new PlaylistConditions();c.matchAny=true;c.weatherEnabled=true;c.weather=Weather.RAIN;
        assertFalse(c.matches(context(structures),settings));
        c.weatherEnabled=false;assertTrue(c.matches(context(structures),settings));
    }
    @Test void unavailableStructureCannotBlockAnotherMatchingFilterInAnyMode(){
        var c=new PlaylistConditions();c.matchAny=true;c.structureEnabled=true;c.structures.add("test:village");
        c.weatherEnabled=true;c.weather=Weather.CLEAR;
        assertTrue(c.matches(context(StructureContext.UNAVAILABLE),settings));
        c.matchAny=false;assertEquals(Failure.STRUCTURE_DATA,c.failure(context(StructureContext.UNAVAILABLE),settings));
        c.matchAny=true;c.weather=Weather.SNOW;
        assertEquals(Failure.STRUCTURE_DATA,c.failure(context(StructureContext.UNAVAILABLE),settings));
    }
    @Test void globalCombatSwitchStillAppliesInAnyMode(){
        var c=new PlaylistConditions();c.matchAny=true;c.combat=State.YES;
        var busy=new Context("minecraft:overworld","minecraft:plains",true,false,6000,false,15,"world",0,70,0,0,0.8,false,structures,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY);
        assertTrue(c.matches(busy,settings));settings.combatEnabled=false;assertFalse(c.matches(busy,settings));
        c.combat=State.NO;assertTrue(c.matches(busy,settings));
    }
    @Test void summaryAndInspectorDescribeTheSelectedMode(){
        var lib=Library.defaults();var p=lib.createPlaylist("Snow or desert");var c=p.conditions;
        c.matchAny=true;c.weatherEnabled=true;c.weather=Weather.SNOW;c.biomeEnabled=true;c.biomes.add("minecraft:desert");
        assertTrue(c.summary().startsWith("Any of: "));assertTrue(c.summary().contains(" OR "));
        var rows=PlaybackInspector.inspect(lib,context(structures),Set.of(),new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1)));
        var row=rows.stream().filter(e->e.id().equals(p.id)).findFirst().orElseThrow();
        assertEquals("None of the enabled conditions match",row.status());assertTrue(row.details().contains("Condition matching: any enabled filter"));
    }
    @Test void anyModeStillRespectsPriorityEmptyPlaylistsAndManualOnly(){
        var lib=Library.defaults();var p=lib.createPlaylist("Clear or desert");p.conditions.matchAny=true;
        p.conditions.weatherEnabled=true;p.conditions.weather=Weather.CLEAR;p.conditions.biomeEnabled=true;p.conditions.biomes.add("minecraft:desert");
        lib.tracks.add(new Track("song","file","Song",false));lib.playlist("day_outside").tracks.put("song",true);
        assertEquals("day_outside",lib.selectPlayable(context(structures),Set.of(),Set.of()).id);
        p.tracks.put("song",true);assertSame(p,lib.selectPlayable(context(structures),Set.of(),Set.of()));
        p.manualOnly=true;assertEquals("day_outside",lib.selectPlayable(context(structures),Set.of(),Set.of()).id);
        p.manualOnly=false;p.enabled=false;assertEquals("day_outside",lib.selectPlayable(context(structures),Set.of(),Set.of()).id);
        assertTrue(new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1)).overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED,0,lib,Set.of()));
    }
}

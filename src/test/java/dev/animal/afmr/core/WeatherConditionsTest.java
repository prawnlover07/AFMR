package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.Weather;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WeatherConditionsTest {
    @TempDir Path directory;
    private final Settings settings=new Settings();
    private Context context(boolean raining,boolean thunder,Precipitation type,boolean covered){
        return new Context("minecraft:overworld","minecraft:plains",false,false,6000,covered,15,"world",0,70,0,covered?10:0,0.8,false,
            StructureContext.UNAVAILABLE,raining,thunder,type,10,false,"","",GameMode.SURVIVAL,false);
    }
    private PlaylistConditions condition(Weather weather){var c=new PlaylistConditions();c.weatherEnabled=true;c.weather=weather;return c;}
    @Test void anyWeatherIncludesRainSnowAndThunderButExcludesClear(){
        var c=condition(Weather.ANY_WEATHER);
        assertTrue(c.matches(context(true,false,Precipitation.RAIN,false),settings));
        assertTrue(c.matches(context(true,false,Precipitation.SNOW,false),settings));
        assertTrue(c.matches(context(true,true,Precipitation.RAIN,false),settings));
        assertTrue(c.matches(context(false,true,Precipitation.NONE,false),settings));
        assertFalse(c.matches(context(false,false,Precipitation.NONE,false),settings));
    }
    @Test void rainAndSnowFollowLocalTypeAndIncludeStorms(){
        var rain=condition(Weather.RAIN);var snow=condition(Weather.SNOW);
        for(boolean thunder:List.of(false,true)){
            var wet=context(true,thunder,Precipitation.RAIN,false);var cold=context(true,thunder,Precipitation.SNOW,false);
            assertTrue(rain.matches(wet,settings));assertFalse(snow.matches(wet,settings));
            assertTrue(snow.matches(cold,settings));assertFalse(rain.matches(cold,settings));
        }
        assertFalse(rain.matches(context(false,false,Precipitation.RAIN,false),settings));
        assertFalse(snow.matches(context(false,false,Precipitation.SNOW,false),settings));
    }
    @Test void stormAndClearUseDimensionWeatherRegardlessOfLocalPrecipitation(){
        var storm=condition(Weather.THUNDERSTORM);var clear=condition(Weather.CLEAR);
        for(var type:Precipitation.values()){
            assertTrue(storm.matches(context(true,true,type,false),settings));
            assertFalse(clear.matches(context(true,true,type,false),settings));
            assertFalse(storm.matches(context(true,false,type,false),settings));
            assertFalse(clear.matches(context(true,false,type,false),settings));
        }
        assertTrue(clear.matches(context(false,false,Precipitation.NONE,false),settings));
        assertFalse(clear.matches(context(false,true,Precipitation.NONE,false),settings));
    }
    @Test void dryBiomesStillHaveWorldWeatherButDoNotMatchRainOrSnow(){
        var dry=context(true,false,Precipitation.NONE,false);
        assertTrue(condition(Weather.ANY_WEATHER).matches(dry,settings));
        assertFalse(condition(Weather.CLEAR).matches(dry,settings));
        assertFalse(condition(Weather.RAIN).matches(dry,settings));
        assertFalse(condition(Weather.SNOW).matches(dry,settings));
    }
    @Test void roofsDoNotChangeWeatherAndCoverIsAnIndependentFilter(){
        var c=condition(Weather.RAIN);
        assertTrue(c.matches(context(true,false,Precipitation.RAIN,true),settings));
        c.cover=PlaylistConditions.State.NO;
        assertFalse(c.matches(context(true,false,Precipitation.RAIN,true),settings));
        assertTrue(c.matches(context(true,false,Precipitation.RAIN,false),settings));
    }
    @Test void disabledWeatherRetainsSelectionAndDoesNotAffectOtherFilters(){
        var c=condition(Weather.SNOW);c.weatherEnabled=false;
        assertTrue(c.matches(context(false,false,Precipitation.NONE,false),settings));assertEquals(Weather.SNOW,c.weather);
        c.timeEnabled=true;c.startTime=12000;c.endTime=23999;
        assertFalse(c.matches(context(false,false,Precipitation.NONE,false),settings));
    }
    @Test void weatherChangesFollowOrdinaryPlaylistPriorityAndFallback(){
        var lib=Library.defaults();var p=lib.createPlaylist("Rain");p.conditions=condition(Weather.RAIN);
        lib.tracks.add(new Track("song","file","Song",false));p.tracks.put("song",true);lib.playlist("day_outside").tracks.put("song",true);
        assertSame(p,lib.selectPlayable(context(true,false,Precipitation.RAIN,false),Set.of(),Set.of()));
        assertEquals("day_outside",lib.selectPlayable(context(false,false,Precipitation.NONE,false),Set.of(),Set.of()).id);
        p.tracks.clear();assertEquals("day_outside",lib.selectPlayable(context(true,false,Precipitation.RAIN,false),Set.of(),Set.of()).id);
    }
    @Test void saveSnapshotAndJsonRoundTripRetainWeatherAndMatchingMode() throws Exception {
        var lib=Library.defaults();var p=lib.createPlaylist("Storm or snow");p.conditions=condition(Weather.SNOW);p.conditions.matchAny=true;
        var snapshot=lib.snapshot();p.conditions.weather=Weather.CLEAR;p.conditions.weatherEnabled=false;p.conditions.matchAny=false;
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(snapshot);var restored=store.load().playlist(p.id).conditions;
            assertTrue(restored.weatherEnabled);assertTrue(restored.matchAny);assertEquals(Weather.SNOW,restored.weather);
            assertTrue(restored.matches(context(true,false,Precipitation.SNOW,false),settings));
        }
    }
    @Test void defaultsAndNormalizationKeepWeatherOptional(){
        var c=new PlaylistConditions();assertFalse(c.matchAny);assertFalse(c.weatherEnabled);
        c.weather=null;c.normalize();assertEquals(Weather.ANY_WEATHER,c.weather);assertFalse(c.weatherEnabled);
        assertEquals("Any environment",c.summary());c.weatherEnabled=true;assertTrue(c.summary().contains("Any weather"));
    }
    @Test void contextPreservesWeather(){
        var c=context(true,true,Precipitation.SNOW,true);
        assertTrue(c.raining());assertTrue(c.thundering());assertEquals(Precipitation.SNOW,c.precipitation());
    }
    @Test void inspectorExplainsWeatherAndFailure(){
        var lib=Library.defaults();var p=lib.createPlaylist("Snow");p.conditions=condition(Weather.SNOW);
        var entries=PlaybackInspector.inspect(lib,context(true,false,Precipitation.RAIN,false),Set.of(),new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1)));
        var row=entries.stream().filter(e->e.id().equals(p.id)).findFirst().orElseThrow();
        assertEquals("Condition not met: weather",row.status());
        assertTrue(row.details().stream().anyMatch(s->s.contains("local precipitation: rain")));
    }
}

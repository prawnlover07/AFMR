package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentConditionsTest {
    private final Library library=Library.defaults();
    private Context context(boolean day,boolean underground,boolean covered,int light,double y){
        return new Context("minecraft:overworld","any",false,false,(day?6000:18000),underground,false,covered,light,"world",0,y,0,(covered?8:0),0.8,false);
    }
    @Test void undergroundYBoundaryIsInclusiveAndNegativeValuesWork(){
        var s=library.settings;s.undergroundMaxY=-20;
        assertTrue(EnvironmentConditions.underground(-20,true,10,s));
        assertTrue(EnvironmentConditions.underground(-21,true,10,s));
        assertFalse(EnvironmentConditions.underground(-19,true,10,s));
    }
    @Test void undergroundRequiresBothYAndEnoughSolidCover(){
        var s=library.settings;s.undergroundMaxY=64;s.undergroundDepth=5;
        assertFalse(EnvironmentConditions.underground(20,false,100,s));
        assertFalse(EnvironmentConditions.underground(20,true,4.9,s));
        assertTrue(EnvironmentConditions.underground(64,true,5,s));
        assertFalse(EnvironmentConditions.underground(65,true,50,s));
    }
    @Test void openSkyNightAndDayNeverCountAsDarkExploration(){
        for(boolean day:List.of(true,false)){
            Context c=context(day,false,false,0,64);
            assertFalse(library.playlist("dark_exploration").matches(c,library.settings));
            assertEquals(day?"day_outside":"night_outside",library.select(c).id);
        }
    }
    @Test void darknessUsesInclusiveLightThresholdAndHasNoYLimit(){
        library.settings.darkMaxLight=4;
        assertTrue(library.playlist("dark_exploration").matches(context(true,false,true,4,200),library.settings));
        assertFalse(library.playlist("dark_exploration").matches(context(true,true,true,5,-50),library.settings));
        library.settings.darkMaxLight=0;
        assertTrue(EnvironmentConditions.dark("minecraft:overworld",true,0,library.settings));
        assertFalse(EnvironmentConditions.dark("minecraft:overworld",true,1,library.settings));
    }
    @Test void darkOverridesUndergroundButHomeAndCombatRemainHigher(){
        Context c=context(true,true,true,0,-20);
        assertEquals("dark_exploration",library.select(c).id);
        assertEquals("home",library.select(c.withHome(true)).id);
        Context combat=new Context(c.dimension(),c.biome(),true,false,6000,true,true,true,0,c.world(),0,-20,0,8,0.8,false);
        assertEquals("combat",library.select(combat).id);
        library.playlist("dark_exploration").enabled=false;
        assertEquals("underground",library.select(c).id);
    }
    @Test void emptyDarkPlaylistFallsThroughWithoutTakingVanillaFromUnderground(){
        Track vanilla=new Track("vanilla:a","a","A",true);library.tracks.add(vanilla);
        var c=context(true,true,true,0,-20);
        assertFalse(library.playlist("dark_exploration").includeVanilla);
        assertEquals("underground",library.selectPlayable(c,Set.of(vanilla.id),Set.of()).id);
        Track local=new Track("custom","file","Dark song",false);library.tracks.add(local);
        library.playlist("dark_exploration").tracks.put(local.id,true);
        assertEquals("dark_exploration",library.selectPlayable(c,Set.of(vanilla.id),Set.of()).id);
        library.playlist("dark_exploration").tracks.put(local.id,false);
        assertEquals("underground",library.selectPlayable(c,Set.of(vanilla.id),Set.of()).id);
    }
    @Test void settingsSnapshotAndNormalizationPreserveConditions(){
        library.settings.undergroundMaxY=-30;library.settings.darkMaxLight=2;
        library.settings.darkInNether=true;library.settings.darkInEnd=true;
        var snapshot=library.snapshot();library.settings.undergroundMaxY=300;library.settings.darkMaxLight=15;
        library.settings.darkInNether=false;library.settings.darkInEnd=false;
        assertEquals(-30,snapshot.settings.undergroundMaxY);assertEquals(2,snapshot.settings.darkMaxLight);
        assertTrue(snapshot.settings.darkInNether);assertTrue(snapshot.settings.darkInEnd);
        library.settings.darkMaxLight=99;library.normalize();assertEquals(15,library.settings.darkMaxLight);
        library.settings.darkMaxLight=-99;library.normalize();assertEquals(0,library.settings.darkMaxLight);
    }
    @Test void netherAndEndAreOptInEvenWhenDarkAndCovered(){
        assertFalse(EnvironmentConditions.dark("minecraft:the_nether",true,0,library.settings));
        assertFalse(EnvironmentConditions.dark("minecraft:the_end",true,0,library.settings));
    }
    @Test void netherToggleIgnoresLightAndCoverWithoutEnablingTheEnd(){
        library.settings.darkInNether=true;library.settings.darkMaxLight=0;
        assertTrue(EnvironmentConditions.dark("minecraft:the_nether",false,15,library.settings));
        assertFalse(EnvironmentConditions.dark("minecraft:the_end",true,0,library.settings));
        assertFalse(EnvironmentConditions.dark("minecraft:overworld",false,0,library.settings));
    }
    @Test void endToggleIgnoresLightAndCoverWithoutEnablingTheNether(){
        library.settings.darkInEnd=true;library.settings.darkMaxLight=0;
        assertTrue(EnvironmentConditions.dark("minecraft:the_end",false,15,library.settings));
        assertFalse(EnvironmentConditions.dark("minecraft:the_nether",true,0,library.settings));
        assertTrue(EnvironmentConditions.dark("another_mod:dimension",true,0,library.settings));
    }
    @Test void dimensionOverrideStillRespectsPriorityAndPlaylistDisable(){
        library.settings.darkInEnd=true;
        var c=new Context("minecraft:the_end","",false,false,6000,false,false,false,15,"world",0,200,0,0,0.8,false);
        assertEquals("dark_exploration",library.select(c).id);
        assertEquals("home",library.select(c.withHome(true)).id);
        var combat=new Context(c.dimension(),"",true,false,6000,false,false,false,15,"world",0,200,0,0,0.8,false);
        assertEquals("combat",library.select(combat).id);
        library.playlist("dark_exploration").enabled=false;
        assertEquals("day_outside",library.select(c).id);
    }
    @Test void enteringDarkAreaUsesExistingBufferFadeAndFullGap(){
        Track outside=new Track("outside","file","Outside",false),dark=new Track("dark","file","Dark",false);
        library.tracks.addAll(List.of(outside,dark));
        library.playlist("day_outside").tracks.put(outside.id,true);
        library.playlist("dark_exploration").tracks.put(dark.id,true);
        library.settings.silenceMinSeconds=library.settings.silenceMaxSeconds=10;
        library.settings.bufferSeconds=3;library.settings.fadeSeconds=2;
        var audio=new PlaybackEngineTest.FakeAudio();var engine=new PlaybackEngine(audio,new Random(1));
        var bright=context(true,false,false,15,100);var dim=context(true,false,true,0,100);
        engine.tick(0,library,bright,Set.of());engine.tick(10,library,bright,Set.of());engine.tick(12,library,bright,Set.of());
        assertSame(outside,engine.current());
        engine.tick(13,library,dim,Set.of());engine.tick(15,library,dim,Set.of());assertEquals(PlaybackEngine.Phase.PLAYING,engine.phase());
        engine.tick(16,library,dim,Set.of());assertEquals(PlaybackEngine.Phase.FADING,engine.phase());
        engine.tick(18,library,dim,Set.of());assertNull(engine.current());
        engine.tick(27.9,library,dim,Set.of());assertNull(engine.current());
        engine.tick(28,library,dim,Set.of());assertSame(dark,engine.current());
    }
}

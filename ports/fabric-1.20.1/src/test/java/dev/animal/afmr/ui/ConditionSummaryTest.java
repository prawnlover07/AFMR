package dev.animal.afmr.ui;

import dev.animal.afmr.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConditionSummaryTest {
    @Test void standardWindowsUseMinecraftClockTimes(){assertEquals("06:00–18:00",ConditionOption.time(0,11999));assertEquals("18:00–06:00",ConditionOption.time(12000,23999));assertEquals("All day",ConditionOption.time(0,23999));}
    @Test void customWindowsKeepMidnightWrappingAndOneTickMeaning(){assertEquals("20:00–04:00",ConditionOption.time(14000,22000));assertEquals("12:00 (1 tick)",ConditionOption.time(6000,6000));}
    @Test void singleSelectionsShowTheirNameAndLargerSetsStayCompact(){var c=new PlaylistConditions();c.biomes.add("minecraft:flower_forest");assertEquals("Flower forest",ConditionOption.BIOME.summary(c));c.biomes.add("minecraft:plains");assertEquals("2 biomes",ConditionOption.BIOME.summary(c));assertEquals("Survival",ConditionOption.GAME_MODE.summary(c));}
    @Test void areaSummariesRetainFractionalDistancesAndUnconfiguredState(){var c=new PlaylistConditions();assertEquals("Not set",ConditionOption.AREA.summary(c));c.area.configured=true;c.area.radius=64;assertEquals("64 blocks",ConditionOption.AREA.summary(c));c.area.radius=12.5;assertEquals("12.5 blocks",ConditionOption.AREA.summary(c));}
}

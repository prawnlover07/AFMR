package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import net.minecraft.client.gui.screens.Screen;

/** Creates the detailed editor from both workspace rows and nested condition groups. */
final class ConditionEditors {
    static Screen create(ConditionOption option,Screen parent,ClientRuntime runtime,PlaylistConditions c){
        return switch(option){
            case AREA->new AreaConditionScreen(parent,runtime,c);
            case TIME->new RangeConditionScreen(parent,runtime,c,RangeConditionScreen.Kind.TIME);
            case DIMENSION->new DimensionConditionScreen(parent,runtime,c);
            case ELEVATION->new RangeConditionScreen(parent,runtime,c,RangeConditionScreen.Kind.ELEVATION);
            case COVER->new CoverConditionScreen(parent,runtime,c);
            case TEMPERATURE->new RangeConditionScreen(parent,runtime,c,RangeConditionScreen.Kind.TEMPERATURE);
            case LIGHT->new RangeConditionScreen(parent,runtime,c,RangeConditionScreen.Kind.LIGHT);
            case COMBAT->new CombatConditionScreen(parent,runtime,c);
            case WATER,RIDING->new StateConditionScreen(parent,runtime,c,option);
            case BIOME->new BiomeConditionScreen(parent,runtime,c);
            case STRUCTURE->new StructureConditionScreen(parent,runtime,c);
            case WEATHER->new WeatherConditionScreen(parent,runtime,c);
            case HEALTH->new HealthConditionScreen(parent,runtime,c);
            case HELD_ITEM->new HeldItemConditionScreen(parent,runtime,c);
            case GAME_MODE->new GameModeConditionScreen(parent,runtime,c);
            case NEARBY_PLAYER->new NearbyPlayerConditionScreen(parent,runtime,c);
        };
    }
}

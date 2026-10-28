package dev.animal.afmr.ui;

import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.State;
import java.util.EnumMap;

/** Condition controls share one ordering and retain tri-state choices during editing. */
enum ConditionOption {
    AREA("Playback area","afmr.copy.playlistconditions.location"), TIME("Time of day","afmr.copy.playlistconditions.time"), DIMENSION("Dimension","afmr.copy.playlistconditions.dimension"),
    BIOME("Biomes","afmr.copy.playlistconditions.biomes"), ELEVATION("Height (Y)","afmr.copy.playlistconditions.elevation"), COVER("Sky exposure","afmr.copy.playlistconditions.cover"),
    TEMPERATURE("Biome temperature","afmr.copy.playlistconditions.biome_temperature"), LIGHT("Light level","afmr.copy.playlistconditions.light"), COMBAT("Combat","afmr.copy.playlistconditions.combat"),
    WATER("Underwater","afmr.copy.playlistconditions.water"), STRUCTURE("Structures","afmr.copy.playlistconditions.structures"), WEATHER("Weather","afmr.copy.playlistconditions.weather"),
    HEALTH("Health","afmr.copy.playlistconditions.health"), HELD_ITEM("Held items","afmr.copy.playlistconditions.held_items"), GAME_MODE("Gamemode","afmr.copy.playlistconditions.gamemode"),
    NEARBY_PLAYER("Nearby player","afmr.copy.playlistconditions.nearby_player"), RIDING("Riding animal","afmr.copy.playlistconditions.riding_animal");
    final String label,help;
    ConditionOption(String label,String help){this.label=label;this.help=help;}
    boolean enabled(PlaylistConditions c){return switch(this){
        case AREA->c.areaEnabled;case TIME->c.timeEnabled;case DIMENSION->c.dimensionEnabled;case ELEVATION->c.elevationEnabled;
        case COVER->c.cover!=State.ANY;case TEMPERATURE->c.temperatureEnabled;case LIGHT->c.lightEnabled;case COMBAT->c.combat!=State.ANY;
        case WATER->c.water!=State.ANY;case BIOME->c.biomeEnabled;case STRUCTURE->c.structureEnabled;case WEATHER->c.weatherEnabled;
        case HEALTH->c.healthEnabled;case HELD_ITEM->c.heldItemEnabled;case GAME_MODE->c.gameModeEnabled;case NEARBY_PLAYER->c.nearbyPlayerEnabled;case RIDING->c.ridingAnimal!=State.ANY;
    };}
    void toggle(PlaylistConditions c,EnumMap<ConditionOption,State> remembered){
        switch(this){
            case AREA->c.areaEnabled=!c.areaEnabled;case TIME->c.timeEnabled=!c.timeEnabled;case DIMENSION->c.dimensionEnabled=!c.dimensionEnabled;
            case ELEVATION->c.elevationEnabled=!c.elevationEnabled;case TEMPERATURE->c.temperatureEnabled=!c.temperatureEnabled;
            case LIGHT->c.lightEnabled=!c.lightEnabled;case BIOME->c.biomeEnabled=!c.biomeEnabled;case STRUCTURE->c.structureEnabled=!c.structureEnabled;
            case WEATHER->c.weatherEnabled=!c.weatherEnabled;case HEALTH->c.healthEnabled=!c.healthEnabled;case HELD_ITEM->c.heldItemEnabled=!c.heldItemEnabled;
            case GAME_MODE->c.gameModeEnabled=!c.gameModeEnabled;case NEARBY_PLAYER->c.nearbyPlayerEnabled=!c.nearbyPlayerEnabled;
            case COVER->c.cover=toggleState(c.cover,remembered);case COMBAT->c.combat=toggleState(c.combat,remembered);
            case WATER->c.water=toggleState(c.water,remembered);case RIDING->c.ridingAnimal=toggleState(c.ridingAnimal,remembered);
        }
    }
    private State toggleState(State current,EnumMap<ConditionOption,State> remembered){
        if(current==State.ANY)return remembered.getOrDefault(this,State.YES);
        remembered.put(this,current);return State.ANY;
    }
    String summary(PlaylistConditions c){return switch(this){
        case AREA->c.area.configured?number(c.area.radius)+" blocks":"Not set";
        case TIME->time(c.startTime,c.endTime);case DIMENSION->name(c.dimension);
        case ELEVATION->c.minY+"–"+c.maxY;case COVER->c.cover==State.NO?"Open sky":"Covered";
        case TEMPERATURE->c.minTemperature+"–"+c.maxTemperature;case LIGHT->c.minLight+"–"+c.maxLight;
        case COMBAT->c.combat==State.NO?"Out of combat":"In combat";case WATER->c.water==State.NO?"Out of water":"Underwater";
        case BIOME->selection(c.biomes,"biomes");case STRUCTURE->selection(c.structures,"structures");case WEATHER->c.weather.label();
        case HEALTH->(c.healthComparison==PlaylistConditions.HealthComparison.BELOW?"Below ":"Above ")+number(c.healthThreshold)+" hearts";
        case HELD_ITEM->selection(c.heldItems,"items")+" / "+c.heldItemHand.label();case GAME_MODE->c.gameModes.size()==1?c.gameModes.iterator().next().label():c.gameModes.size()+" modes";
        case NEARBY_PLAYER->(c.nearbyPlayerName.isBlank()?"Player":c.nearbyPlayerName)+" / "+number(c.nearbyPlayerRadius)+" blocks";case RIDING->c.ridingAnimal==State.NO?"Not riding":"Riding";
    };}
    static String time(int start,int end){
        if(start==0&&end==23999)return "All day";
        if(start==0&&end==11999)return "06:00–18:00";
        if(start==12000&&end==23999)return "18:00–06:00";
        return start==end?clock(start)+" (1 tick)":clock(start)+"–"+clock(end);
    }
    private static String clock(int tick){int minutes=Math.floorMod(tick+6000,24000)*60/1000;return (minutes/60<10?"0":"")+minutes/60+":"+(minutes%60<10?"0":"")+minutes%60;}
    private static String number(double value){return value==Math.rint(value)?Long.toString((long)value):Double.toString(value);}
    private static String name(String id){String value=id.startsWith("minecraft:")?id.substring(10):id;value=value.replace('_',' ');return value.isBlank()?"Not set":Character.toUpperCase(value.charAt(0))+value.substring(1);}
    private static String selection(java.util.Set<String> values,String plural){return values.isEmpty()?"Not set":values.size()==1?name(values.iterator().next()):values.size()+" "+plural;}

}

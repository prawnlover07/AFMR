package dev.animal.afmr.library;

import com.google.gson.*;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.State;
import dev.animal.afmr.core.PlaylistTemplates;
import java.util.*;

/** Expand saved rule presets into ordinary editable data before loading the model. */
final class SavedPlaylistRules {
    private SavedPlaylistRules(){}
    static Library read(JsonObject json,Gson gson){
        JsonObject settings=json.has("settings")&&!json.get("settings").isJsonNull()?json.getAsJsonObject("settings"):new JsonObject();
        Home home=json.has("home")?gson.fromJson(json.get("home"),Home.class):new Home();
        expand(json.getAsJsonArray("playlists"),settings,home,gson);
        if(json.has("legacyPlaylists"))expand(json.getAsJsonArray("legacyPlaylists"),settings,home,gson);
        Library library=gson.fromJson(json,Library.class);
        if(library.schemaVersion==1)upgradeFirstLayout(library);
        return library;
    }
    private static void expand(JsonArray playlists,JsonObject settings,Home home,Gson gson){
        if(playlists==null)return;
        for(var element:playlists){
            var p=element.getAsJsonObject();
            if(!p.has("rule"))continue;
            String rule=p.get("rule").getAsString();
            String match=p.has("match")?p.get("match").getAsString():"";
            var c=new PlaylistConditions();
            switch(rule){
                case "CUSTOM"->{if(p.has("conditions")&&!p.get("conditions").isJsonNull())c=gson.fromJson(p.get("conditions"),PlaylistConditions.class);}
                case "GENERAL"->{}
                case "COMBAT"->c.combat=State.YES;
                case "UNDERWATER"->c.water=State.YES;
                case "DIMENSION"->{c.dimensionEnabled=true;c.dimension=match;}
                case "BIOME"->{c.biomeEnabled=true;c.biomes.add(match);}
                case "DAY","NIGHT"->{c.timeEnabled=true;c.startTime=rule.equals("DAY")?0:12000;c.endTime=rule.equals("DAY")?11999:23999;}
                case "HOME"->{c.areaEnabled=true;c.area=home==null?new Home():gson.fromJson(gson.toJsonTree(home),Home.class);}
                case "UNDERGROUND","DAY_OUTSIDE","NIGHT_OUTSIDE"->{
                    String id=rule.toLowerCase(Locale.ROOT);
                    c=PlaylistTemplates.create().stream().filter(t->t.id.equals(id)).findFirst().orElseThrow().conditions;
                    var underground=rule.equals("UNDERGROUND")?c:c.groups.get(0);
                    underground.maxY=integer(settings,"undergroundMaxY",64);underground.minCoverDepth=number(settings,"undergroundDepth",5);
                }
                case "DARK_EXPLORATION"->{
                    c=PlaylistTemplates.create().get(2).conditions;
                    c.groups.get(0).maxLight=integer(settings,"darkMaxLight",4);
                    c.groups.get(1).enabled=flag(settings,"darkInNether");c.groups.get(2).enabled=flag(settings,"darkInEnd");
                }
                default->throw new IllegalArgumentException("Unknown saved playlist rule: "+rule);
            }
            p.add("conditions",gson.toJsonTree(c));p.remove("rule");p.remove("match");
        }
    }
    private static int integer(JsonObject settings,String key,int fallback){return settings.has(key)?settings.get(key).getAsInt():fallback;}
    private static double number(JsonObject settings,String key,double fallback){return settings.has(key)?settings.get(key).getAsDouble():fallback;}
    private static boolean flag(JsonObject settings,String key){return settings.has(key)&&settings.get(key).getAsBoolean();}
    private static void upgradeFirstLayout(Library library){
        var old=library.playlists;library.playlists=new ArrayList<>(PlaylistTemplates.create());library.legacyPlaylists.addAll(old);
        for(String[] mapping:new String[][]{{"combat","combat"},{"day","day_outside"},{"night","night_outside"}}){
            var from=old.stream().filter(p->p.id.equals(mapping[0])).findFirst().orElse(null);var to=library.playlist(mapping[1]);
            if(from!=null){to.tracks.putAll(from.tracks);to.finishCurrentSong=from.finishCurrentSong;to.finishCurrentSongOnExit=from.finishCurrentSongOnExit;to.includeVanilla=from.includeVanilla;if(!from.tracks.isEmpty())to.enabled=from.enabled;}
        }
        var general=old.stream().filter(p->p.id.equals("general")).findFirst().orElse(null);
        if(general!=null)for(String id:List.of("day_outside","night_outside","underground")){
            var to=library.playlist(id);general.tracks.forEach(to.tracks::putIfAbsent);
            if(to.tracks.equals(general.tracks)){to.includeVanilla=general.includeVanilla;to.finishCurrentSong=general.finishCurrentSong;to.finishCurrentSongOnExit=general.finishCurrentSongOnExit;}
        }
        for(var folder:library.folders)folder.playlistId=switch(folder.playlistId){case "general"->"ambient";case "day"->"day_outside";case "night"->"night_outside";case "combat"->"combat";default->"library";};
        library.schemaVersion=2;
    }
}

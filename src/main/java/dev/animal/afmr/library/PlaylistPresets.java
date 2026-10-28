package dev.animal.afmr.library;

import com.google.gson.*;
import com.google.gson.stream.*;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.TransitionRule;
import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

/** Portable playlist data. No audio, absolute music paths, profile bindings or global settings. */
public final class PlaylistPresets {
    public static final int MAX_PLAYLISTS=128,MAX_BYTES=16*1024*1024;
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final String FORMAT="afmr-playlists";
    public static final class FileData {
        public String format=FORMAT;
        public List<Playlist> playlists=new ArrayList<>();
        public List<PresetSongs.FolderRef> folders=new ArrayList<>();
        public List<PresetSongs.Song> songs=new ArrayList<>();
    }
    public enum Placement { ABOVE_SELECTED, TOP, BOTTOM }
    private PlaylistPresets(){}
    public static boolean supported(Path path){return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".afmrp");}
    public static FileData capture(Library lib,Set<String> selected,boolean includeSongs){
        var data=new FileData();var originals=new HashMap<String,String>();
        for(var original:lib.playlists)if(selected.contains(original.id)){
            var p=Library.copyPlaylist(original,false);p.id="preset:"+data.playlists.size();
            clearWorlds(p.conditions);
            originals.put(p.id,original.id);
            data.playlists.add(p);
        }
        var exported=new HashMap<String,String>();for(var entry:originals.entrySet())exported.put(entry.getValue(),entry.getKey());
        for(var p:data.playlists){var rules=new LinkedHashMap<String,TransitionRule>();
            for(var entry:p.transitions.entrySet())if(exported.containsKey(entry.getKey()))rules.put(exported.get(entry.getKey()),entry.getValue());
            p.transitions=rules;
        }
        if(includeSongs)PresetSongs.capture(lib,originals,data);
        validate(data);return data;
    }
    private static void clearWorlds(PlaylistConditions c){c.area.world="";for(var child:c.groups)clearWorlds(child);}
    public static void write(Path path,FileData data)throws IOException{
        if(!supported(path))throw new IOException("Choose a .afmrp filename.");
        validate(data);byte[] bytes=GSON.toJson(data).getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IOException("Playlist file exceeds 16 MiB.");
        Path absolute=path.toAbsolutePath().normalize(),temporary=Files.createTempFile(absolute.getParent(),".afmrp-",".tmp");
        try{
            Files.write(temporary,bytes);
            try{Files.move(temporary,absolute,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,absolute,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
    public static FileData read(Path path)throws IOException{
        if(!supported(path))throw new IOException("Choose an .afmrp playlist file.");
        if(Files.size(path)>MAX_BYTES)throw new IOException("Playlist file exceeds 16 MiB.");
        byte[] bytes;
        try(var input=Files.newInputStream(path)){bytes=input.readNBytes(MAX_BYTES+1);}
        if(bytes.length>MAX_BYTES)throw new IOException("Playlist file exceeds 16 MiB.");
        try{
            String json=StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();preflight(json);
            JsonObject root=JsonParser.parseString(json).getAsJsonObject();
            keys(root,FileData.class);
            if(!root.has("format")||!root.has("playlists")||!root.get("playlists").isJsonArray())throw new IllegalArgumentException("Missing AFMR format or playlists");
            int size=root.getAsJsonArray("playlists").size();
            if(size<1||size>MAX_PLAYLISTS)throw new IllegalArgumentException("Use 1-"+MAX_PLAYLISTS+" playlists");
            for(var element:root.getAsJsonArray("playlists")){
                var p=element.getAsJsonObject();keys(p,Playlist.class);
                if(p.has("transitions"))for(var rule:p.getAsJsonObject("transitions").entrySet())keys(rule.getValue().getAsJsonObject(),TransitionRule.class);
                if(p.has("tracks"))for(var assignment:p.getAsJsonObject("tracks").entrySet())
                    if(!assignment.getValue().isJsonPrimitive()||!assignment.getValue().getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Song enabled state must be a boolean");
                if(!p.has("conditions"))throw new IllegalArgumentException("Missing playlist conditions");
                conditionKeys(p.getAsJsonObject("conditions"),0,new int[1]);
            }
            if(root.has("folders"))for(var folder:root.getAsJsonArray("folders"))keys(folder.getAsJsonObject(),PresetSongs.FolderRef.class);
            if(root.has("songs"))for(var song:root.getAsJsonArray("songs"))keys(song.getAsJsonObject(),PresetSongs.Song.class);
            FileData data=GSON.fromJson(root,FileData.class);validate(data);return data;
        }catch(RuntimeException e){throw new IOException("Invalid AFMR playlist file: "+e.getMessage(),e);}
    }
    private static void preflight(String json)throws IOException{
        try(var reader=new JsonReader(new StringReader(json))){
            reader.setStrictness(Strictness.STRICT);int depth=0;
            var names=new ArrayDeque<Set<String>>();
            while(reader.peek()!=JsonToken.END_DOCUMENT)switch(reader.peek()){
                case BEGIN_OBJECT->{if(++depth>32)throw new IOException("Playlist nesting exceeds limits.");reader.beginObject();names.push(new HashSet<>());}
                case END_OBJECT->{reader.endObject();names.pop();depth--;}
                case BEGIN_ARRAY->{if(++depth>32)throw new IOException("Playlist nesting exceeds limits.");reader.beginArray();}
                case END_ARRAY->{reader.endArray();depth--;}
                case NAME->{String name=reader.nextName();if(name.length()>1024||!names.peek().add(name))throw new IOException("Invalid or duplicate field: "+name);}
                case STRING,NUMBER->{if(reader.nextString().length()>4096)throw new IOException("Playlist value is too long.");}
                case BOOLEAN->reader.nextBoolean();
                case NULL->reader.nextNull();
                default->throw new IOException("Invalid playlist JSON.");
            }
        }
    }
    private static void keys(JsonObject json,Class<?> type){
        var allowed=new HashSet<String>();
        for(var field:type.getFields())if(!Modifier.isStatic(field.getModifiers())&&!Modifier.isTransient(field.getModifiers())){
            allowed.add(field.getName());if(!json.has(field.getName()))continue;
            JsonElement value=json.get(field.getName());Class<?> kind=field.getType();
            if(value.isJsonNull())throw new IllegalArgumentException("Null field: "+field.getName());
            boolean valid;
            if(kind==String.class||kind.isEnum())valid=value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString();
            else if(kind==boolean.class)valid=value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean();
            else if(kind.isPrimitive())valid=value.isJsonPrimitive()&&value.getAsJsonPrimitive().isNumber()&&Double.isFinite(value.getAsDouble());
            else if(Collection.class.isAssignableFrom(kind))valid=value.isJsonArray();
            else valid=value.isJsonObject();
            if(!valid)throw new IllegalArgumentException("Invalid field type: "+field.getName());
        }
        for(String key:json.keySet())if(!allowed.contains(key))throw new IllegalArgumentException("Unsupported field '"+key+"'; this setup may need a newer AFMR.");
    }
    private static void conditionKeys(JsonObject c,int depth,int[] count){
        if(depth>PlaylistConditions.MAX_DEPTH||++count[0]>PlaylistConditions.MAX_NODES)throw new IllegalArgumentException("Too many condition groups");
        keys(c,PlaylistConditions.class);
        if(c.has("proximityVolume"))keys(c.getAsJsonObject("proximityVolume"),dev.animal.afmr.core.PlayerProximityVolume.class);
        if(c.has("area"))keys(c.getAsJsonObject("area"),Home.class);
        if(c.has("groups")){
            var groups=c.getAsJsonArray("groups");if(groups.size()>PlaylistConditions.MAX_GROUPS)throw new IllegalArgumentException("Too many child groups");
            for(var child:groups)conditionKeys(child.getAsJsonObject(),depth+1,count);
        }
    }
    public static void validate(FileData data){
        if(data==null||!FORMAT.equals(data.format)||data.playlists==null||data.playlists.isEmpty()||data.playlists.size()>MAX_PLAYLISTS)
            throw new IllegalArgumentException("Incomplete or oversized AFMR playlist file");
        var ids=new HashSet<String>();for(var p:data.playlists)if(p==null||p.id==null||p.id.isBlank()||!ids.add(p.id))throw new IllegalArgumentException("Invalid playlist identity");
        for(var p:data.playlists){
            if(p.transitions==null||p.transitions.size()>128)throw new IllegalArgumentException("Invalid transition rules");
            for(var entry:p.transitions.entrySet())if(entry.getKey()==null||!ids.contains(entry.getKey())||entry.getKey().equals(p.id)||entry.getValue()==null||!entry.getValue().valid())throw new IllegalArgumentException("Invalid transition rule");
            if(p==null||p.name==null||p.name.isBlank()||p.name.length()>80||p.name.codePoints().anyMatch(Character::isISOControl)||p.conditions==null||p.tracks==null||!Double.isFinite(p.volume)||p.volume<0||p.volume>1
                ||p.songsBeforeSilence<1||p.songsBeforeSilence>1000
                ||!Double.isFinite(p.silenceMinSeconds)||!Double.isFinite(p.silenceMaxSeconds)||p.silenceMinSeconds<0||p.silenceMaxSeconds<p.silenceMinSeconds||p.silenceMaxSeconds>86400)
                throw new IllegalArgumentException("Invalid playlist settings");
            validateConditions(p.conditions,0,new int[1]);
            p.conditions.normalize();
            requirePortableWorlds(p.conditions);
        }
        PresetSongs.validate(data);
    }
    private static void validateConditions(PlaylistConditions c,int depth,int[] count){
        if(c==null||depth>PlaylistConditions.MAX_DEPTH||++count[0]>PlaylistConditions.MAX_NODES||c.groups==null||c.groups.size()>PlaylistConditions.MAX_GROUPS||c.area==null
            ||c.cover==null||c.combat==null||c.water==null||c.ridingAnimal==null||c.weather==null||c.healthComparison==null||c.heldItemHand==null||c.gameModes==null
            ||c.biomes==null||c.structures==null||c.heldItems==null||c.dimension==null||c.area.dimension==null||c.area.world==null)
            throw new IllegalArgumentException("Invalid condition group");
        if(c.gameModes.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Unsupported gamemode condition");
        if(!Double.isFinite(c.nearbyPlayerRadius)||c.nearbyPlayerRadius<1||c.nearbyPlayerRadius>1024
            ||c.nearbyPlayerName==null||c.nearbyPlayerUuid==null
            ||!c.nearbyPlayerName.isEmpty()&&!dev.animal.afmr.core.NearbyPlayers.validName(c.nearbyPlayerName)
            ||!c.nearbyPlayerUuid.isEmpty()&&!dev.animal.afmr.core.NearbyPlayers.validUuid(c.nearbyPlayerUuid)
            ||!Double.isFinite(c.area.x)||!Double.isFinite(c.area.y)||!Double.isFinite(c.area.z)||!Double.isFinite(c.area.radius)
            ||c.proximityVolume==null||!c.proximityVolume.valid()
            ||!Double.isFinite(c.minTemperature)||!Double.isFinite(c.maxTemperature)||!Double.isFinite(c.minCoverDepth)||!Double.isFinite(c.structureRadius)||!Double.isFinite(c.healthThreshold))
            throw new IllegalArgumentException("Invalid condition number");
        for(var child:c.groups)validateConditions(child,depth+1,count);
    }
    private static void requirePortableWorlds(PlaylistConditions c){
        if(!c.area.world.isEmpty())throw new IllegalArgumentException("Portable location contains an original world identifier");
        for(var child:c.groups)requirePortableWorlds(child);
    }
    public static int locations(FileData data){
        int count=0;for(var p:data.playlists)count+=locations(p.conditions);return count;
    }
    private static int locations(PlaylistConditions c){int count=c.area.configured?1:0;for(var child:c.groups)count+=locations(child);return count;}
    private static void bindWorld(PlaylistConditions c,String world){
        if(c.area.configured||!c.area.world.isEmpty())c.area.world=world;
        for(var child:c.groups)bindWorld(child,world);
    }
    public record ImportResult(String profileId,List<String> ids){}
    /** A null profile ID creates an imported-only profile on successful application. */
    public static ImportResult apply(Library lib,String profileId,String newProfileName,String anchor,Placement placement,FileData data,String world,PresetSongs.Plan songs){
        validate(data);
        if(profileId!=null&&!lib.profiles.exists(profileId))throw new IllegalStateException("The destination profile was deleted; import the file again.");
        if(profileId==null){newProfileName=dev.animal.afmr.core.PlaylistProfiles.cleanName(newProfileName);
            if(lib.profiles.inactive.size()+1>=dev.animal.afmr.core.PlaylistProfiles.LIMIT)throw new IllegalArgumentException("Profile limit reached; choose an existing profile or delete one first.");
        }
        if(locations(data)>0&&(world==null||world.isBlank()))throw new IllegalArgumentException("Join a world to bind the imported locations.");
        List<Playlist> destination=profileId==null?new ArrayList<>():lib.profiles.playlists(lib,profileId);
        var byName=new HashMap<String,Playlist>();var ambiguous=new HashSet<String>();
        for(var p:destination)if(byName.putIfAbsent(matchName(p.name),p)!=null)ambiguous.add(matchName(p.name));
        if(profileId!=null){var importedNames=new HashSet<String>();for(var p:data.playlists){
            String name=matchName(p.name);
            if(ambiguous.contains(name)||!importedNames.add(name))throw new IllegalArgumentException("Ambiguous playlist name: "+p.name+". Rename duplicate playlists before importing into this profile.");
        }}
        int position=placement==Placement.TOP?0:destination.size();
        if(placement==Placement.ABOVE_SELECTED)for(int i=0;i<destination.size();i++)if(destination.get(i).id.equals(anchor)){position=i;break;}
        var prepared=new ArrayList<Playlist>(data.playlists.size());var originals=new ArrayList<Playlist>(data.playlists.size());
        var imported=new HashMap<String,String>();
        for(var source:data.playlists){
            var existing=byName.get(matchName(source.name));var p=Library.copyPlaylist(source,false);
            p.id=existing==null?"custom:"+UUID.randomUUID():existing.id;
            if(existing!=null&&songs==null)p.tracks=existing.tracks;
            if(world!=null&&!world.isBlank())bindWorld(p.conditions,world);
            prepared.add(p);originals.add(existing);imported.put(source.id,p.id);
        }
        for(var p:prepared){var rules=new LinkedHashMap<String,TransitionRule>();
            for(var entry:p.transitions.entrySet())rules.put(imported.get(entry.getKey()),entry.getValue());p.transitions=rules;
        }
        // Complete validation/preparation before touching existing playlist objects.
        if(songs!=null)PresetSongs.apply(lib,data,songs,prepared);
        var added=new ArrayList<Playlist>();
        for(int i=0;i<prepared.size();i++){
            var p=prepared.get(i);var existing=originals.get(i);
            if(existing==null)added.add(p);else replaceSettings(existing,p);
        }
        destination.addAll(position,added);
        if(profileId==null)profileId=lib.profiles.createImported(newProfileName,destination);
        return new ImportResult(profileId,prepared.stream().map(p->p.id).toList());
    }
    public static String matchName(String name){return name.strip().toLowerCase(Locale.ROOT);}
    /** Keep identity, position and live scheduler references while transferring the owned draft. */
    private static void replaceSettings(Playlist target,Playlist source){
        target.name=source.name;target.enabled=source.enabled;target.manualOnly=source.manualOnly;
        target.finishCurrentSong=source.finishCurrentSong;target.finishCurrentSongOnExit=source.finishCurrentSongOnExit;
        target.playOnTrigger=source.playOnTrigger;target.customSilence=source.customSilence;
        target.customSongCount=source.customSongCount;target.songsBeforeSilence=source.songsBeforeSilence;
        target.silenceMinSeconds=source.silenceMinSeconds;target.silenceMaxSeconds=source.silenceMaxSeconds;
        target.includeVanilla=source.includeVanilla;target.volume=source.volume;target.conditions=source.conditions;
        target.tracks=source.tracks;target.transitions=source.transitions;
    }
}

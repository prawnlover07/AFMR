package dev.animal.afmr.library;

import com.google.gson.*;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Expands actual sound files, so vanilla songs can be toggled individually. */
public final class VanillaCatalog {
    private final Map<String, List<JsonElement>> events = new HashMap<>();
    private final Map<ResourceLocation, Set<String>> musicPools = new HashMap<>();
    public void reload(Minecraft mc, Library library) {
        events.clear(); musicPools.clear();
        for (String namespace : mc.getResourceManager().getNamespaces()) {
            List<net.minecraft.server.packs.resources.Resource> resources;
            try{resources=mc.getResourceManager().getResources(new ResourceLocation(namespace,"sounds.json"));}
            catch(java.io.IOException absent){continue;}
            for (var resource : resources) {
                try (var reader = new java.io.InputStreamReader(resource.getInputStream(),java.nio.charset.StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : json.entrySet()) {
                        JsonObject value = entry.getValue().getAsJsonObject();
                        String key = namespace + ":" + entry.getKey();
                        if (value.has("replace") && value.get("replace").getAsBoolean()) events.remove(key);
                        if (value.has("sounds")) for (var sound : value.getAsJsonArray("sounds"))
                            events.computeIfAbsent(key, unused -> new ArrayList<>()).add(sound);
                    }
                } catch (Exception e) { dev.animal.afmr.Afmr.LOG.warn("Cannot index music in {}", namespace, e); }
            }
        }
        Set<String> files = new TreeSet<>();
        for (String key : events.keySet()) if (new ResourceLocation(key).getPath().startsWith("music.")) {
            Set<String> expanded = new HashSet<>(); expand(key, expanded, new HashSet<>());
            files.addAll(expanded);
            Set<String> ids = new HashSet<>(); for (String file : expanded) ids.add("vanilla:" + file);
            musicPools.put(new ResourceLocation(key), Set.copyOf(ids));
        }
        Map<String, Track> existing = new HashMap<>(); library.tracks.forEach(t -> {
            existing.put(t.id,t);if(t.vanilla)t.durationSeconds=0; // Resource packs can replace a file at the same ID.
        });
        for (String file : files) {
            String id = "vanilla:" + file;
            if (!existing.containsKey(id)) {
                ResourceLocation location = new ResourceLocation(file);
                String translation = (location.getNamespace().equals("minecraft")?location.getPath():location.toString().replace(':','.'));
                String title = new net.minecraft.network.chat.TranslatableComponent(translation).getString();
                if (title.equals(translation)) {
                    String name = location.getPath(); name = name.substring(name.lastIndexOf('/') + 1).replace('_', ' ');
                    title = name.isEmpty() ? file : Character.toUpperCase(name.charAt(0)) + name.substring(1);
                }
                Track track = new Track(id, file, title, true); track.artist = "Minecraft soundtrack";
                library.tracks.add(track);
            }
        }
        library.rebuildTrackIndex();
    }
    private void expand(String event, Set<String> output, Set<String> visited) {
        if (!visited.add(event)) return;
        String namespace = new ResourceLocation(event).getNamespace();
        for (var element : events.getOrDefault(event, List.of())) {
            String name; boolean nested = false;
            if (element.isJsonPrimitive()) name = element.getAsString();
            else {
                var value = element.getAsJsonObject();
                name = value.get("name").getAsString();
                nested = value.has("type") && value.get("type").getAsString().equals("event");
            }
            if (!name.contains(":")) name = namespace + ":" + name;
            if (nested) expand(name, output, visited); else output.add(name);
        }
    }
    public Set<String> applicable(Minecraft mc) {
        var music = mc.getSituationalMusic();
        if (music == null) return Set.of();
        return musicPools.getOrDefault(music.getEvent().getLocation(), Set.of());
    }
}

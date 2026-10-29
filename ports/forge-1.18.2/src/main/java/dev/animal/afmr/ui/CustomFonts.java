package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.FontFiles;
import dev.animal.afmr.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import com.mojang.bridge.game.PackType;
import net.minecraft.SharedConstants;
import com.google.gson.JsonObject;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Selection and render caches are client-owned; workers exclusively own font-file preparation. */
public final class CustomFonts implements AutoCloseable {
    public enum Target { MENU, WIDGET }
    private final ClientRuntime runtime;
    private final Path config;
    private final Minecraft mc=Minecraft.getInstance();
    private boolean busy,started,closed;
    private String status="";
    private final Font[] fonts=new Font[2];
    private final FontSet[] sets=new FontSet[2];
    private final String[] ids={"",""};
    public CustomFonts(ClientRuntime runtime,Path config){this.runtime=runtime;this.config=config;}
    public boolean busy(){return busy;}
    public String status(){return busy?"Loading fonts…":status;}
    public String selection(Target target){var settings=runtime.library().settings;return target==Target.MENU?settings.menuFontId:settings.widgetFontId;}
    public String name(Target target){String id=selection(target);if(id.equals("minecraft:default"))return "Minecraft default";var s=runtime.library().settings;String name=target==Target.MENU?s.menuFontName:s.widgetFontName;return id.equals("afmr:custom_"+target.name().toLowerCase(Locale.ROOT))&&!name.isBlank()?name:id;}
    private Map<ResourceLocation,FontSet> available(){return ((FontManagerAccess)((MinecraftFontAccess)mc).afmr$fontManager()).afmr$fontSets();}
    public List<String> choices(){return available().keySet().stream().map(ResourceLocation::toString).filter(id->!id.equals("minecraft:missing")).sorted().toList();}
    public Font font(Target target){
        String id=selection(target);int index=target.ordinal();var key=ResourceLocation.tryParse(id);var set=key==null?null:available().get(key);
        if(id.equals("minecraft:default")||set==null)return mc.font;
        if(fonts[index]==null||sets[index]!=set||!ids[index].equals(id)){
            fonts[index]=new Font(requested->available().getOrDefault(requested.equals(new ResourceLocation("minecraft","default"))?key:requested,available().get(new ResourceLocation("minecraft","default"))));sets[index]=set;ids[index]=id;
        }return fonts[index];
    }
    public void select(Target target,String id){
        if(busy||closed)return;
        if(ResourceLocation.tryParse(id)==null||!choices().contains(id)){status="Font is not available in the active resource packs.";return;}
        var settings=runtime.library().settings;if(target==Target.MENU)settings.menuFontId=id;else settings.widgetFontId=id;status="";runtime.save();
    }
    public void start(){
        if(started)return;started=true;
        if(!selection(Target.MENU).equals("afmr:custom_menu")&&!selection(Target.WIDGET).equals("afmr:custom_widget"))return;
        prepare(null,null);
    }
    public void choose(Target target,Path source){if(!closed&&!busy&&runtime.enabled()&&!runtime.backupBusy())prepare(target,source);}
    private String metadata(){
        var pack=new JsonObject();pack.addProperty("description","AFMR imported fonts");pack.addProperty("pack_format",SharedConstants.getCurrentVersion().getPackVersion(PackType.RESOURCE));
        var root=new JsonObject();root.add("pack",pack);return root.toString();
    }
    @Override public void close(){closed=true;Arrays.fill(fonts,null);Arrays.fill(sets,null);}
    private record Prepared(String name,boolean changed){}
    private void prepare(Target target,Path source){
        busy=true;status="";String metadata=metadata();var resources=mc.getResourceManager();Path packs=mc.gameDirectory.toPath().resolve("resourcepacks");
        CompletableFuture.supplyAsync(()->{
            try{
                FontFiles.packDirectory(packs);
                String name="";if(source!=null){var asset=FontFiles.read(source);name=asset.name();FontFiles.install(config.resolve(target.name().toLowerCase(Locale.ROOT)+"-font.ttf"),asset);}
                var fallback=new com.google.gson.JsonArray();
                var defaults=resources.getResources(new ResourceLocation("minecraft","font/default.json"));
                int remaining=1024*1024;
                // Resource manager returns packs low to high priority; higher packs win first.
                for(int i=defaults.size()-1;i>=0;i--){
                    var resource=defaults.get(i);
                    try(var input=resource.getInputStream()){
                        byte[] bytes=input.readNBytes(remaining+1);remaining-=bytes.length;
                        if(remaining<0)throw new java.io.IOException("Default font definitions exceed 1 MiB.");
                        var json=com.google.gson.JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                        for(var provider:json.getAsJsonArray("providers"))fallback.add(provider);
                    }
                }
                boolean changed=source!=null||!FontFiles.currentPack(config,packs,metadata,fallback);
                if(changed)FontFiles.buildPack(config,packs,metadata,fallback);return new Prepared(name,changed);
            }catch(Exception ex){throw new CompletionException(ex);}
        }).whenComplete((name,failure)->mc.execute(()->{
            if(closed)return;
            if(failure!=null){busy=false;status="Could not load font: "+failure.getCause().getMessage();return;}
            var repository=mc.getResourcePackRepository();repository.reload();var selected=new ArrayList<>(repository.getSelectedIds());String pack="file/"+FontFiles.PACK;
            if(!repository.getAvailableIds().contains(pack)){busy=false;status="Minecraft could not load the AFMR font pack.";return;}
            if(!name.changed&&selected.contains(pack)){busy=false;return;}
            if(!selected.contains(pack))selected.add(pack);repository.setSelected(selected);
            if(!mc.options.resourcePacks.contains(pack)){mc.options.resourcePacks.add(pack);mc.options.save();}
            mc.reloadResourcePacks().whenComplete((unused,error)->mc.execute(()->{
                if(closed)return;busy=false;if(error!=null){status="Could not reload font resources.";return;}
                if(target!=null){var s=runtime.library().settings;String id="afmr:custom_"+target.name().toLowerCase(Locale.ROOT);if(target==Target.MENU){s.menuFontId=id;s.menuFontName=name.name;}else{s.widgetFontId=id;s.widgetFontName=name.name;}runtime.save();}
                status="";
            }));
        }));
    }
}

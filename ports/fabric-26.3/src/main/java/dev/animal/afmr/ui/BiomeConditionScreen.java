package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Registry discovery and searching happen only while editing, never during playback. */
final class BiomeConditionScreen extends MusicScreen {
    private record Entry(String id,String label,String search) {}
    private final PlaylistConditions conditions;
    private final Set<String> selected;
    private final Set<String> available=new HashSet<>();
    private List<Entry> entries=List.of();
    private DenseList<Entry> list;
    private FlatButton toggle;
    private EditBox search;
    private String query="",error="";
    private boolean enabled;

    BiomeConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Biome condition",parent,runtime);this.conditions=conditions;
        selected=new LinkedHashSet<>(conditions.biomes);enabled=conditions.biomeEnabled;
    }
    @Override protected void init(){
        super.init();available.clear();
        if(minecraft.level!=null)for(var id:minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME).keySet())available.add(id.toString());
        int half=(contentWidth-6)/2;
        toggle=toggle("Filter",left,47,half,()->enabled,()->{enabled=!enabled;});
        button("Add current biome",left+half+6,47,half,()->add(runtime.context().biome())).active=runtime.inWorld();
        list=addRenderableWidget(new DenseList<>(left,97,contentWidth,Math.max(14,bodyBottom()-155),
            (g,item,x,y,w,hover)->g.text(menuFont,fit((selected.contains(item.id())?"[x] ":"[ ] ")+item.label(),w-8),x+4,y+3,
                selected.contains(item.id())?MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF):hover?MenuAppearance.color(MenuColor.TEXT,0xFFFFFFFF):MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED)),
            (item,event,twice)->{if(!selected.remove(item.id())){selected.add(item.id());enabled=true;}error="";},Entry::label));
        search=edit(query,"Search biomes or enter namespace:id",left,71,contentWidth-82,value->{query=value;filter();});
        button("Add ID",left+contentWidth-76,71,76,()->add(search.getValue().strip()));
        button("Clear / any",left,bodyBottom()-29,100,()->{selected.clear();enabled=false;error="";});
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
        rebuildEntries();
    }
    private void rebuildEntries(){
        var ids=new TreeSet<>(available);ids.addAll(selected);
        entries=ids.stream().map(id->{
            int colon=id.indexOf(':');String name=id.substring(colon+1).replace('_',' ');
            String label=name+"  |  "+id+(available.contains(id)?"":" (not in current world)");
            return new Entry(id,label,label.toLowerCase(Locale.ROOT));
        }).toList();filter();
    }
    private void filter(){
        String needle=query.strip().toLowerCase(Locale.ROOT);
        list.items(needle.isEmpty()?entries:entries.stream().filter(e->e.search().contains(needle)).toList());list.top();
    }
    private void add(String value){
        var id=Identifier.tryParse(value);
        if(id==null||value.isBlank()){error="Enter an ID such as minecraft:plains.";return;}
        selected.add(id.toString());enabled=true;error="";
        query="";search.setValue("");rebuildEntries();
        for(var entry:entries)if(entry.id().equals(id.toString())){list.reveal(entry);break;}
    }
    @Override public void onClose(){
        if(enabled&&selected.isEmpty()){error="Choose a biome, or turn the filter off.";return;}
        conditions.biomeEnabled=enabled;conditions.biomes=new LinkedHashSet<>(selected);runtime.save();super.onClose();
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        String status=!error.isEmpty()?error:selected.size()+" selected";
        g.text(menuFont,fit(status,contentWidth),left,bodyBottom()-51,error.isEmpty()?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF):0xFFFFA0A0);
    }
}

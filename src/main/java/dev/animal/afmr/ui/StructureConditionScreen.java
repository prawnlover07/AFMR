package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.StructureContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Search is confined to editing; the catalog is an owned server observation. */
public final class StructureConditionScreen extends MusicScreen {
    private record Entry(String id,String label,String search) {}
    private final PlaylistConditions conditions;
    private final Set<String> selected;
    private Set<String> available=Set.of();
    private List<Entry> entries=List.of();
    private DenseList<Entry> list;
    private FlatButton toggle;
    private EditBox search,radius;
    private String query="",error="",radiusValue;
    private boolean enabled;

    StructureConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Structure condition",parent,runtime);this.conditions=conditions;
        selected=new LinkedHashSet<>(conditions.structures);enabled=conditions.structureEnabled;radiusValue=""+conditions.structureRadius;
    }
    @Override protected void init(){
        super.init();available=runtime.structureIds();
        int half=(contentWidth-6)/2;
        toggle=toggle("Filter",left,47,half,()->enabled,()->{enabled=!enabled;updateToggle();});
        var current=button("Add current structures",left+half+6,47,half,this::addCurrent);
        current.active=runtime.inWorld()&&minecraft.getSingleplayerServer()!=null;
        var addCurrentStructuresHelpText=Component.translatable("afmr.copy.structurecondition.add_current_structures");
        if(!addCurrentStructuresHelpText.getString().isBlank())current.setTooltip(Tooltip.create(addCurrentStructuresHelpText));
        list=addRenderableWidget(new DenseList<>(left,121,contentWidth,Math.max(14,bodyBottom()-193),
            (g,item,x,y,w,hover)->g.text(menuFont,fit((selected.contains(item.id())?"[x] ":"[ ] ")+item.label(),w-8),x+4,y+3,
                selected.contains(item.id())?MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF):hover?MenuAppearance.color(MenuColor.TEXT,0xFFFFFFFF):MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED)),
            (item,event,twice)->{if(!selected.remove(item.id())){selected.add(item.id());enabled=true;}error="";updateToggle();},Entry::label));
        search=edit(query,"Search structures or enter namespace:id",left,71,contentWidth-82,value->{query=value;filter();});
        var structureSearchHelpText=Component.translatable("afmr.copy.structurecondition.structure_search");
        if(!structureSearchHelpText.getString().isBlank())search.setTooltip(Tooltip.create(structureSearchHelpText));
        button("Add ID",left+contentWidth-76,71,76,()->add(search.getValue().strip()));
        radius=edit(radiusValue,"Radius (0-128)",left+160,95,Math.min(100,contentWidth-160),value->radiusValue=value);
        var structureRadiusHelpText=Component.translatable("afmr.copy.structurecondition.structure_radius");
        if(!structureRadiusHelpText.getString().isBlank())radius.setTooltip(Tooltip.create(structureRadiusHelpText));
        button("Clear / any",left,bodyBottom()-29,100,()->{selected.clear();enabled=false;error="";updateToggle();});
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
        rebuildEntries();
    }
    private void updateToggle(){}
    private void rebuildEntries(){
        var ids=new TreeSet<>(available);ids.addAll(selected);
        entries=ids.stream().map(id->{
            String label=id.substring(id.indexOf(':')+1).replace('_',' ')+"  |  "+id+(available.contains(id)?"":" (not in current world)");
            return new Entry(id,label,label.toLowerCase(Locale.ROOT));
        }).toList();filter();
    }
    private void filter(){
        String needle=query.strip().toLowerCase(Locale.ROOT);
        list.items(needle.isEmpty()?entries:entries.stream().filter(e->e.search().contains(needle)).toList());list.top();
    }
    private void add(String value){
        var id=Identifier.tryParse(value);
        if(id==null||value.isBlank()){error="Enter an ID such as minecraft:village_plains.";return;}
        selected.add(id.toString());enabled=true;error="";updateToggle();
        query="";search.setValue("");rebuildEntries();
        for(var entry:entries)if(entry.id().equals(id.toString())){list.reveal(entry);break;}
    }
    private void addCurrent(){
        var c=runtime.context();
        if(!c.structures().available()){error="Structure data unavailable. Use singleplayer or an AFMR server companion.";return;}
        boolean found=false;
        for(var box:c.structures().bounds())if(box.distanceSquared(c.x(),c.y(),c.z())==0){selected.add(box.id());found=true;}
        if(!found){error="You are not inside a loaded generated structure.";return;}
        enabled=true;error="";updateToggle();query="";search.setValue("");rebuildEntries();
    }
    @Override public void onClose(){
        if(enabled&&selected.isEmpty()){error="Choose a structure, or turn the filter off.";return;}
        double value=conditions.structureRadius;
        try{
            value=Double.parseDouble(radius.getValue());
            if(!Double.isFinite(value)||value<0||value>StructureContext.MAX_RADIUS)throw new IllegalArgumentException();
        }catch(IllegalArgumentException e){if(enabled){error="Use a radius from 0 to 128 blocks.";return;}value=conditions.structureRadius;}
        conditions.structureEnabled=enabled;conditions.structures=new LinkedHashSet<>(selected);conditions.structureRadius=value;
        runtime.save();super.onClose();
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        var catalog=runtime.structureIds();if(catalog!=available){available=catalog;rebuildEntries();}
        g.text(menuFont,"Radius outside bounds",left,101,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        String info=!runtime.inWorld()?"Join a world":!runtime.serverConditionsAvailable()?"Server companion required for structure data"
            :runtime.context().structures().available()?"Structure data ready":"Waiting for structure data";
        g.text(menuFont,fit(info,contentWidth),left,bodyBottom()-61,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF));
        g.text(menuFont,fit(error.isEmpty()?selected.size()+" selected":error,contentWidth),left,bodyBottom()-49,error.isEmpty()?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF):0xFFFFA0A0);
    }
}

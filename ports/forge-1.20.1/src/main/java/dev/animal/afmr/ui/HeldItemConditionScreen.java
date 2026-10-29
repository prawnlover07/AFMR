package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.Hand;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.*;

/** Item catalog and search strings are built only while editing. */
final class HeldItemConditionScreen extends MusicScreen {
    private record Entry(String id,String label,String search) {}
    private static final Hand[] HANDS=Hand.values();
    private final PlaylistConditions conditions;
    private final Set<String> selected;
    private final Map<String,Entry> catalog=new TreeMap<>();
    private List<Entry> entries=List.of();
    private DenseList<Entry> list;
    private FlatButton toggle,handButton;
    private EditBox search;
    private String query="",error="";
    private boolean enabled;
    private Hand hand;

    HeldItemConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Held item condition",parent,runtime);this.conditions=conditions;
        selected=new LinkedHashSet<>(conditions.heldItems);enabled=conditions.heldItemEnabled;hand=conditions.heldItemHand;
    }
    @Override protected void init(){
        super.init();
        if(catalog.isEmpty())for(var item:BuiltInRegistries.ITEM)if(item!=Items.AIR){
            String id=BuiltInRegistries.ITEM.getKey(item).toString();
            String label=item.getName(item.getDefaultInstance()).getString()+"  |  "+id;
            catalog.put(id,new Entry(id,label,label.toLowerCase(Locale.ROOT)));
        }
        int half=(contentWidth-6)/2;
        toggle=toggle("Filter",left,47,half,()->enabled,()->{enabled=!enabled;updateLabels();});
        button("Add held items",left+half+6,47,half,this::addHeld).active=runtime.inWorld();
        handButton=button("",left,71,contentWidth,()->{hand=HANDS[(hand.ordinal()+1)%HANDS.length];updateLabels();});
        var handSelectionHelpText=Component.translatable("afmr.copy.helditemcondition.hand_selection");
        if(!handSelectionHelpText.getString().isBlank())handButton.setTooltip(Tooltip.create(handSelectionHelpText));
        list=addRenderableWidget(new DenseList<>(left,121,contentWidth,Math.max(14,bodyBottom()-179),
            (g,item,x,y,w,hover)->g.drawString(menuFont,fit((selected.contains(item.id())?"[x] ":"[ ] ")+item.label(),w-8),x+4,y+3,
                selected.contains(item.id())?MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF):hover?MenuAppearance.color(MenuColor.TEXT,0xFFFFFFFF):MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED)),
            (item,clickX,clickY,button,twice)->{if(!selected.remove(item.id())){selected.add(item.id());enabled=true;}error="";updateLabels();},Entry::label));
        search=edit(query,"Search items or enter namespace:id",left,95,contentWidth-82,value->{query=value;filter();});
        button("Add ID",left+contentWidth-76,95,76,()->add(search.getValue().strip()));
        button("Clear / any",left,bodyBottom()-29,100,()->{selected.clear();enabled=false;error="";updateLabels();});
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
        updateLabels();rebuildEntries();
    }
    private void updateLabels(){
        handButton.setMessage(Component.literal("Hand: "+hand.label()));
    }
    private void rebuildEntries(){
        var items=new TreeMap<>(catalog);
        for(String id:selected)if(!items.containsKey(id)){
            String label=id+" (not registered)";items.put(id,new Entry(id,label,label.toLowerCase(Locale.ROOT)));
        }
        entries=List.copyOf(items.values());filter();
    }
    private void filter(){
        String needle=query.strip().toLowerCase(Locale.ROOT);
        list.items(needle.isEmpty()?entries:entries.stream().filter(e->e.search().contains(needle)).toList());list.top();
    }
    private void add(String value){
        var id=ResourceLocation.tryParse(value);
        if(id==null||value.isBlank()){error="Enter an ID such as minecraft:fishing_rod.";return;}
        if(id.toString().equals("minecraft:air")){error="Choose an item that can be held.";return;}
        selected.add(id.toString());enabled=true;error="";updateLabels();query="";search.setValue("");rebuildEntries();
        for(var entry:entries)if(entry.id().equals(id.toString())){list.reveal(entry);break;}
    }
    private void addHeld(){
        var c=runtime.context();boolean found=false;
        if(hand!=Hand.OFF_HAND&&!c.mainHandItem().isEmpty()){selected.add(c.mainHandItem());found=true;}
        if(hand!=Hand.MAIN_HAND&&!c.offHandItem().isEmpty()){selected.add(c.offHandItem());found=true;}
        if(!found){error="No item held in the chosen hand(s).";return;}
        enabled=true;error="";updateLabels();query="";search.setValue("");rebuildEntries();
    }
    @Override public void onClose(){
        if(enabled&&selected.isEmpty()){error="Choose an item, or turn the filter off.";return;}
        conditions.heldItemEnabled=enabled;conditions.heldItems=new LinkedHashSet<>(selected);conditions.heldItemHand=hand;
        runtime.save();super.onClose();
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        g.drawString(menuFont,fit(error.isEmpty()?selected.size()+" selected":error,contentWidth),left,bodyBottom()-49,error.isEmpty()?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF):0xFFFFA0A0);
    }
}

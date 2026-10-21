package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.PlaylistConditions.State;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

final class CustomConditionsScreen extends MusicScreen {
    private final Playlist playlist;
    private EditBox name;
    private String error="";
    private int page;
    CustomConditionsScreen(Screen parent,ClientRuntime runtime,Playlist playlist){super("Custom playlist conditions",parent,runtime);this.playlist=playlist;}
    @Override protected void init(){
        super.init();name=edit(playlist.name,"Playlist name",left,47,contentWidth,null);name.setMaxLength(80);
        var c=playlist.conditions;
        String[] labels={"Location: "+(c.areaEnabled?"within "+(int)c.area.radius+" blocks":"any"),"Time: "+(c.timeEnabled?c.startTime+" to "+c.endTime:"any"),
            "Dimension: "+(c.dimensionEnabled?c.dimension:"any"),"Elevation: "+(c.elevationEnabled?"Y "+c.minY+" to "+c.maxY:"any"),
            "Cover: "+state(c.cover,"covered","open sky"),"Biome temperature: "+(c.temperatureEnabled?c.minTemperature+" to "+c.maxTemperature:"any"),
            "Light: "+(c.lightEnabled?c.minLight+" to "+c.maxLight:"any"),"Combat: "+state(c.combat,"in combat","peaceful"),"Water: "+state(c.water,"underwater","out of water"),
            "Biomes: "+(c.biomeEnabled?c.biomes.size()+" selected":"any")};
        int size=Math.max(1,(height-155)/24);page=Math.clamp(page,0,(labels.length-1)/size);
        for(int i=page*size;i<Math.min(labels.length,(page+1)*size);i++){int kind=i;
            button(labels[i],left,82+(i-page*size)*24,contentWidth,()->open(kind));}
        pages(page,size,labels.length,height-53,next->{if(saveName()){page=next;rebuildWidgets();}});
        button("Save & back",left+contentWidth-104,height-29,104,this::onClose);
    }
    private static String state(State state,String yes,String no){return state==State.ANY?"any":state==State.YES?yes:no;}
    private boolean saveName(){
        String value=name.getValue().strip();if(value.isBlank()){error="Enter a playlist name.";return false;}
        playlist.name=value;runtime.save();error="";return true;
    }
    private void open(int kind){
        if(!saveName())return;
        Screen next=switch(kind){
            case 0->new AreaConditionScreen(this,runtime,playlist.conditions);
            case 1->new RangeConditionScreen(this,runtime,playlist.conditions,RangeConditionScreen.Kind.TIME);
            case 2->new DimensionConditionScreen(this,runtime,playlist.conditions);
            case 3->new RangeConditionScreen(this,runtime,playlist.conditions,RangeConditionScreen.Kind.ELEVATION);
            case 4->new CoverConditionScreen(this,runtime,playlist.conditions);
            case 5->new RangeConditionScreen(this,runtime,playlist.conditions,RangeConditionScreen.Kind.TEMPERATURE);
            case 6->new RangeConditionScreen(this,runtime,playlist.conditions,RangeConditionScreen.Kind.LIGHT);
            case 9->new BiomeConditionScreen(this,runtime,playlist.conditions);
            default->null;
        };
        if(next!=null)minecraft.gui.setScreen(next);
        else{var c=playlist.conditions;if(kind==7)c.combat=State.values()[(c.combat.ordinal()+1)%3];else c.water=State.values()[(c.water.ordinal()+1)%3];runtime.save();rebuildWidgets();}
    }
    @Override public void onClose(){if(saveName())super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit(error.isEmpty()?"All enabled filters must match. Unset filters allow any value.":error,contentWidth),left,height-78,error.isEmpty()?0xFF8FA7A0:0xFFFFA0A0);
    }
}

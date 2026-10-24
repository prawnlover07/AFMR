package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.State;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

final class PlaylistConditionsScreen extends MusicScreen {
    private final Playlist playlist;
    private final PlaylistConditions conditions;
    private final int depth;
    private EditBox name;
    private String error="";
    private int page;
    PlaylistConditionsScreen(Screen parent,ClientRuntime runtime,Playlist playlist,PlaylistConditions conditions,int depth){
        super(depth==0?"Playlist conditions":"Condition group",parent,runtime);this.playlist=playlist;this.conditions=conditions;this.depth=depth;
    }
    @Override protected void init(){
        super.init();name=edit(depth==0?playlist.name:conditions.label,depth==0?"Playlist name":"Group name",left,47,contentWidth,null);name.setMaxLength(80);
        var c=conditions;int segment=(contentWidth-8)/(depth==0?2:3);
        var matchAllOrAnyHelpControl=button("Conditions: match "+(c.matchAny?"any":"all"),left,74,segment,()->{
            if(saveName()){c.matchAny=!c.matchAny;runtime.save();rebuildWidgets();}
        });
        var matchAllOrAnyHelpText=Component.translatable("afmr.copy.playlistconditions.match_all_or_any");
        if(!matchAllOrAnyHelpText.getString().isBlank())matchAllOrAnyHelpControl.setTooltip(Tooltip.create(matchAllOrAnyHelpText));
        var notHelpControl=button("Not: "+(c.negated?"on":"off"),left+segment+4,74,segment,()->{
            if(saveName()){c.negated=!c.negated;runtime.save();rebuildWidgets();}
        });
        var notHelpText=Component.translatable("afmr.copy.playlistconditions.not");
        if(!notHelpText.getString().isBlank())notHelpControl.setTooltip(Tooltip.create(notHelpText));
        if(depth>0){
            var groupEnabledHelpControl=button("Group: "+(c.enabled?"on":"off"),left+2*(segment+4),74,segment,()->{
            if(saveName()){c.enabled=!c.enabled;runtime.save();rebuildWidgets();}
        });
            var groupEnabledHelpText=Component.translatable("afmr.copy.playlistconditions.group_enabled");
            if(!groupEnabledHelpText.getString().isBlank())groupEnabledHelpControl.setTooltip(Tooltip.create(groupEnabledHelpText));
        }
        String[] labels={"Location: "+(c.areaEnabled?"within "+(int)c.area.radius+" blocks":"any"),"Time: "+(c.timeEnabled?c.startTime+" to "+c.endTime:"any"),
            "Dimension: "+(c.dimensionEnabled?c.dimension:"any"),"Elevation: "+(c.elevationEnabled?"Y "+c.minY+" to "+c.maxY:"any"),
            "Cover: "+state(c.cover,"covered","open sky"),"Biome temperature: "+(c.temperatureEnabled?c.minTemperature+" to "+c.maxTemperature:"any"),
            "Light: "+(c.lightEnabled?c.minLight+" to "+c.maxLight:"any"),"Combat: "+state(c.combat,c.combatPlayersOnly?"PvP only":"in combat",c.combatPlayersOnly?"not in PvP":"peaceful"),"Water: "+state(c.water,"underwater","out of water"),
            "Biomes: "+(c.biomeEnabled?c.biomes.size()+" selected":"any"),
            "Structures (singleplayer only): "+(c.structureEnabled?c.structures.size()+" selected, radius "+c.structureRadius:"any"),
            "Weather: "+(c.weatherEnabled?c.weather.label():"filter off"),"Groups: "+c.groups.size(),
            "Health: "+(c.healthEnabled?(c.healthComparison==PlaylistConditions.HealthComparison.BELOW?"below ":"above ")+c.healthThreshold+" hearts":"any"),
            "Held items: "+(c.heldItemEnabled?c.heldItems.size()+" selected / "+c.heldItemHand.label():"any"),
            "Gamemode: "+(c.gameModeEnabled?c.gameModes.size()+" selected":"any")};
        int size=Math.max(1,(height-181)/24);page=Math.clamp(page,0,(labels.length-1)/size);
        for(int i=page*size;i<Math.min(labels.length,(page+1)*size);i++){int kind=i;
            var locationHelpControl=button(labels[i],left,108+(i-page*size)*24,contentWidth,()->open(kind));
            var locationHelpText=Component.translatable(switch(kind){
                case 0->"afmr.copy.playlistconditions.location";
                case 1->"afmr.copy.playlistconditions.time";
                case 2->"afmr.copy.playlistconditions.dimension";
                case 3->"afmr.copy.playlistconditions.elevation";
                case 4->"afmr.copy.playlistconditions.cover";
                case 5->"afmr.copy.playlistconditions.biome_temperature";
                case 6->"afmr.copy.playlistconditions.light";
                case 7->"afmr.copy.playlistconditions.combat";
                case 8->"afmr.copy.playlistconditions.water";
                case 9->"afmr.copy.playlistconditions.biomes";
                case 10->"afmr.copy.playlistconditions.structures";
                case 11->"afmr.copy.playlistconditions.weather";
                case 13->"afmr.copy.playlistconditions.health";
                case 14->"afmr.copy.playlistconditions.held_items";
                case 15->"afmr.copy.playlistconditions.gamemode";
                default->"afmr.copy.playlistconditions.nested_groups";
            });
            if(!locationHelpText.getString().isBlank())locationHelpControl.setTooltip(Tooltip.create(locationHelpText));}
        pages(page,size,labels.length,height-53,next->{if(saveName()){page=next;rebuildWidgets();}});
        button("Save & back",left+contentWidth-104,height-29,104,this::onClose);
    }
    private static String state(State state,String yes,String no){return state==State.ANY?"any":state==State.YES?yes:no;}
    private boolean saveName(){
        String value=name.getValue().strip();if(value.isBlank()){error=depth==0?"Enter a playlist name.":"Enter a group name.";return false;}
        if(depth==0)playlist.name=value;else conditions.label=value;runtime.save();error="";return true;
    }
    private void open(int kind){
        if(!saveName())return;
        Screen next=switch(kind){
            case 0->new AreaConditionScreen(this,runtime,conditions);
            case 1->new RangeConditionScreen(this,runtime,conditions,RangeConditionScreen.Kind.TIME);
            case 2->new DimensionConditionScreen(this,runtime,conditions);
            case 3->new RangeConditionScreen(this,runtime,conditions,RangeConditionScreen.Kind.ELEVATION);
            case 4->new CoverConditionScreen(this,runtime,conditions);
            case 5->new RangeConditionScreen(this,runtime,conditions,RangeConditionScreen.Kind.TEMPERATURE);
            case 6->new RangeConditionScreen(this,runtime,conditions,RangeConditionScreen.Kind.LIGHT);
            case 7->new CombatConditionScreen(this,runtime,conditions);
            case 9->new BiomeConditionScreen(this,runtime,conditions);
            case 10->new StructureConditionScreen(this,runtime,conditions);
            case 11->new WeatherConditionScreen(this,runtime,conditions);
            case 12->new ConditionGroupsScreen(this,runtime,playlist,conditions,depth);
            case 13->new HealthConditionScreen(this,runtime,conditions);
            case 14->new HeldItemConditionScreen(this,runtime,conditions);
            case 15->new GameModeConditionScreen(this,runtime,conditions);
            default->null;
        };
        if(next!=null)minecraft.gui.setScreen(next);
        else{var c=conditions;c.water=State.values()[(c.water.ordinal()+1)%3];runtime.save();rebuildWidgets();}
    }
    @Override public void onClose(){if(saveName())super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        if(!error.isEmpty())g.text(font,fit(error,contentWidth),left,height-78,0xFFFFA0A0);
    }
}

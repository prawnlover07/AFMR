package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.MenuColor;
import dev.animal.afmr.core.PlaylistConditions.State;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Owns the independently scrolling, inline playlist settings and their numeric drafts. */
final class PlaylistSettingsPanel {
    private static final int ROW=22,TOP=158;
    final Playlist playlist;
    private final LibraryScreen screen;
    private final ClientRuntime runtime;
    private final EnumMap<ConditionOption,State> remembered=new EnumMap<>(ConditionOption.class);
    private final List<AbstractWidget> controls=new ArrayList<>();
    private final List<Positioned> positioned=new ArrayList<>();
    private final List<Label> labels=new ArrayList<>();
    private final Map<String,String> drafts=new HashMap<>();
    private final Map<String,MenuField> fields=new HashMap<>();
    private record Positioned(AbstractWidget widget,int row){}
    private record Label(String text,int row){}
    private int x,w,row,scroll,rows;
    private float scale;
    private boolean more,typing,dirty;
    private long editedAt;
    private ConditionOption expanded;
    private AutoplayHereButton areaAction;
    private String error="";
    PlaylistSettingsPanel(LibraryScreen screen,ClientRuntime runtime,Playlist playlist){this.screen=screen;this.runtime=runtime;this.playlist=playlist;}
    List<AbstractWidget> build(WorkspaceLayout layout){
        controls.clear();positioned.clear();labels.clear();fields.clear();row=0;
        x=layout.rulesX()+12;w=layout.rulesWidth()-34;scale=layout.scale();rows=Math.max(1,(layout.footerY()-TOP-20)/ROW);
        var c=playlist.conditions;
        boolean active=c.negated||c.groups.stream().anyMatch(group->group.enabled);
        for(var option:ConditionOption.values())active|=option.enabled(c);
        // Either legacy automatic restriction can prevent selection. The one checkbox reports effective eligibility.
        var automatic=new ToggleButton("Play automatically",x,100,w,20,scale,()->playlist.enabled&&!playlist.manualOnly,()->{
            if(!commit())return;boolean enable=!(playlist.enabled&&!playlist.manualOnly);playlist.enabled=enable;playlist.manualOnly=false;save();
        });help(automatic,"afmr.copy.library.automatic_switch");controls.add(automatic);
        areaAction=new AutoplayHereButton(x,126,w,22,scale,()->{
            if(!commit())return;playlist.autoplayHere(runtime.context());drafts.clear();expanded=ConditionOption.AREA;save();
        });areaAction.active=runtime.inWorld();controls.add(areaAction);
        label("When it plays");
        if(active||more){button(c.matchAny?"Match any rule":"Match all rules",0,w,()->{c.matchAny=!c.matchAny;save();},"afmr.copy.library.match_all_or_any");row++;}
        for(var option:ConditionOption.values()){
            if(!more&&!option.enabled(c))continue;
            String text=option.label+(option.enabled(c)?": "+option.summary(c):"");
            var toggle=new ToggleButton(text,x,0,w-44,20,scale,()->option.enabled(playlist.conditions),()->{
                if(!commit())return;option.toggle(playlist.conditions,remembered);save();
            });help(toggle,option.help);add(toggle);
            button(expanded==option?"v":">",w-40,40,()->{
                if(option==ConditionOption.AREA||option==ConditionOption.TIME){expanded=expanded==option?null:option;drafts.clear();screen.rebuildSettings();}
                else screen.openSettingsScreen(ConditionEditors.create(option,screen,runtime,playlist.conditions));
            },option.help);row++;
            if(expanded==option){if(option==ConditionOption.AREA)areaFields();else if(option==ConditionOption.TIME)timeFields();}
        }
        button(more?"Fewer rules":active?"More rules":"Show rules",0,w,()->{more=!more;scroll=0;screen.rebuildSettings();},null);row++;
        if(active||more){check("Invert rules (NOT)",()->playlist.conditions.negated,()->playlist.conditions.negated=!playlist.conditions.negated,"afmr.copy.playlistconditions.not");
        button("Condition groups: "+c.groups.size()+" >",0,w,()->screen.openSettingsScreen(new ConditionGroupsScreen(screen,runtime,playlist,c,0)),"afmr.copy.playlistconditions.nested_groups");row++;}
        label("Playback");
        number("volume","Volume (%)",playlist.volume*100,0,w);row++;
        check("Custom gap between songs",()->playlist.customSilence,()->playlist.customSilence=!playlist.customSilence,"afmr.copy.library.playlist_silence");
        if(playlist.customSilence){
            number("min","Shortest gap (s)",playlist.silenceMinSeconds,0,w);row++;
            number("max","Longest gap (s)",playlist.silenceMaxSeconds,0,w);row++;
        }
        button("Songs before gap: "+(playlist.customSongCount?playlist.songsBeforeSilence:"global")+" >",0,w,
            ()->screen.openSettingsScreen(new SongCountScreen(screen,runtime,playlist)),null);row++;
        check("Include Minecraft music",()->playlist.includeVanilla,()->playlist.includeVanilla=!playlist.includeVanilla,"afmr.copy.library.minecraft_music");
        check("Let the previous song finish",()->playlist.finishCurrentSong,()->playlist.finishCurrentSong=!playlist.finishCurrentSong,"afmr.copy.library.finish_song_on_entry");
        check("Finish songs before switching away",()->playlist.finishCurrentSongOnExit,()->playlist.finishCurrentSongOnExit=!playlist.finishCurrentSongOnExit,"afmr.copy.library.finish_song_on_exit");
        check("Skip wait on automatic start",()->playlist.playOnTrigger,()->playlist.playOnTrigger=!playlist.playOnTrigger,"afmr.copy.library.play_on_trigger");
        button("Playlist switching >",0,w,()->screen.openSettingsScreen(new PlaylistTransitionsScreen(screen,runtime,playlist.id)),null);row++;
        button("Override shortcuts >",0,w,()->screen.openSettingsScreen(new OverridesScreen(screen,runtime,-1,playlist.id)),null);row++;
        scroll=Math.max(0,Math.min(scroll,Math.max(0,row-rows)));
        controls.add(new SettingsScrollbar(x+w+3,TOP,rows*ROW-2,Math.max(0,row-rows),rows,()->scroll,this::scrollTo));
        position();return controls;
    }
    private void label(String text){labels.add(new Label(text,row++));}
    private void add(AbstractWidget widget){controls.add(widget);positioned.add(new Positioned(widget,row));}
    private void help(AbstractWidget widget,String... keys){
        var text=new net.minecraft.network.chat.TextComponent("");
        for(String key:keys)if(key!=null){var entry=new net.minecraft.network.chat.TranslatableComponent(key);if(!entry.getString().isBlank()){if(!text.getString().isBlank())text.append("\n");text.append(entry);}}
        if(!text.getString().isBlank())((Described)widget).description(text);
    }
    private void button(String text,int dx,int width,Runnable action,String help){
        var button=new FlatButton(text,x+dx,0,width,20,scale,()->{if(commit())action.run();});help(button,help);add(button);
    }
    private void check(String text,BooleanSupplier checked,Runnable action,String help){
        var toggle=new ToggleButton(text,x,0,w,20,scale,checked,()->{if(commit()){action.run();save();}});help(toggle,help);add(toggle);row++;
    }
    private void number(String key,String label,double value,int dx,int width){
        labels.add(new Label(label,row));
        var box=new MenuField(MenuAppearance.font(),x+dx+width-66,0,64,18,new net.minecraft.network.chat.TextComponent(label));box.setMaxLength(32);
        box.setValue(drafts.getOrDefault(key,format(value)));box.setResponder(text->{runtime.playlistEdits().coalesce("inline:"+playlist.id+":"+key);drafts.put(key,text);editedAt=System.nanoTime();typing=true;commit();typing=false;});fields.put(key,box);add(box);
        switch(key){
            case "volume"->help(box,"afmr.copy.library.playlist_volume","afmr.copy.playlistvolume.live_volume_slider");
            case "min"->help(box,"afmr.copy.playlistsilence.minimum_silence","afmr.copy.playlistsilence.custom_silence_switch");
            case "max"->help(box,"afmr.copy.playlistsilence.maximum_silence");
        }
    }
    private static String format(double value){return value==Math.rint(value)?Long.toString((long)value):Double.toString(value);}
    private void areaFields(){
        var area=playlist.conditions.area;
        number("x","Center X",area.x,0,w);row++;number("y","Center Y",area.y,0,w);row++;
        number("z","Center Z",area.z,0,w);row++;number("radius","Radius (blocks)",area.radius,0,w);row++;
        button("Use current location",0,w,()->{
            var context=runtime.context();area.x=context.x();area.y=context.y();area.z=context.z();area.world=runtime.worldKey();area.dimension=context.dimension();area.configured=true;
            playlist.conditions.areaEnabled=true;drafts.clear();save();
        },null);controls.get(controls.size()-1).active=runtime.inWorld();row++;
        button("World / dimension >",0,w,()->screen.openSettingsScreen(new AreaConditionScreen(screen,runtime,playlist.conditions)),null);row++;
    }
    private void timeFields(){
        var c=playlist.conditions;
        number("start","Start tick",c.startTime,0,w);row++;number("end","End tick",c.endTime,0,w);row++;
        int third=(w-8)/3;
        button("Day",0,third,()->timeRange(0,11999),null);button("Night",third+4,third,()->timeRange(12000,23999),null);button("All day",2*(third+4),w-2*(third+4),()->timeRange(0,23999),null);row++;
        check("Fit songs to time window",()->c.fitSongsToTimeWindow,()->c.fitSongsToTimeWindow=!c.fitSongsToTimeWindow,"afmr.copy.rangecondition.fit_songs_to_time_window");
    }
    private void timeRange(int start,int end){var c=playlist.conditions;c.startTime=start;c.endTime=end;c.timeEnabled=true;drafts.clear();save();}
    private double value(String key,double fallback){
        String text=fields.containsKey(key)?fields.get(key).getValue():null;
        double value=text==null?fallback:Double.parseDouble(text.strip());if(!Double.isFinite(value))throw new IllegalArgumentException();return value;
    }
    /** Validate all visible drafts before publishing a coherent setting change. Invalid input stays editable. */
    boolean commit(){
        try{
            double volume=value("volume",playlist.volume*100),min=value("min",playlist.silenceMinSeconds),max=value("max",playlist.silenceMaxSeconds);
            if(volume<0||volume>100){error="Volume: 0–100%.";return false;}
            if(min<0||max<min||max>86400){error="Gap: 0–86400s; longest ≥ shortest.";return false;}
            var c=playlist.conditions;var area=c.area;
            double ax=value("x",area.x),ay=value("y",area.y),az=value("z",area.z),radius=value("radius",area.radius);
            if(radius<1||radius>100000){error="Radius: 1–100000 blocks.";return false;}
            double start=value("start",c.startTime),end=value("end",c.endTime);
            if(start!=Math.rint(start)||end!=Math.rint(end)||start<0||start>23999||end<0||end>23999){error="Time: whole ticks 0–23999.";return false;}
            boolean areaChanged=area.x!=ax||area.y!=ay||area.z!=az||area.radius!=radius;
            boolean changed=playlist.volume!=volume/100||playlist.silenceMinSeconds!=min||playlist.silenceMaxSeconds!=max||area.x!=ax||area.y!=ay||area.z!=az||area.radius!=radius||c.startTime!=(int)start||c.endTime!=(int)end;
            playlist.volume=volume/100;playlist.silenceMinSeconds=min;playlist.silenceMaxSeconds=max;
            area.x=ax;area.y=ay;area.z=az;area.radius=radius;c.startTime=(int)start;c.endTime=(int)end;
            if(changed){if(areaChanged&&fields.containsKey("x")&&!area.configured&&runtime.inWorld()){area.world=runtime.worldKey();area.dimension=runtime.context().dimension();area.configured=true;}dirty=true;}
            if(dirty&&!typing)flush();
            error="";return true;
        }catch(IllegalArgumentException ex){error="Enter valid numbers.";return false;}
    }
    private void save(){runtime.save();screen.rebuildSettings();}
    private void position(){
        for(var p:positioned){var widget=p.widget;widget.y=TOP+(p.row-scroll)*ROW+1;widget.visible=p.row>=scroll&&p.row<scroll+rows;
            if(!widget.visible&&widget.isFocused()){if(widget.isFocused())widget.changeFocus(true);screen.setFocused(null);}
        }
    }
    void reveal(String text){
        if(!commit())return;more=true;screen.rebuildSettings();
        for(var item:positioned)if(item.widget.getMessage().getString().startsWith(text)){scrollTo(item.row);screen.setFocused(item.widget);if(!item.widget.isFocused())item.widget.changeFocus(true);break;}
        if(text.startsWith("Set playback area")){scrollTo(0);screen.setFocused(areaAction);if(!areaAction.isFocused())areaAction.changeFocus(true);}
    }
    void scrollTo(int target){scroll=Math.max(0,Math.min(target,Math.max(0,row-rows)));position();}
    boolean scroll(double mx,double my,double dy){
        if(mx<x||mx>x+w+12||my<TOP||my>=TOP+rows*ROW||dy==0)return false;
        scrollTo(scroll-(int)Math.copySign(Math.max(1,Math.ceil(Math.abs(dy)*3)),dy));return true;
    }
    void draw(PoseStack g){
        var font=MenuAppearance.font();
        for(var label:labels)if(label.row>=scroll&&label.row<scroll+rows){
            int width=label.text.equals("Playback")||label.text.equals("When it plays")?w:w-72;
            font.draw(g,font.plainSubstrByWidth(label.text,width),x,TOP+(label.row-scroll)*ROW+7,MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3));
        }
        if(!error.isBlank())font.draw(g,font.plainSubstrByWidth(error,w),x,TOP+rows*ROW+3,0xFFFF7777);
    }
    void flush(){if(dirty){runtime.save();dirty=false;}}
    void tick(){if(dirty&&System.nanoTime()-editedAt>250_000_000L)flush();if(areaAction!=null)areaAction.tick();}
    boolean invalidDraft(){return !error.isBlank();}
    void clearDrafts(){drafts.clear();}
    void cancelHold(){if(areaAction!=null)areaAction.cancelHold();}
}

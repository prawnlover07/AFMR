package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.State;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import java.util.EnumMap;

final class PlaylistConditionsScreen extends MusicScreen {
    private final Playlist playlist;
    private final PlaylistConditions conditions;
    private final int depth;
    private final EnumMap<ConditionOption,State> remembered=new EnumMap<>(ConditionOption.class);
    private MenuField name;
    private String error="";
    private int page;
    PlaylistConditionsScreen(Screen parent,ClientRuntime runtime,Playlist playlist,PlaylistConditions conditions,int depth){
        super(depth==0?"When it plays":"Condition group",parent,runtime);this.playlist=playlist;this.conditions=conditions;this.depth=depth;
    }
    @Override protected void init(){
        super.init();name=null;
        if(depth>0){name=edit(conditions.label,"Group name",left,47,contentWidth,null);name.setMaxLength(80);}
        int segment=(contentWidth-8)/(depth==0?2:3);
        var match=button("Match "+(conditions.matchAny?"any rule":"all rules"),left,74,segment,()->{
            if(saveName()){conditions.matchAny=!conditions.matchAny;runtime.save();rebuildWidgets();}
        });help(match,"afmr.copy.playlistconditions.match_all_or_any");
        var not=addRenderableWidget(new ToggleButton("Invert (NOT)",left+segment+4,74,segment,20,1,()->conditions.negated,()->{
            if(saveName()){conditions.negated=!conditions.negated;runtime.save();}
        }));help(not,"afmr.copy.playlistconditions.not");
        if(depth>0){
            var enabled=addRenderableWidget(new ToggleButton("Group enabled",left+2*(segment+4),74,segment,20,1,()->conditions.enabled,()->{
                if(saveName()){conditions.enabled=!conditions.enabled;runtime.save();}
            }));help(enabled,"afmr.copy.playlistconditions.group_enabled");
        }
        var options=ConditionOption.values();int size=Math.max(1,(bodyBottom()-181)/24),total=options.length+1;
        page=Math.max(0,Math.min(page,(total-1)/size));
        for(int i=page*size;i<Math.min(total,(page+1)*size);i++){
            int y=108+(i-page*size)*24;
            if(i==options.length){help(button("Condition groups: "+conditions.groups.size()+" >",left,y,contentWidth,()->{
                if(saveName())minecraft.setScreen(new ConditionGroupsScreen(this,runtime,playlist,conditions,depth));
            }),"afmr.copy.playlistconditions.nested_groups");continue;}
            var option=options[i];String label=option.label+(option.enabled(conditions)?": "+option.summary(conditions):"");
            var toggle=addRenderableWidget(new ToggleButton(label,left,y,contentWidth-48,20,1,()->option.enabled(conditions),()->{
                if(saveName()){option.toggle(conditions,remembered);runtime.save();rebuildWidgets();}
            }));help(toggle,option.help);
            help(button(">",left+contentWidth-44,y,44,()->{
                if(saveName())minecraft.setScreen(ConditionEditors.create(option,this,runtime,conditions));
            }),option.help);
        }
        pages(page,size,total,bodyBottom()-53,next->{if(saveName()){page=next;rebuildWidgets();}});
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    private static void help(FlatButton control,String key){var text=Component.translatable(key);if(!text.getString().isBlank())control.description(text);}
    private boolean saveName(){
        if(name!=null){String value=name.getValue().strip();if(value.isBlank()){error="Enter a group name.";return false;}conditions.label=value;}
        runtime.save();error="";return true;
    }
    @Override public void onClose(){if(saveName())super.onClose();}
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){if(!error.isEmpty())menuFont.drawShadow(g,fit(error,contentWidth),left,bodyBottom()-78,0xFFFFA0A0);}
}

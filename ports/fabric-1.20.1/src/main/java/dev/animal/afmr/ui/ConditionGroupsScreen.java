package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.PlaylistConditions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class ConditionGroupsScreen extends MusicScreen {
    private final Playlist playlist;
    private final PlaylistConditions conditions;
    private final int depth;
    private PlaylistConditions selected;
    private FlatButton edit,remove;
    private record Row(PlaylistConditions group,String text){}
    ConditionGroupsScreen(Screen parent,ClientRuntime runtime,Playlist playlist,PlaylistConditions conditions,int depth){
        super("Condition groups",parent,runtime);this.playlist=playlist;this.conditions=conditions;this.depth=depth;
    }
    @Override protected void init(){
        super.init();
        var add=button("Add group",left,47,contentWidth,()->{
            var group=new PlaylistConditions();group.label="Group "+(conditions.groups.size()+1);
            conditions.groups.add(group);selected=group;runtime.save();open();
        });
        add.active=depth<PlaylistConditions.MAX_DEPTH&&conditions.groups.size()<PlaylistConditions.MAX_GROUPS&&playlist.conditions.nodeCount()<PlaylistConditions.MAX_NODES;
        var addGroupHelpText=Component.translatable("afmr.copy.conditiongroups.add_group");
        if(!addGroupHelpText.getString().isBlank())add.setTooltip(Tooltip.create(addGroupHelpText));
        var list=addRenderableWidget(new DenseList<Row>(left,75,contentWidth,Math.max(14,height-138),(g,row,x,y,w,hover)->{
            if(row.group==selected||hover)g.fill(x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF35594E));
            g.drawString(font,fit(row.text,w-8),x+4,y+3,row.group.enabled?MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0):MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF7D9389));
        },(row,clickX,clickY,button,twice)->{selected=row.group;edit.active=remove.active=true;if(twice)open();},Row::text));
        list.items(conditions.groups.stream().map(c->new Row(c,(c.label.isBlank()?"Group":c.label)+" / "+c.summary())).toList());
        int third=(contentWidth-8)/3;
        edit=button("Edit",left,height-29,third,this::open);
        remove=button("Remove",left+third+4,height-29,third,()->{conditions.groups.remove(selected);selected=null;runtime.save();rebuildWidgets();});
        edit.active=remove.active=selected!=null&&conditions.groups.contains(selected);
        button("Back",left+2*(third+4),height-29,third,this::onClose);
    }
    private void open(){if(selected!=null)minecraft.setScreen(new PlaylistConditionsScreen(this,runtime,playlist,selected,depth+1));}
}

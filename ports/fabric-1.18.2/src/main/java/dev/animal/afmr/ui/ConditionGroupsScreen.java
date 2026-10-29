package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.PlaylistConditions;
import net.minecraft.client.gui.screens.Screen;
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
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();
        var add=button("Add group",left,47,contentWidth,()->{
            var group=new PlaylistConditions();group.label="Group "+(conditions.groups.size()+1);
            conditions.groups.add(group);selected=group;runtime.save();open();
        });
        add.active=depth<PlaylistConditions.MAX_DEPTH&&conditions.groups.size()<PlaylistConditions.MAX_GROUPS&&playlist.conditions.nodeCount()<PlaylistConditions.MAX_NODES;
        var addGroupHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.conditiongroups.add_group");
        if(!addGroupHelpText.getString().isBlank())add.description(addGroupHelpText);
        var list=addRenderableWidget(new DenseList<Row>(left,75,contentWidth,Math.max(14,bodyBottom()-138),(g,row,x,y,w,hover)->{
            if(row.group==selected||hover)net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
            menuFont.drawShadow(g,fit(row.text,w-8),x+4,y+3,row.group.enabled?MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED):MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF7F8B98));
        },(row,clickX,clickY,button,twice)->{selected=row.group;edit.active=remove.active=true;if(twice)open();},Row::text));
        list.items(conditions.groups.stream().map(c->new Row(c,(c.label.isBlank()?"Group":c.label)+" / "+c.summary())).toList());
        int third=(contentWidth-8)/3;
        edit=button("Edit",left,bodyBottom()-29,third,this::open);
        remove=button("Remove",left+third+4,bodyBottom()-29,third,()->{conditions.groups.remove(selected);selected=null;runtime.save();init(minecraft,width,height);});
        edit.active=remove.active=selected!=null&&conditions.groups.contains(selected);
        button("Back",left+2*(third+4),bodyBottom()-29,third,this::onClose);
    }
    private void open(){if(selected!=null)minecraft.setScreen(new PlaylistConditionsScreen(this,runtime,playlist,selected,depth+1));}
}

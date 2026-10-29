package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class DimensionConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private boolean enabled;
    private FlatButton toggle;
    private MenuField dimension;
    private String error="";
    DimensionConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){super("Dimension condition",parent,runtime);this.conditions=conditions;}
    @Override protected void init(){
        super.init();enabled=conditions.dimensionEnabled;
        toggle=toggle("Filter",left,47,contentWidth,()->enabled,()->{enabled=!enabled;});
        dimension=edit(conditions.dimension,"Dimension ID",left,91,contentWidth,null);
        int third=(contentWidth-8)/3;
        button("Overworld",left,123,third,()->choose("minecraft:overworld"));button("Nether",left+third+4,123,third,()->choose("minecraft:the_nether"));button("End",left+2*(third+4),123,third,()->choose("minecraft:the_end"));
        button("Use current dimension",left,151,contentWidth,()->choose(runtime.context().dimension())).active=runtime.inWorld();
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    private void choose(String id){dimension.setValue(id);enabled=true;}
    @Override public void onClose(){
        try{if(enabled){var id=ResourceLocation.tryParse(dimension.getValue().strip());if(id==null)throw new IllegalArgumentException();conditions.dimension=id.toString();}conditions.dimensionEnabled=enabled;runtime.save();super.onClose();}
        catch(IllegalArgumentException e){error="Use a dimension ID such as minecraft:overworld.";}
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        menuFont.drawShadow(g,"Dimension ID (modded dimensions are supported)",left,77,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        menuFont.drawShadow(g,fit(error,contentWidth),left,bodyBottom()-48,0xFFFFA0A0);
    }
}

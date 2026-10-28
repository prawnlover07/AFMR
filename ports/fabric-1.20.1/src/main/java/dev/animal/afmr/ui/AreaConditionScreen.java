package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class AreaConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private EditBox x,y,z,radius,dimension;
    private boolean enabled,configured;
    private String world,error="";
    private FlatButton toggle;
    AreaConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){super("Location condition",parent,runtime);this.conditions=conditions;}
    @Override protected void init(){
        super.init();var area=conditions.area;enabled=conditions.areaEnabled;configured=area.configured;world=area.world;
        toggle=toggle("Filter",left,47,contentWidth,()->enabled,()->{enabled=!enabled;});
        int third=(contentWidth-8)/3;
        x=edit(""+area.x,"Center X",left,87,third,null);y=edit(""+area.y,"Center Y",left+third+4,87,third,null);z=edit(""+area.z,"Center Z",left+2*(third+4),87,third,null);
        radius=edit(""+area.radius,"Radius",left,129,100,null);dimension=edit(area.dimension,"Dimension",left+108,129,contentWidth-108,null);
        button("Use current location",left,158,contentWidth,()->{
            var c=runtime.context();x.setValue(""+c.x());y.setValue(""+c.y());z.setValue(""+c.z());dimension.setValue(c.dimension());
            world=runtime.worldKey();configured=true;enabled=true;
        }).active=runtime.inWorld();
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override public void onClose(){
        try{
            if(enabled){
                double nx=Double.parseDouble(x.getValue()),ny=Double.parseDouble(y.getValue()),nz=Double.parseDouble(z.getValue()),nr=Double.parseDouble(radius.getValue());
                if(!Double.isFinite(nx)||!Double.isFinite(ny)||!Double.isFinite(nz)||!Double.isFinite(nr)||nr<1||nr>100000)throw new IllegalArgumentException();
                var id=ResourceLocation.tryParse(dimension.getValue().strip());if(id==null)throw new IllegalArgumentException();String dim=id.toString();
                if(!configured&&runtime.inWorld()){world=runtime.worldKey();configured=true;}
                if(!configured){error="Join a world and capture or enter your location first.";return;}
                var area=conditions.area;area.x=nx;area.y=ny;area.z=nz;area.radius=nr;area.dimension=dim;area.world=world;area.configured=true;
            }
            conditions.areaEnabled=enabled;runtime.save();super.onClose();
        }catch(IllegalArgumentException e){error="Use finite coordinates, a dimension ID, and radius 1-100000.";}
    }
    @Override protected void drawBody(GuiGraphics g,int mx,int my,float delta){
        int third=(contentWidth-8)/3;g.drawString(menuFont,"Center X",left,75,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));g.drawString(menuFont,"Y",left+third+4,75,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));g.drawString(menuFont,"Z",left+2*(third+4),75,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        g.drawString(menuFont,"Radius",left,117,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));g.drawString(menuFont,"Dimension",left+108,117,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        if(!error.isEmpty())g.drawString(menuFont,fit(error,contentWidth),left,bodyBottom()-48,0xFFFFA0A0);
    }
}

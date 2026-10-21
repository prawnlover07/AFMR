package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

final class HomeScreen extends MusicScreen {
    private EditBox x,y,z,radius,dimension;
    private String error="";
    HomeScreen(Screen parent,ClientRuntime runtime){super("Home area",parent,runtime);}
    @Override protected void init(){
        super.init();var home=runtime.library().home;
        int third=(contentWidth-8)/3;
        x=edit(Double.toString(home.x),"X",left,69,third,null);
        y=edit(Double.toString(home.y),"Y",left+third+4,69,third,null);
        z=edit(Double.toString(home.z),"Z",left+2*(third+4),69,third,null);
        radius=edit(Double.toString(home.radius),"Radius in blocks",left,111,100,null);
        dimension=edit(home.dimension,"Dimension",left+108,111,contentWidth-108,null);
        button("Use current location",left,143,contentWidth,()->{
            try{double value=Double.parseDouble(radius.getValue());if(Double.isFinite(value)&&value>=1&&value<=100000)runtime.library().home.radius=value;}catch(NumberFormatException ignored){}
            runtime.captureHome();rebuildWidgets();
        }).active=runtime.inWorld();
        button("Save & back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override public void onClose(){
        try{
            double nx=Double.parseDouble(x.getValue()),ny=Double.parseDouble(y.getValue()),nz=Double.parseDouble(z.getValue()),nr=Double.parseDouble(radius.getValue());
            if(!Double.isFinite(nx)||!Double.isFinite(ny)||!Double.isFinite(nz)||!Double.isFinite(nr)||nr<1||nr>100000)throw new IllegalArgumentException();
            String dim=net.minecraft.resources.Identifier.parse(dimension.getValue()).toString();
            var home=runtime.library().home;home.x=nx;home.y=ny;home.z=nz;home.radius=nr;home.dimension=dim;
            if(runtime.inWorld()){home.world=runtime.worldKey();home.configured=true;}
            runtime.save();super.onClose();
        }catch(Exception e){error="Use finite coordinates, a valid dimension, and radius 1-100000.";}
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int mx,int my,float delta){
        int third=(contentWidth-8)/3;
        g.text(font,"Center X",left,54,0xFFABC3B9);g.text(font,"Y",left+third+4,54,0xFFABC3B9);g.text(font,"Z",left+2*(third+4),54,0xFFABC3B9);
        g.text(font,"Radius (blocks)",left,97,0xFFABC3B9);g.text(font,"Dimension",left+108,97,0xFFABC3B9);
        String hint=error.isBlank()?(runtime.library().home.configured?"Home is scoped to the captured world and dimension.":"Join a world, then capture your current position."):error;
        g.text(font,fit(hint,contentWidth),left,height-49,error.isBlank()?0xFF8DE0BE:0xFFFFA0A0);
    }
}

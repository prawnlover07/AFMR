package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

final class UndergroundSettingsScreen extends MusicScreen {
    private EditBox maximumY,minimumCover;
    private String error="";
    UndergroundSettingsScreen(Screen parent,ClientRuntime runtime){super("Underground conditions",parent,runtime);}
    @Override protected void init(){
        super.init();var s=runtime.library().settings;
        maximumY=edit(Integer.toString(s.undergroundMaxY),"Maximum Y",left+contentWidth-100,47,100,null);
        maximumY.setMaxLength(12);
        button("Use current Y",left,75,contentWidth,()->maximumY.setValue(Integer.toString(runtime.currentBlockY()))).active=runtime.inWorld();
        minimumCover=edit(Double.toString(s.undergroundDepth),"Minimum cover",left+contentWidth-100,106,100,null);
        button("Save & back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override public void onClose(){
        try{
            int y=Integer.parseInt(maximumY.getValue().strip());
            double cover=Double.parseDouble(minimumCover.getValue().strip());
            if(!Double.isFinite(cover)||cover<1||cover>64)throw new NumberFormatException();
            var s=runtime.library().settings;s.undergroundMaxY=y;s.undergroundDepth=cover;
            runtime.save();super.onClose();
        }catch(NumberFormatException e){error="Y: signed integer. Minimum cover: 1-64 blocks.";}
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit("At or below Y (inclusive)",contentWidth-108),left,53,0xFFD9E8E0);
        g.text(font,fit("Minimum cover (blocks)",contentWidth-108),left,112,0xFFD9E8E0);
        g.text(font,fit("Both the Y cutoff and solid cover are required.",contentWidth),left,140,0xFF8FA7A0);
        g.text(font,fit("Dark exploration ignores this Y cutoff.",contentWidth),left,154,0xFF8FA7A0);
        String hint=error.isBlank()?(runtime.inWorld()?"Current player block Y: "+runtime.currentBlockY():"Join a world to capture your Y."):error;
        g.text(font,fit(hint,contentWidth),left,height-48,error.isBlank()?0xFF8DE0BE:0xFFFFA0A0);
    }
}

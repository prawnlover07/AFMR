package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

final class DarknessSettingsScreen extends MusicScreen {
    private EditBox maximumLight;
    private String error="";
    DarknessSettingsScreen(Screen parent,ClientRuntime runtime){super("Dark exploration conditions",parent,runtime);}
    @Override protected void init(){
        super.init();maximumLight=edit(Integer.toString(runtime.library().settings.darkMaxLight),"Maximum light",left+contentWidth-80,47,80,null);
        maximumLight.setMaxLength(2);
        var s=runtime.library().settings;int half=(contentWidth-6)/2;
        button("Nether: "+(s.darkInNether?"on":"off"),left,75,half,()->{
            if(apply()){s.darkInNether=!s.darkInNether;runtime.save();rebuildWidgets();}
        });
        button("End: "+(s.darkInEnd?"on":"off"),left+half+6,75,half,()->{
            if(apply()){s.darkInEnd=!s.darkInEnd;runtime.save();rebuildWidgets();}
        });
        button("Save & back",left+contentWidth-104,height-29,104,this::onClose);
    }
    private boolean apply(){
        try{
            int level=Integer.parseInt(maximumLight.getValue().strip());if(level<0||level>15)throw new NumberFormatException();
            runtime.library().settings.darkMaxLight=level;error="";return true;
        }catch(NumberFormatException e){error="Enter an integer light level from 0 to 15.";return false;}
    }
    @Override public void onClose(){if(apply()){runtime.save();super.onClose();}}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit("At or below light level",contentWidth-88),left,53,0xFFD9E8E0);
        g.text(font,fit("Nether/End on: plays anywhere there, ignoring light and cover.",contentWidth),left,105,0xFF8FA7A0);
        g.text(font,fit("Nether/End off: Dark exploration is disabled there.",contentWidth),left,119,0xFF8FA7A0);
        g.text(font,fit("Elsewhere: requires solid cover and light at or below the limit.",contentWidth),left,133,0xFF8FA7A0);
        g.text(font,fit("Open-sky nighttime stays outside. Y and video brightness do not apply.",contentWidth),left,147,0xFF8FA7A0);
        var c=runtime.context();String hint=error.isBlank()?(runtime.inWorld()?"Current light: "+c.lightLevel()+" / 15 | "+(c.covered()?"under solid cover":"uncovered"):"Join a world to see the current light level."):error;
        g.text(font,fit(hint,contentWidth),left,height-48,error.isBlank()?0xFF8DE0BE:0xFFFFA0A0);
    }
}

package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

final class RangeConditionScreen extends MusicScreen {
    enum Kind { TIME, ELEVATION, TEMPERATURE, LIGHT }
    private final PlaylistConditions conditions;
    private final Kind kind;
    private boolean enabled,fitSongs;
    private MenuField minimum,maximum;
    private FlatButton toggle,fitToggle;
    private String error="";
    RangeConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions,Kind kind){
        super(switch(kind){case TIME->"Time condition";case ELEVATION->"Elevation condition";case TEMPERATURE->"Biome temperature condition";case LIGHT->"Light condition";},parent,runtime);
        this.conditions=conditions;this.kind=kind;fitSongs=conditions.fitSongsToTimeWindow;
        enabled=switch(kind){case TIME->conditions.timeEnabled;case ELEVATION->conditions.elevationEnabled;case TEMPERATURE->conditions.temperatureEnabled;case LIGHT->conditions.lightEnabled;};
    }
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();var c=conditions;int half=(contentWidth-6)/2;
        toggle=toggle("Filter",left,47,contentWidth,()->enabled,()->{enabled=!enabled;});
        String min=switch(kind){case TIME->""+c.startTime;case ELEVATION->""+c.minY;case TEMPERATURE->""+c.minTemperature;case LIGHT->""+c.minLight;};
        String max=switch(kind){case TIME->""+c.endTime;case ELEVATION->""+c.maxY;case TEMPERATURE->""+c.maxTemperature;case LIGHT->""+c.maxLight;};
        minimum=edit(min,"Minimum",left,90,half,null);maximum=edit(max,"Maximum",left+half+6,90,half,null);
        var timeRangeHelpText=new net.minecraft.network.chat.TranslatableComponent(switch(kind){
            case TIME->"afmr.copy.rangecondition.time_range";
            case ELEVATION->"afmr.copy.rangecondition.elevation_range";
            case TEMPERATURE->"afmr.copy.rangecondition.temperature_range";
            case LIGHT->"afmr.copy.rangecondition.light_range";
        });
        var help=timeRangeHelpText.getString().isBlank()?null:timeRangeHelpText;
        minimum.description(help);maximum.description(help);
        switch(kind){
            case TIME->{int third=(contentWidth-8)/3;button("Day",left,120,third,()->range("0","11999"));button("Night",left+third+4,120,third,()->range("12000","23999"));button("All day",left+2*(third+4),120,third,()->range("0","23999"));}
            case ELEVATION->{button("Current Y as min",left,120,half,()->minimum.setValue(""+runtime.currentBlockY())).active=runtime.inWorld();button("Current Y as max",left+half+6,120,half,()->maximum.setValue(""+runtime.currentBlockY())).active=runtime.inWorld();}
            case TEMPERATURE->{button("Cold (up to 0.15)",left,120,half,()->range("-100","0.15"));button("Warm (1 or higher)",left+half+6,120,half,()->range("1","100"));}
            case LIGHT->{button("Dark (0-4)",left,120,half,()->range("0","4"));button("Bright (8-15)",left+half+6,120,half,()->range("8","15"));}
        }
        if(kind==Kind.TIME){
            fitToggle=toggle("Fit songs to time window",left,145,contentWidth,()->fitSongs,()->{
                fitSongs=!fitSongs;
            });
            var fitSongsToTimeWindowHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.rangecondition.fit_songs_to_time_window");
            if(!fitSongsToTimeWindowHelpText.getString().isBlank())fitToggle.description(fitSongsToTimeWindowHelpText);
        }
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    private void range(String min,String max){minimum.setValue(min);maximum.setValue(max);enabled=true;}
    @Override public void onClose(){
        try{
            var c=conditions;
            if(enabled){
                if(kind==Kind.TEMPERATURE){double min=Double.parseDouble(minimum.getValue()),max=Double.parseDouble(maximum.getValue());
                    if(!Double.isFinite(min)||!Double.isFinite(max)||min>max)throw new NumberFormatException();c.minTemperature=min;c.maxTemperature=max;
                }else{int min=Integer.parseInt(minimum.getValue().strip()),max=Integer.parseInt(maximum.getValue().strip());
                    if(kind==Kind.TIME){if(min<0||min>23999||max<0||max>23999)throw new NumberFormatException();c.startTime=min;c.endTime=max;}
                    else if(kind==Kind.LIGHT){if(min<0||max>15||min>max)throw new NumberFormatException();c.minLight=min;c.maxLight=max;}
                    else{if(min>max)throw new NumberFormatException();c.minY=min;c.maxY=max;}
                }
            }
            switch(kind){case TIME->{c.timeEnabled=enabled;c.fitSongsToTimeWindow=fitSongs;}case ELEVATION->c.elevationEnabled=enabled;case TEMPERATURE->c.temperatureEnabled=enabled;case LIGHT->c.lightEnabled=enabled;}
            runtime.save();super.onClose();
        }catch(NumberFormatException e){error=kind==Kind.TIME?"Use clock ticks 0-23999. Start > end crosses midnight.":kind==Kind.LIGHT?"Use light levels 0-15, minimum <= maximum.":"Enter valid numbers with minimum <= maximum.";}
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        int half=(contentWidth-6)/2;
        menuFont.drawShadow(g,kind==Kind.TIME?"Start tick (inclusive)":"Minimum (inclusive)",left,76,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        menuFont.drawShadow(g,kind==Kind.TIME?"End tick (inclusive)":"Maximum (inclusive)",left+half+6,76,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        var c=runtime.context();String current=runtime.inWorld()?switch(kind){case TIME->"Current tick: "+c.timeOfDay();case ELEVATION->"Current block Y: "+runtime.currentBlockY();case TEMPERATURE->String.format(Locale.ROOT,"Current biome: %s / %.3f",c.biome(),c.biomeTemperature());case LIGHT->"Current local light: "+c.lightLevel();}:"Not in world";
        menuFont.drawShadow(g,fit(current,contentWidth),left,170,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        menuFont.drawShadow(g,fit(error,contentWidth),left,bodyBottom()-48,0xFFFFA0A0);
    }
}

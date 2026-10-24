package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.Weather;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.EnumMap;

final class WeatherConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private final EnumMap<Weather,FlatButton> choices=new EnumMap<>(Weather.class);
    private boolean enabled;
    private Weather selected;
    private FlatButton toggle;

    WeatherConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Weather condition",parent,runtime);this.conditions=conditions;
        enabled=conditions.weatherEnabled;selected=conditions.weather;
    }
    @Override protected void init(){
        super.init();choices.clear();
        toggle=button("",left,47,contentWidth,()->{enabled=!enabled;updateLabels();});
        choice(Weather.ANY_WEATHER,left,71,contentWidth,Component.translatable("afmr.copy.weathercondition.any_weather"));
        int half=(contentWidth-6)/2;
        choice(Weather.RAIN,left,95,half,Component.translatable("afmr.copy.weathercondition.rain"));
        choice(Weather.SNOW,left+half+6,95,half,Component.translatable("afmr.copy.weathercondition.snow"));
        choice(Weather.THUNDERSTORM,left,119,half,Component.translatable("afmr.copy.weathercondition.thunderstorm"));
        choice(Weather.CLEAR,left+half+6,119,half,Component.translatable("afmr.copy.weathercondition.clear"));
        button("Save & back",left+contentWidth-104,height-29,104,this::onClose);
        updateLabels();
    }
    private void choice(Weather weather,int x,int y,int width,Component help){
        var b=button("",x,y,width,()->{selected=weather;enabled=true;updateLabels();});
        var choiceHelpText=help;
        if(!choiceHelpText.getString().isBlank())b.setTooltip(Tooltip.create(choiceHelpText));choices.put(weather,b);
    }
    private void updateLabels(){
        toggle.setMessage(Component.literal("Filter: "+(enabled?"on":"off")));
        for(var entry:choices.entrySet())entry.getValue().setMessage(Component.literal((entry.getKey()==selected?"[x] ":"[ ] ")+entry.getKey().label()));
    }
    @Override public void onClose(){
        conditions.weatherEnabled=enabled;conditions.weather=selected;runtime.save();super.onClose();
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        String status="";
        if(runtime.inWorld()){
            var c=runtime.context();
            status="Rain/snow weather: "+c.raining()+"; thunder: "+c.thundering()+"; local type: "+c.precipitation().name().toLowerCase(java.util.Locale.ROOT);
        }
        g.drawString(font,fit(status,contentWidth),left,height-49,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF8FA7A0));
    }
}

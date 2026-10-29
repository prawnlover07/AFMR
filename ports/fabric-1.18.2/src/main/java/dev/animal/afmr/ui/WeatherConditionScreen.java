package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.Weather;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
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
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();choices.clear();
        toggle=toggle("Filter",left,47,contentWidth,()->enabled,()->{enabled=!enabled;updateLabels();});
        choice(Weather.ANY_WEATHER,left,71,contentWidth,new net.minecraft.network.chat.TranslatableComponent("afmr.copy.weathercondition.any_weather"));
        int half=(contentWidth-6)/2;
        choice(Weather.RAIN,left,95,half,new net.minecraft.network.chat.TranslatableComponent("afmr.copy.weathercondition.rain"));
        choice(Weather.SNOW,left+half+6,95,half,new net.minecraft.network.chat.TranslatableComponent("afmr.copy.weathercondition.snow"));
        choice(Weather.THUNDERSTORM,left,119,half,new net.minecraft.network.chat.TranslatableComponent("afmr.copy.weathercondition.thunderstorm"));
        choice(Weather.CLEAR,left+half+6,119,half,new net.minecraft.network.chat.TranslatableComponent("afmr.copy.weathercondition.clear"));
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
        updateLabels();
    }
    private void choice(Weather weather,int x,int y,int width,Component help){
        var b=button("",x,y,width,()->{selected=weather;enabled=true;updateLabels();});
        var choiceHelpText=help;
        if(!choiceHelpText.getString().isBlank())b.description(choiceHelpText);choices.put(weather,b);
    }
    private void updateLabels(){
        for(var entry:choices.entrySet())entry.getValue().setMessage(new net.minecraft.network.chat.TextComponent((entry.getKey()==selected?"[x] ":"[ ] ")+entry.getKey().label()));
    }
    @Override public void onClose(){
        conditions.weatherEnabled=enabled;conditions.weather=selected;runtime.save();super.onClose();
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        String status="";
        if(runtime.inWorld()){
            var c=runtime.context();
            status="Rain/snow weather: "+c.raining()+"; thunder: "+c.thundering()+"; local type: "+c.precipitation().name().toLowerCase(java.util.Locale.ROOT);
        }
        menuFont.drawShadow(g,fit(status,contentWidth),left,bodyBottom()-49,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF));
    }
}

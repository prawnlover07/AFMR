package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

final class SettingsScreen extends MusicScreen {
    private final Map<String,EditBox> values=new LinkedHashMap<>();
    private String error="";
    private int page;
    SettingsScreen(Screen parent,ClientRuntime runtime){super("Playback settings",parent,runtime);}
    @Override protected void init(){
        super.init();values.clear();var s=runtime.library().settings;
        int half=(contentWidth-6)/2;
        if(page==0){
            field("Minimum silence (seconds)",s.silenceMinSeconds,47);
            field("Maximum silence (seconds)",s.silenceMaxSeconds,77);
            field("Fade duration (seconds)",s.fadeSeconds,107);
            field("Context buffer (seconds)",s.bufferSeconds,137);
            button("Restore Minecraft defaults",left,height-65,contentWidth,()->{
                s.restoreMinecraftPlaybackDefaults();runtime.save();error="";rebuildWidgets();
            });
        } else {
            field("Now-playing duration (seconds)",s.popupSeconds,47);
            button("Combat settings...",left,83,contentWidth,()->{if(apply())minecraft.gui.setScreen(new CombatSettingsScreen(this,runtime));});
            button("Combat music: "+(s.combatEnabled?"on":"off"),left,107,half,()->flip(()->s.combatEnabled=!s.combatEnabled));
            button("Now playing: "+(s.nowPlaying?"on":"off"),left+half+6,107,half,()->flip(()->s.nowPlaying=!s.nowPlaying));
            button("Rescan folders on launch: "+(s.rescanOnLaunch?"on":"off"),left,131,contentWidth,()->flip(()->s.rescanOnLaunch=!s.rescanOnLaunch));
            button("ReplayGain track tags: "+(s.replayGainEnabled?"on":"off"),left,155,contentWidth,()->flip(()->s.replayGainEnabled=!s.replayGainEnabled));
        }
        button(page==0?"More settings >":"< Timing settings",left,height-29,Math.min(150,half),()->{if(apply()){page=1-page;rebuildWidgets();}});
        button("Save & back",left+contentWidth-Math.min(130,half),height-29,Math.min(130,half),this::onClose);
    }
    private void field(String name,double value,int y){
        values.put(name,edit(Double.toString(value),name,left+contentWidth-100,y+12,100,null));
    }
    private void flip(Runnable change){if(apply()){change.run();runtime.save();rebuildWidgets();}}
    private boolean apply(){
        Map<String,Double> parsed=new HashMap<>();
        for(var entry:values.entrySet()){
            try{double value=Double.parseDouble(entry.getValue().getValue());if(!Double.isFinite(value)||value<0)throw new NumberFormatException();parsed.put(entry.getKey(),value);}
            catch(NumberFormatException e){error="Enter a finite, non-negative number for every field.";return false;}
        }
        var s=runtime.library().settings;
        if(page==0){
            double min=parsed.get("Minimum silence (seconds)"),max=parsed.get("Maximum silence (seconds)");
            double fade=parsed.get("Fade duration (seconds)"),buffer=parsed.get("Context buffer (seconds)");
            if(max<min||max>86400||fade>60||buffer>60){error="Silence: 0-86400, max >= min. Fade and buffer: 0-60.";return false;}
            s.silenceMinSeconds=min;s.silenceMaxSeconds=max;s.fadeSeconds=fade;s.bufferSeconds=buffer;
        }else{
            double popup=parsed.get("Now-playing duration (seconds)");
            if(popup<1||popup>60){error="Popup: 1-60 seconds.";return false;}
            s.popupSeconds=popup;
        }
        runtime.save();error="";return true;
    }
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        int i=0;for(String name:values.keySet())g.text(font,fit(name,contentWidth-108),left,50+(i++)*30,0xFFD9E8E0);
        String hint=page==0?"Defaults: 10-20 min gaps, no added fade/buffer. Future gaps only.":"ReplayGain: existing track tags + peak protection; next song only.";
        g.text(font,fit(error.isBlank()?hint:error,contentWidth),left,height-41,error.isBlank()?0xFF8FA7A0:0xFFFFA0A0);
    }
}

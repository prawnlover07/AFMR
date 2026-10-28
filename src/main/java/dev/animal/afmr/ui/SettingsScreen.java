package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.JukeboxMode;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;
import net.minecraft.client.gui.components.AbstractWidget;

final class SettingsScreen extends MusicScreen {
    private final Map<String,EditBox> values=new LinkedHashMap<>();
    private String error="";
    private int half,scroll,rows;
    private record Positioned(AbstractWidget control,int y){}
    private final List<Positioned> positions=new ArrayList<>();
    SettingsScreen(Screen parent,ClientRuntime runtime){super("Timing & playback",parent,runtime);}
    @Override protected void init(){
        super.init();values.clear();positions.clear();int first=children().size();var s=runtime.library().settings;
        half=(contentWidth-18)/2;int x=left+half+18;
        field("Min silence (sec)",s.silenceMinSeconds,52,Component.translatable("afmr.copy.settings.min_silence_sec"));
        field("Max silence (sec)",s.silenceMaxSeconds,76,Component.translatable("afmr.copy.settings.max_silence_sec"));
        field("Fade in (sec)",s.fadeInSeconds,100,Component.translatable("afmr.copy.settings.fade_in_sec"));
        field("Fade out (sec)",s.fadeOutSeconds,124,Component.translatable("afmr.copy.settings.fade_out_sec"));
        field("Context buffer (sec)",s.bufferSeconds,148,Component.translatable("afmr.copy.settings.context_buffer_sec"));
        var playImmediatelyOnJoinHelpControl=toggle("Play immediately on join",left,176,half,()->s.playImmediatelyOnJoin,()->flip(()->s.playImmediatelyOnJoin=!s.playImmediatelyOnJoin));
        var playImmediatelyOnJoinHelpText=Component.translatable("afmr.copy.settings.play_immediately_on_join");
        if(!playImmediatelyOnJoinHelpText.getString().isBlank())playImmediatelyOnJoinHelpControl.setTooltip(Tooltip.create(playImmediatelyOnJoinHelpText));
        addRenderableWidget(minecraft.options.musicFrequency().createButton(minecraft.options,x,52,half,value->{
            values.get("Min silence (sec)").setValue(Double.toString(s.silenceMinSeconds));
            values.get("Max silence (sec)").setValue(Double.toString(s.silenceMaxSeconds));
        }));
        var normalizationHelpControl=toggle("Normalization",x,78,half,()->s.replayGainEnabled,()->flip(()->s.replayGainEnabled=!s.replayGainEnabled));
        var normalizationHelpText=Component.translatable("afmr.copy.settings.normalization");
        if(!normalizationHelpText.getString().isBlank())normalizationHelpControl.setTooltip(Tooltip.create(normalizationHelpText));
        var continuePlayingOnMatchHelpControl=toggle("Continue playing on match",x,104,half,()->s.continuePlayingOnMatch,()->flip(()->s.continuePlayingOnMatch=!s.continuePlayingOnMatch));
        var continuePlayingOnMatchHelpText=Component.translatable("afmr.copy.settings.continue_playing_on_match");
        if(!continuePlayingOnMatchHelpText.getString().isBlank())continuePlayingOnMatchHelpControl.setTooltip(Tooltip.create(continuePlayingOnMatchHelpText));
        var jukeboxPolicyHelpControl=button("Jukebox: "+s.jukeboxMode.label(),x,130,half,()->flip(()->s.jukeboxMode=JukeboxMode.values()[(s.jukeboxMode.ordinal()+1)%JukeboxMode.values().length]));
        var jukeboxPolicyHelpText=Component.translatable("afmr.copy.settings.jukebox_policy");
        if(!jukeboxPolicyHelpText.getString().isBlank())jukeboxPolicyHelpControl.setTooltip(Tooltip.create(jukeboxPolicyHelpText));
        button("Songs before silence: "+s.songsBeforeSilence+" >",x,156,half,()->{if(apply())minecraft.gui.setScreen(new SongCountScreen(this,runtime,null));});
        for(int i=first;i<children().size();i++)if(children().get(i) instanceof AbstractWidget control)positions.add(new Positioned(control,control.getY()));
        rows=Math.max(1,(bodyBottom()-100)/24);scroll=Math.max(0,Math.min(scroll,Math.max(0,6-rows)));positionControls();
        addRenderableWidget(new SettingsScrollbar(left+contentWidth+2,52,rows*24,Math.max(0,6-rows),rows,()->scroll,this::scrollTo));
        int combatWidth=Math.min(150,contentWidth/3),backWidth=Math.min(130,contentWidth/4);
        var combatSettingsHelpControl=button("Combat settings >",left,bodyBottom()-29,combatWidth,()->{if(apply())minecraft.gui.setScreen(new CombatSettingsScreen(this,runtime));});
        var combatSettingsHelpText=Component.translatable("afmr.copy.settings.combat_settings");
        if(!combatSettingsHelpText.getString().isBlank())combatSettingsHelpControl.setTooltip(Tooltip.create(combatSettingsHelpText));
        var minecraftDefaultsHelpControl=button("Minecraft defaults",left+combatWidth+6,bodyBottom()-29,contentWidth-combatWidth-backWidth-12,()->{s.restoreMinecraftPlaybackDefaults();runtime.save();error="";rebuildWidgets();});
        var minecraftDefaultsHelpText=Component.translatable("afmr.copy.settings.minecraft_defaults");
        if(!minecraftDefaultsHelpText.getString().isBlank())minecraftDefaultsHelpControl.setTooltip(Tooltip.create(minecraftDefaultsHelpText));
        button("Save & back",left+contentWidth-backWidth,bodyBottom()-29,backWidth,this::onClose);
    }
    private void positionControls(){for(var item:positions){int row=(item.y-52)/24;var control=item.control;control.setY(item.y-scroll*24);control.visible=row>=scroll&&row<scroll+rows;if(!control.visible&&control.isFocused()){control.setFocused(false);setFocused(null);}}}
    private void scrollTo(int next){scroll=Math.max(0,Math.min(next,Math.max(0,6-rows)));positionControls();}
    @Override protected void revealControl(AbstractWidget control){for(var item:positions)if(item.control==control){scrollTo((item.y-52)/24);break;}}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){if(x>=left&&x<left+contentWidth&&y>=52&&y<52+rows*24&&dy!=0){scrollTo(scroll+(dy<0?1:-1));return true;}return super.mouseScrolled(x,y,dx,dy);}
    private void field(String name,double value,int y,Component help){
        var control=edit(Double.toString(value),name,left+half-76,y,76,null);
        var fieldHelpText=help;
        if(!fieldHelpText.getString().isBlank())control.setTooltip(Tooltip.create(fieldHelpText));values.put(name,control);
    }
    private void flip(Runnable change){if(apply()){change.run();runtime.save();rebuildWidgets();}}
    private boolean apply(){
        double[] parsed=new double[5];int i=0;
        try{for(var value:values.values()){double number=Double.parseDouble(value.getValue());if(!Double.isFinite(number)||number<0)throw new NumberFormatException();parsed[i++]=number;}}
        catch(NumberFormatException e){error="Invalid number";return false;}
        if(parsed[1]<parsed[0]||parsed[1]>86400){error="Silence: 0-86400; max >= min.";return false;}
        if(parsed[2]>60||parsed[3]>60||parsed[4]>60){error="Fades / buffer: 0-60 seconds.";return false;}
        var s=runtime.library().settings;s.silenceMinSeconds=parsed[0];s.silenceMaxSeconds=parsed[1];s.fadeInSeconds=parsed[2];s.fadeOutSeconds=parsed[3];s.bufferSeconds=parsed[4];
        runtime.save();error="";return true;
    }
    @Override protected boolean saveChanges(){return apply();}
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        int i=0;for(String name:values.keySet()){int row=i++;if(row>=scroll&&row<scroll+rows)g.text(menuFont,fit(name,half-82),left,58+(row-scroll)*24,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));}
        if(!error.isBlank())g.text(menuFont,fit(error,half),left,bodyBottom()-43,0xFFFFA0A0);
    }
}

package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import dev.animal.afmr.core.Library.DeathMusic;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

final class CombatSettingsScreen extends MusicScreen {
    private EditBox value;
    private int page;
    private String error="";
    CombatSettingsScreen(Screen parent,ClientRuntime runtime){super("Combat settings",parent,runtime);}
    CombatSettingsScreen(Screen parent,ClientRuntime runtime,boolean death){this(parent,runtime);page=death?1:0;}
    @Override protected void init(){
        super.init();var s=runtime.library().settings;int half=(contentWidth-6)/2;
        if(page==0){
            var combatMusicHelpControl=toggle("Combat music",left,47,contentWidth,()->s.combatEnabled,()->flip(()->s.combatEnabled=!s.combatEnabled));
            var combatMusicHelpText=Component.translatable("afmr.copy.combatsettings.combat_music");
            if(!combatMusicHelpText.getString().isBlank())combatMusicHelpControl.setTooltip(Tooltip.create(combatMusicHelpText));
            toggle("On attack",left,75,half,()->s.combatOnAttack,()->flip(()->s.combatOnAttack=!s.combatOnAttack));
            toggle("On damage",left+half+6,75,half,()->s.combatOnDamage,()->flip(()->s.combatOnDamage=!s.combatOnDamage));
            var combatTriggersHelpControl=toggle("Combatants only",left,103,contentWidth,()->s.combatThreatsOnly,()->flip(()->s.combatThreatsOnly=!s.combatThreatsOnly));
            var combatTriggersHelpText=Component.translatable("afmr.copy.combatsettings.combat_triggers");
            if(!combatTriggersHelpText.getString().isBlank())combatTriggersHelpControl.setTooltip(Tooltip.create(combatTriggersHelpText));
            value=edit(""+s.combatQuietSeconds,"Combat quiet period",left+contentWidth-100,143,100,null);
            var quietPeriodHelpText=Component.translatable("afmr.copy.combatsettings.quiet_period");
            if(!quietPeriodHelpText.getString().isBlank())value.setTooltip(Tooltip.create(quietPeriodHelpText));
        }else{
            button("On death: "+switch(s.combatDeathMusic){case FADE->"start fading immediately";case CUT->"cut immediately";case KEEP_PLAYING->"finish combat song";},left,47,contentWidth,()->flip(()->s.combatDeathMusic=DeathMusic.values()[(s.combatDeathMusic.ordinal()+1)%3]));
            value=edit(""+s.combatDeathFadeSeconds,"Death fade seconds",left+contentWidth-100,95,100,null);
            var deathFadeHelpText=Component.translatable("afmr.copy.combatsettings.death_fade");
            if(!deathFadeHelpText.getString().isBlank())value.setTooltip(Tooltip.create(deathFadeHelpText));
            var newMusicWhileDeadHelpControl=toggle("Prevent new music while dead",left,127,contentWidth,()->s.preventMusicWhileDead,()->flip(()->s.preventMusicWhileDead=!s.preventMusicWhileDead));
            var newMusicWhileDeadHelpText=Component.translatable("afmr.copy.combatsettings.new_music_while_dead");
            if(!newMusicWhileDeadHelpText.getString().isBlank())newMusicWhileDeadHelpControl.setTooltip(Tooltip.create(newMusicWhileDeadHelpText));
        }
        button(page==0?"Death options >":"< Combat triggers",left,bodyBottom()-29,150,()->{if(apply()){page=1-page;rebuildWidgets();}});
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    private void flip(Runnable action){if(apply()){action.run();runtime.save();rebuildWidgets();}}
    private boolean apply(){
        try{double number=Double.parseDouble(value.getValue());if(!Double.isFinite(number)||number<(page==0?1:0)||number>(page==0?600:60))throw new NumberFormatException();
            var s=runtime.library().settings;if(page==0)s.combatQuietSeconds=number;else s.combatDeathFadeSeconds=number;runtime.save();error="";return true;}
        catch(NumberFormatException e){error=page==0?"Quiet period: 1-600 seconds.":"Death fade: 0-60 seconds (0 cuts immediately).";return false;}
    }
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(menuFont,fit(page==0?"Combat quiet period (seconds)":"Death fade duration (seconds)",contentWidth-108),left,page==0?149:101,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        g.text(menuFont,fit(error,contentWidth),left,bodyBottom()-48,0xFFFFA0A0);
    }
}

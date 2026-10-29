package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.network.chat.Component;
import dev.animal.afmr.core.Library.DeathMusic;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.client.gui.screens.Screen;

final class CombatSettingsScreen extends MusicScreen {
    private MenuField value;
    private int page;
    private String error="";
    CombatSettingsScreen(Screen parent,ClientRuntime runtime){super("Combat settings",parent,runtime);}
    CombatSettingsScreen(Screen parent,ClientRuntime runtime,boolean death){this(parent,runtime);page=death?1:0;}
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();var s=runtime.library().settings;int half=(contentWidth-6)/2;
        if(page==0){
            var combatMusicHelpControl=toggle("Combat music",left,47,contentWidth,()->s.combatEnabled,()->flip(()->s.combatEnabled=!s.combatEnabled));
            var combatMusicHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.combatsettings.combat_music");
            if(!combatMusicHelpText.getString().isBlank())combatMusicHelpControl.description(combatMusicHelpText);
            toggle("On attack",left,75,half,()->s.combatOnAttack,()->flip(()->s.combatOnAttack=!s.combatOnAttack));
            var incoming=toggle("On damage",left+half+6,75,half,()->s.combatOnDamage,()->flip(()->s.combatOnDamage=!s.combatOnDamage));
            incoming.description(new net.minecraft.network.chat.TextComponent("Incoming hits and ranged hits work in singleplayer or with AFMR's server companion. Without it, only direct attacks start combat."));
            var combatTriggersHelpControl=toggle("Combatants only",left,103,contentWidth,()->s.combatThreatsOnly,()->flip(()->s.combatThreatsOnly=!s.combatThreatsOnly));
            var combatTriggersHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.combatsettings.combat_triggers");
            if(!combatTriggersHelpText.getString().isBlank())combatTriggersHelpControl.description(combatTriggersHelpText);
            value=edit(""+s.combatQuietSeconds,"Combat quiet period",left+contentWidth-100,143,100,null);
            var quietPeriodHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.combatsettings.quiet_period");
            if(!quietPeriodHelpText.getString().isBlank())value.description(quietPeriodHelpText);
        }else{
            button("On death: "+switch(s.combatDeathMusic){case FADE->"start fading immediately";case CUT->"cut immediately";case KEEP_PLAYING->"finish combat song";},left,47,contentWidth,()->flip(()->s.combatDeathMusic=DeathMusic.values()[(s.combatDeathMusic.ordinal()+1)%3]));
            value=edit(""+s.combatDeathFadeSeconds,"Death fade seconds",left+contentWidth-100,95,100,null);
            var deathFadeHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.combatsettings.death_fade");
            if(!deathFadeHelpText.getString().isBlank())value.description(deathFadeHelpText);
            var newMusicWhileDeadHelpControl=toggle("Prevent new music while dead",left,127,contentWidth,()->s.preventMusicWhileDead,()->flip(()->s.preventMusicWhileDead=!s.preventMusicWhileDead));
            var newMusicWhileDeadHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.combatsettings.new_music_while_dead");
            if(!newMusicWhileDeadHelpText.getString().isBlank())newMusicWhileDeadHelpControl.description(newMusicWhileDeadHelpText);
        }
        button(page==0?"Death options >":"< Combat triggers",left,bodyBottom()-29,150,()->{if(apply()){page=1-page;init(minecraft,width,height);}});
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    private void flip(Runnable action){if(apply()){action.run();runtime.save();init(minecraft,width,height);}}
    private boolean apply(){
        try{double number=Double.parseDouble(value.getValue());if(!Double.isFinite(number)||number<(page==0?1:0)||number>(page==0?600:60))throw new NumberFormatException();
            var s=runtime.library().settings;if(page==0)s.combatQuietSeconds=number;else s.combatDeathFadeSeconds=number;runtime.save();error="";return true;}
        catch(NumberFormatException e){error=page==0?"Quiet period: 1-600 seconds.":"Death fade: 0-60 seconds (0 cuts immediately).";return false;}
    }
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        menuFont.drawShadow(g,fit(page==0?"Combat quiet period (seconds)":"Death fade duration (seconds)",contentWidth-108),left,page==0?149:101,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        menuFont.drawShadow(g,fit(error,contentWidth),left,bodyBottom()-48,0xFFFFA0A0);
    }
}

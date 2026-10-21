package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.DeathMusic;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

final class CombatSettingsScreen extends MusicScreen {
    private EditBox value;
    private int page;
    private String error="";
    CombatSettingsScreen(Screen parent,ClientRuntime runtime){super("Combat settings",parent,runtime);}
    @Override protected void init(){
        super.init();var s=runtime.library().settings;int half=(contentWidth-6)/2;
        if(page==0){
            button("Combat music: "+(s.combatEnabled?"on":"off"),left,47,contentWidth,()->flip(()->s.combatEnabled=!s.combatEnabled));
            button("On attack: "+(s.combatOnAttack?"on":"off"),left,75,half,()->flip(()->s.combatOnAttack=!s.combatOnAttack));
            button("On damage: "+(s.combatOnDamage?"on":"off"),left+half+6,75,half,()->flip(()->s.combatOnDamage=!s.combatOnDamage));
            value=edit(""+s.combatQuietSeconds,"Combat quiet period",left+contentWidth-100,119,100,null);
        }else{
            button("On death: "+switch(s.combatDeathMusic){case FADE->"start fading immediately";case CUT->"cut immediately";case KEEP_PLAYING->"finish combat song";},left,47,contentWidth,()->flip(()->s.combatDeathMusic=DeathMusic.values()[(s.combatDeathMusic.ordinal()+1)%3]));
            value=edit(""+s.combatDeathFadeSeconds,"Death fade seconds",left+contentWidth-100,95,100,null);
            button("New music while dead: "+(s.preventMusicWhileDead?"paused":"allowed"),left,127,contentWidth,()->flip(()->s.preventMusicWhileDead=!s.preventMusicWhileDead));
        }
        button(page==0?"Death options >":"< Combat triggers",left,height-29,150,()->{if(apply()){page=1-page;rebuildWidgets();}});
        button("Save & back",left+contentWidth-104,height-29,104,this::onClose);
    }
    private void flip(Runnable action){if(apply()){action.run();runtime.save();rebuildWidgets();}}
    private boolean apply(){
        try{double number=Double.parseDouble(value.getValue());if(!Double.isFinite(number)||number<(page==0?1:0)||number>(page==0?600:60))throw new NumberFormatException();
            var s=runtime.library().settings;if(page==0)s.combatQuietSeconds=number;else s.combatDeathFadeSeconds=number;runtime.save();error="";return true;}
        catch(NumberFormatException e){error=page==0?"Quiet period: 1-600 seconds.":"Death fade: 0-60 seconds (0 cuts immediately).";return false;}
    }
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit(page==0?"Combat quiet period (seconds)":"Death fade duration (seconds)",contentWidth-108),left,page==0?125:101,0xFFD9E8E0);
        String hint=page==0?"Each enabled trigger restarts the quiet period.":"Death fades bypass the normal context buffer and finish-song toggle.";
        g.text(font,fit(hint,contentWidth),left,157,0xFF8FA7A0);
        g.text(font,fit(page==0?"Death and respawn clear the combat timer.":"Applies to Combat and custom playlists requiring combat.",contentWidth),left,174,0xFF8FA7A0);
        g.text(font,fit(error,contentWidth),left,height-48,0xFFFFA0A0);
    }
}

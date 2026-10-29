package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.State;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class CombatConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private State selected;
    private boolean playersOnly;
    private FlatButton state,players;

    CombatConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Combat condition",parent,runtime);this.conditions=conditions;
        selected=conditions.combat;playersOnly=conditions.combatPlayersOnly;
    }
    @Override protected void init(){
        super.init();
        state=button("",left,47,contentWidth,()->{selected=State.values()[(selected.ordinal()+1)%3];updateLabels();});
        var combatStateHelpText=Component.translatable("afmr.copy.combatcondition.combat_state");
        if(!combatStateHelpText.getString().isBlank())state.description(combatStateHelpText);
        players=toggle("Players only",left,75,contentWidth,()->playersOnly,()->{playersOnly=!playersOnly;updateLabels();});
        var playersOnlyHelpText=Component.translatable("afmr.copy.combatcondition.players_only");
        if(!playersOnlyHelpText.getString().isBlank())players.description(playersOnlyHelpText);
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
        updateLabels();
    }
    private void updateLabels(){
        state.setMessage(Component.literal("Combat: "+switch(selected){case ANY->"any";case YES->playersOnly?"in PvP":"in combat";case NO->playersOnly?"not in PvP":"peaceful";}));
        players.active=selected!=State.ANY;
    }
    @Override protected boolean saveChanges(){
        conditions.combat=selected;conditions.combatPlayersOnly=playersOnly;runtime.save();return true;
    }
    @Override public void onClose(){if(saveChanges())super.onClose();}
}

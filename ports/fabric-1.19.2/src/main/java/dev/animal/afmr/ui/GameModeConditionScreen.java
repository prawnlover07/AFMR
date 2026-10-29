package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.GameMode;
import dev.animal.afmr.core.PlaylistConditions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.EnumSet;

final class GameModeConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private final EnumSet<GameMode> selected=EnumSet.noneOf(GameMode.class);
    private boolean enabled;
    private FlatButton toggle;

    GameModeConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Gamemode condition",parent,runtime);this.conditions=conditions;
        enabled=conditions.gameModeEnabled;selected.addAll(conditions.gameModes);
    }
    @Override protected void init(){
        super.init();
        toggle=toggle("Filter",left,47,contentWidth,()->enabled,()->{enabled=!enabled;});
        var gamemodeFilterHelpText=Component.translatable("afmr.copy.gamemodecondition.gamemode_filter");
        if(!gamemodeFilterHelpText.getString().isBlank())toggle.description(gamemodeFilterHelpText);
        int half=(contentWidth-6)/2,i=0;
        for(var mode:GameMode.values()){
            toggle(mode.label(),left+(i%2)*(half+6),79+(i/2)*24,half,()->selected.contains(mode),()->{
                if(!selected.remove(mode))selected.add(mode);
            });
            i++;
        }
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override protected boolean saveChanges(){
        conditions.gameModeEnabled=enabled;conditions.gameModes=selected.clone();runtime.save();return true;
    }
    @Override public void onClose(){saveChanges();super.onClose();}
}

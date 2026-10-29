package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.State;
import net.minecraft.client.gui.screens.Screen;

final class StateConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private final ConditionOption option;
    StateConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions,ConditionOption option){
        super(option.label,parent,runtime);this.conditions=conditions;this.option=option;
    }
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();
        State state=option==ConditionOption.WATER?conditions.water:conditions.ridingAnimal;
        String yes=option==ConditionOption.WATER?"Underwater":"Riding an animal",no=option==ConditionOption.WATER?"Out of water":"Not riding an animal";
        for(int i=0;i<3;i++){
            State choice=State.values()[i];
            var control=button(i==0?"Any":i==1?yes:no,left,60+i*26,contentWidth,()->{
                if(option==ConditionOption.WATER)conditions.water=choice;else conditions.ridingAnimal=choice;
                runtime.save();init(minecraft,width,height);
            });control.selected=state==choice;
        }
        button("Back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
}

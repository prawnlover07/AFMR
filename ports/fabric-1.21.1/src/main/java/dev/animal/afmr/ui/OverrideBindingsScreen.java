package dev.animal.afmr.ui;

import dev.animal.afmr.Afmr;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class OverrideBindingsScreen extends MusicScreen {
    private int page;
    OverrideBindingsScreen(Screen parent,ClientRuntime runtime){super("Keys & override slots",parent,runtime);}
    @Override protected void init(){
        super.init();int size=Math.max(1,(bodyBottom()-130)/24);
        page=Math.max(0,Math.min(page,(Library.OVERRIDE_SLOTS-1)/size));
        button("Minecraft key binds >",left,47,contentWidth,()->minecraft.setScreen(new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(this,minecraft.options)));
        for(int i=page*size;i<Math.min(Library.OVERRIDE_SLOTS,(page+1)*size);i++){
            int index=i;var b=runtime.library().settings.overrideSlots[i];var p=runtime.library().playlist(b.playlistId);
            String name=b.playlistId.isBlank()?"Unassigned":p==null?"Missing playlist":p.name;
            var overrideSlotAssignmentHelpControl=button((i+1)+" ["+Afmr.overrideKeyName(i)+"] "+name+" / "+(b.mode==Library.OverrideMode.ONE_SONG?"one song":"until released"),left,68+(i-page*size)*24,contentWidth,
                ()->minecraft.setScreen(new OverridesScreen(this,runtime,index,b.playlistId)));
            var overrideSlotAssignmentHelpText=Component.translatable("afmr.copy.overridebindings.override_slot_assignment");
            if(!overrideSlotAssignmentHelpText.getString().isBlank())overrideSlotAssignmentHelpControl.setTooltip(Tooltip.create(overrideSlotAssignmentHelpText));
        }
        pages(page,size,Library.OVERRIDE_SLOTS,bodyBottom()-53,next->{page=next;rebuildWidgets();});
        button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
}

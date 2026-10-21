package dev.animal.afmr.ui;

import dev.animal.afmr.Afmr;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

final class OverrideBindingsScreen extends MusicScreen {
    private int page;
    OverrideBindingsScreen(Screen parent,ClientRuntime runtime){super("Override keybind slots",parent,runtime);}
    @Override protected void init(){
        super.init();int size=Math.max(1,(height-130)/24);
        page=Math.clamp(page,0,(Library.OVERRIDE_SLOTS-1)/size);
        for(int i=page*size;i<Math.min(Library.OVERRIDE_SLOTS,(page+1)*size);i++){
            int index=i;var b=runtime.library().settings.overrideSlots[i];var p=runtime.library().playlist(b.playlistId);
            String name=b.playlistId.isBlank()?"Unassigned":p==null?"Missing playlist":p.name;
            button((i+1)+" ["+Afmr.overrideKeyName(i)+"] "+name+" / "+(b.mode==Library.OverrideMode.ONE_SONG?"one song":"until released"),left,68+(i-page*size)*24,contentWidth,
                ()->minecraft.gui.setScreen(new OverridesScreen(this,runtime,index,b.playlistId)));
        }
        pages(page,size,Library.OVERRIDE_SLOTS,height-53,next->{page=next;rebuildWidgets();});
        button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){g.text(font,fit("Bind Override slot 1-8 in Minecraft Options > Controls.",contentWidth),left,48,0xFF8FA7A0);}
}

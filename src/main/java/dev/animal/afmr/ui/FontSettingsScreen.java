package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

final class FontSettingsScreen extends MusicScreen {
    private final CustomFonts.Target target;
    private String query="";
    private DenseList<String> choices;
    private List<String> available=List.of();
    private boolean busy;
    FontSettingsScreen(Screen parent,ClientRuntime runtime,CustomFonts.Target target){super(target==CustomFonts.Target.MENU?"Menu font":"Widget font",parent,runtime);this.target=target;}
    @Override protected void init(){
        int offset=choices==null?0:choices.offset();super.init();busy=runtime.fonts().busy();available=runtime.fonts().choices();
        edit(query,"Search fonts",left,72,contentWidth,value->{query=value;refresh();});
        choices=addRenderableWidget(new DenseList<>(left,100,contentWidth,Math.max(14,bodyBottom()-164),(g,id,x,y,w,hover)->{
            if(id.equals(runtime.fonts().selection(target))||hover)g.fill(x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
            g.text(menuFont,fit(id,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3),false);
        },(id,event,twice)->{runtime.fonts().select(target,id);rebuildWidgets();},id->id));refresh();choices.offset(offset);choices.active=!busy;
        int third=(contentWidth-8)/3;
        button("Minecraft default",left,bodyBottom()-29,third,()->{runtime.fonts().select(target,"minecraft:default");rebuildWidgets();}).active=!busy;
        button("Import TTF...",left+third+4,bodyBottom()-29,third,()->NativePicker.open(runtime,target==CustomFonts.Target.MENU?NativePicker.Kind.MENU_FONT:NativePicker.Kind.WIDGET_FONT,"")).active=!busy;
        button("Back",left+2*(third+4),bodyBottom()-29,third,this::onClose);
    }
    private void refresh(){if(choices!=null)choices.items(available.stream().filter(id->id.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList());}
    @Override public void tick(){if(busy!=runtime.fonts().busy()){busy=runtime.fonts().busy();rebuildWidgets();}}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float dt){
        g.text(menuFont,fit(runtime.fonts().name(target),contentWidth),left,54,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF),false);
        g.text(menuFont,fit(runtime.fonts().status(),contentWidth),left,bodyBottom()-49,0xFFFFB16E,false);
    }
}

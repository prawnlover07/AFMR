package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

final class ProfileLinksScreen extends MusicScreen {
    private final String profileId;
    private String selected="";
    ProfileLinksScreen(Screen parent,ClientRuntime runtime,String id){super("Linked worlds / servers",parent,runtime);profileId=id;}
    @Override protected void init(){
        super.init();
        var rows=addRenderableWidget(new DenseList<String>(left,57,contentWidth,Math.max(28,bodyBottom()-125),
            (g,key,x,y,w,hover)->{
                if(key.equals(selected))g.fill(x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
                g.drawString(menuFont,fit(key,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6),false);
            },(key,clickX,clickY,button,twice)->{selected=key;rebuildWidgets();},key->key));
        rows.items(runtime.library().profiles.bindings.entrySet().stream().filter(e->e.getValue().equals(profileId)).map(java.util.Map.Entry::getKey).toList());
        button("Remove selected link",left,bodyBottom()-59,contentWidth,()->{
            runtime.library().profiles.bindings.remove(selected);selected="";runtime.save();rebuildWidgets();
        }).active=!selected.isBlank();
        button("Back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
}

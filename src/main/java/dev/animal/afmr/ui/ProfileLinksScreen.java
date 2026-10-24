package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

final class ProfileLinksScreen extends MusicScreen {
    private final String profileId;
    private String selected="";
    ProfileLinksScreen(Screen parent,ClientRuntime runtime,String id){super("Linked worlds / servers",parent,runtime);profileId=id;}
    @Override protected void init(){
        super.init();
        var rows=addRenderableWidget(new DenseList<String>(left,57,contentWidth,Math.max(28,height-125),
            (g,key,x,y,w,hover)->{
                if(key.equals(selected))g.fill(x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF35594E));
                g.text(font,fit(key,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE3F2E9),false);
            },(key,event,twice)->{selected=key;rebuildWidgets();},key->key));
        rows.items(runtime.library().profiles.bindings.entrySet().stream().filter(e->e.getValue().equals(profileId)).map(java.util.Map.Entry::getKey).toList());
        button("Remove selected link",left,height-59,contentWidth,()->{
            runtime.library().profiles.bindings.remove(selected);selected="";runtime.save();rebuildWidgets();
        }).active=!selected.isBlank();
        button("Back",left,height-29,contentWidth,this::onClose);
    }
}

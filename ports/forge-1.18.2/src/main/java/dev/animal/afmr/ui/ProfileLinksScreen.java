package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

final class ProfileLinksScreen extends MusicScreen {
    private final String profileId;
    private String selected="";
    ProfileLinksScreen(Screen parent,ClientRuntime runtime,String id){super("Linked worlds / servers",parent,runtime);profileId=id;}
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();
        var rows=addRenderableWidget(new DenseList<String>(left,57,contentWidth,Math.max(28,bodyBottom()-125),
            (g,key,x,y,w,hover)->{
                if(key.equals(selected))net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
                menuFont.draw(g,fit(key,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6));
            },(key,clickX,clickY,button,twice)->{selected=key;init(minecraft,width,height);},key->key));
        rows.items(runtime.library().profiles.bindings.entrySet().stream().filter(e->e.getValue().equals(profileId)).map(java.util.Map.Entry::getKey).toList());
        button("Remove selected link",left,bodyBottom()-59,contentWidth,()->{
            runtime.library().profiles.bindings.remove(selected);selected="";runtime.save();init(minecraft,width,height);
        }).active=!selected.isBlank();
        button("Back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
}

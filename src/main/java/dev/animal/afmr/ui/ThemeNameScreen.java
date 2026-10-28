package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.ColorTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;

final class ThemeNameScreen extends MusicScreen {
    private final ThemesScreen themes;
    private final String id;
    private EditBox name;
    ThemeNameScreen(ThemesScreen parent,ClientRuntime runtime,String id){super(id.isEmpty()?"Save theme":"Rename theme",parent,runtime);themes=parent;this.id=id;}
    @Override protected void init(){
        String draft=name==null?null:name.getValue();super.init();var existing=ColorTheme.find(runtime.library().settings,id);
        name=edit(draft!=null?draft:existing==null?"My theme":existing.name,"Theme name",left,65,contentWidth,null);name.setMaxLength(ColorTheme.MAX_NAME);setInitialFocus(name);
        var save=button(id.isEmpty()?"Save":"Rename",left,bodyBottom()-29,(contentWidth-6)/2,()->{
            if(id.isEmpty()){String created=runtime.themes().saveCurrent(name.getValue());if(created.isEmpty())return;themes.selected(created);}
            else if(!runtime.themes().rename(id,name.getValue()))return;
            super.onClose();
        });name.setResponder(value->save.active=!value.isBlank());
        button("Cancel",left+(contentWidth-6)/2+6,bodyBottom()-29,(contentWidth-6)/2,this::onClose);
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float dt){g.text(menuFont,fit(runtime.themes().status(),contentWidth),left,95,0xFFFFB16E,false);}
}

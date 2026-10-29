package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Live appearance editing; saved custom themes update only after all drafts validate. */
final class ThemeEditorScreen extends MusicScreen {
    private final ThemesScreen themes;
    private final ColorTheme opening;
    private final Map<String,MenuField> fields=new LinkedHashMap<>();
    private final Map<String,String> drafts=new LinkedHashMap<>();
    private String error="";
    private int page;
    private final List<String> keys=new ArrayList<>();
    ThemeEditorScreen(ThemesScreen parent,ClientRuntime runtime){
        super("Theme colors",parent,runtime);themes=parent;opening=runtime.themes().draft().copy();
        for(var role:MenuColor.values())keys.add(role.name());keys.addAll(List.of("menuOpacity","widgetBackground","widgetOpacity"));
    }
    private boolean color(String key){return !key.equals("menuOpacity")&&!key.equals("widgetOpacity");}
    private String label(String key){return switch(key){case "menuOpacity"->"Menu opacity (%)";case "widgetOpacity"->"Widget opacity (%)";case "widgetBackground"->"Widget background";default->MenuColor.valueOf(key).label;};}
    private int rgb(String key){return key.equals("widgetBackground")?runtime.themes().draft().widgetBackground:MenuColor.valueOf(key).rgb(runtime.themes().draft().menuColors);}
    private String value(String key){var s=runtime.themes().draft();return switch(key){case "menuOpacity"->Double.toString(s.menuOpacity*100);case "widgetOpacity"->Double.toString(s.widgetOpacity*100);default->String.format(Locale.ROOT,"#%06X",key.equals("widgetBackground")?s.widgetBackground:MenuColor.valueOf(key).rgb(s.menuColors));};}
    private void apply(String key,String text){
        var s=runtime.themes().draft();
        if(color(key)){int rgb=MenuColor.parse(text);if(key.equals("widgetBackground"))s.widgetBackground=rgb;else s.menuColors.put(MenuColor.valueOf(key),rgb);}
        else{double opacity=Double.parseDouble(text.strip())/100;if(!ColorTheme.validOpacity(opacity))throw new IllegalArgumentException("Use opacity from 0 to 100.");if(key.equals("menuOpacity"))s.menuOpacity=opacity;else s.widgetOpacity=opacity;}
    }
    @Override protected void init(){
        super.init();fields.forEach((key,field)->drafts.put(key,field.getValue()));fields.clear();int size=pageSize(50,65);page=Math.max(0,Math.min(page,(keys.size()-1)/size));
        for(int i=page*size;i<Math.min(keys.size(),(page+1)*size);i++){
            String key=keys.get(i);int y=50+(i-page*size)*22;
            var field=edit(drafts.getOrDefault(key,value(key)),label(key),left+contentWidth-90,y,90,null);field.setMaxLength(color(key)?7:10);fields.put(key,field);
            field.setResponder(text->{try{apply(key,text);error="";}catch(IllegalArgumentException ex){error=color(key)?"Use six hex digits: #RRGGBB":"Use opacity from 0 to 100.";}});
            if(color(key)){
                var text=Component.translatable("afmr.copy.menucolors.hex_color_field");if(!text.getString().isBlank())field.description(text);
                addRenderableWidget(new ColorSwatchButton("Pick "+label(key),left+contentWidth-116,y+1,18,18,()->rgb(key),()->{
                    int initial;try{initial=MenuColor.parse(field.getValue());}catch(IllegalArgumentException ex){initial=rgb(key);}
                    minecraft.setScreen(new ColorPickerScreen(this,runtime,label(key)+" color",initial,rgb->field.setValue(String.format(Locale.ROOT,"#%06X",rgb))));
                }));
            }
        }
        if(keys.size()>size)pages(page,size,keys.size(),bodyBottom()-53,next->{if(validateDrafts()){page=next;rebuildWidgets();}});
        int third=(contentWidth-12)/3;
        var reset=button("Reset menu colors",left,bodyBottom()-29,third,()->{runtime.themes().draft().menuColors=MenuColor.defaults();drafts.clear();fields.clear();error="";rebuildWidgets();});
        var help=Component.translatable("afmr.copy.menucolors.reset_menu_colors");if(!help.getString().isBlank())reset.description(help);
        button("Cancel",left+third+6,bodyBottom()-29,third,this::onClose);
        button("Apply",left+2*(third+6),bodyBottom()-29,third,()->{if(validateDrafts()&&!themes.applyAndClose())error=runtime.themes().status();});
    }
    private boolean validateDrafts(){
        fields.forEach((key,field)->drafts.put(key,field.getValue()));
        for(var draft:drafts.entrySet())try{apply(draft.getKey(),draft.getValue());}catch(IllegalArgumentException ex){
            error=color(draft.getKey())?"Use six hex digits: #RRGGBB":"Use opacity from 0 to 100.";
            if(!fields.containsKey(draft.getKey())){page=keys.indexOf(draft.getKey())/pageSize(50,65);rebuildWidgets();}return false;
        }
        error="";return true;
    }
    @Override public void onClose(){runtime.themes().restoreDraft(opening);super.onClose();}
    @Override protected void drawBody(PoseStack g,int x,int y,float dt){int row=0;for(var key:fields.keySet())menuFont.drawShadow(g,fit(label(key),contentWidth-126),left,56+row++*22,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));if(!error.isBlank())menuFont.drawShadow(g,fit(error,contentWidth-138),left+138,bodyBottom()-47,0xFFFFA0A0);}
}

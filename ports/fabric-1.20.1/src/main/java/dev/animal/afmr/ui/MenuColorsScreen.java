package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.EnumMap;
import java.util.Locale;

/** Valid RGB edits preview live; navigation validates drafts and saves the palette. */
final class MenuColorsScreen extends MusicScreen {
    private final EnumMap<MenuColor,EditBox> fields=new EnumMap<>(MenuColor.class);
    private final EnumMap<MenuColor,String> drafts=new EnumMap<>(MenuColor.class);
    private String error="";
    private int page;
    MenuColorsScreen(Screen parent,ClientRuntime runtime){super("Menu colors",parent,runtime);}
    @Override protected void init(){
        super.init();fields.forEach((role,field)->drafts.put(role,field.getValue()));fields.clear();var roles=MenuColor.values();int size=pageSize(50,65);
        page=Math.max(0,Math.min(page,(roles.length-1)/size));
        for(int i=page*size;i<Math.min(roles.length,(page+1)*size);i++){
            var role=roles[i];int y=50+(i-page*size)*22;
            var field=edit(drafts.getOrDefault(role,String.format(Locale.ROOT,"#%06X",role.rgb(runtime.library().settings.menuColors))),role.label,left+contentWidth-90,y,90,null);
            field.setMaxLength(7);fields.put(role,field);
            var hexColorFieldHelpText=Component.translatable("afmr.copy.menucolors.hex_color_field");
            if(!hexColorFieldHelpText.getString().isBlank())field.setTooltip(Tooltip.create(hexColorFieldHelpText));
            field.setResponder(value->{try{runtime.library().settings.menuColors.put(role,MenuColor.parse(value));error="";}catch(IllegalArgumentException e){error="Use six hex digits: #RRGGBB";}});
            addRenderableWidget(new ColorSwatchButton("Pick "+role.label,left+contentWidth-116,y+1,18,18,()->role.rgb(runtime.library().settings.menuColors),()->{
                int initial;try{initial=MenuColor.parse(field.getValue());}catch(IllegalArgumentException e){initial=role.rgb(runtime.library().settings.menuColors);}
                minecraft.setScreen(new ColorPickerScreen(this,runtime,role.label+" color",initial,rgb->field.setValue(String.format(Locale.ROOT,"#%06X",rgb))));
            }));
        }
        if(roles.length>size)pages(page,size,roles.length,height-53,value->{if(saveChanges()){page=value;rebuildWidgets();}});
        var resetMenuColorsHelpControl=button("Reset colors",left,height-29,120,()->{runtime.library().settings.menuColors=MenuColor.defaults();runtime.save();drafts.clear();fields.clear();error="";rebuildWidgets();});
        var resetMenuColorsHelpText=Component.translatable("afmr.copy.menucolors.reset_menu_colors");
        if(!resetMenuColorsHelpText.getString().isBlank())resetMenuColorsHelpControl.setTooltip(Tooltip.create(resetMenuColorsHelpText));
        button("Save & back",left+contentWidth-120,height-29,120,this::onClose);
    }
    @Override protected void repositionElements(){
        if(!MusicMenu.retainWidgets(this))rebuildWidgets();
    }
    @Override protected boolean saveChanges(){
        fields.forEach((role,field)->drafts.put(role,field.getValue()));
        for(var draft:drafts.entrySet())try{MenuColor.parse(draft.getValue());}catch(IllegalArgumentException e){
            error="Use six hex digits: #RRGGBB";
            if(!fields.containsKey(draft.getKey())){page=draft.getKey().ordinal()/pageSize(50,65);rebuildWidgets();}
            return false;
        }
        runtime.save();error="";return true;
    }
    @Override public void onClose(){if(saveChanges())super.onClose();}
    @Override public void removed(){runtime.save();super.removed();}
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        int row=0;
        for(var role:fields.keySet()){
            int fieldY=50+row++*22;
            g.drawString(font,fit(role.label,contentWidth-126),left,fieldY+6,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        }
        if(!error.isBlank())g.drawString(font,fit(error,contentWidth-138),left+138,height-47,0xFFFFA0A0);
    }
}

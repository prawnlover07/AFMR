package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;

final class ThemesScreen extends MusicScreen {
    private String selectedId="";
    private static final MenuColor[] SWATCHES={MenuColor.BACKGROUND,MenuColor.PANELS,MenuColor.CONTROLS,MenuColor.SELECTION,MenuColor.ACCENT};
    private DenseList<ColorTheme> themes;
    private int revision,menuFocus;
    private String seenImport="";
    private boolean revealSelection=true;
    private List<ColorTheme> rows=List.of();
    private final List<FlatButton> menu=new ArrayList<>();
    ThemesScreen(Screen parent,ClientRuntime runtime){
        super("Themes",parent,runtime);runtime.themes().beginPreview();
        if(ColorTheme.graphite().matches(runtime.library().settings))selectedId=ColorTheme.GRAPHITE;
        for(var theme:runtime.library().settings.savedThemes)if(theme.matches(runtime.library().settings)){selectedId=theme.id;break;}
    }
    void selected(String id){selectedId=id;revealSelection=true;}
    @Override protected void init(){
        int offset=themes==null?0:themes.offset();super.init();if(runtime.themes().preview()==null)runtime.themes().draft();menu.clear();revision=runtime.themes().revision();int half=(contentWidth-6)/2;boolean ready=!runtime.themes().busy();
        button("Edit colors…",left,49,half,()->minecraft.setScreen(new ThemeEditorScreen(this,runtime))).active=ready;
        button("Save as…",left+half+6,49,half,()->minecraft.setScreen(new ThemeNameScreen(this,runtime,""))).active=ready&&runtime.library().settings.savedThemes.size()<ColorTheme.MAX_SAVED;
        themes=addRenderableWidget(new DenseList<>(left,79,contentWidth,Math.max(28,bodyBottom()-164),(g,theme,x,y,w,hover)->{
            if(theme.id.equals(selectedId)||hover)g.fill(x,y,x+w,y+14,MenuAppearance.color(theme.id.equals(selectedId)?MenuColor.SELECTION:MenuColor.HOVER,theme.id.equals(selectedId)?0xFF304963:0xFF3B424D));
            g.drawString(menuFont,fit(theme.name+(theme.id.equals(selectedId)&&!theme.matches(runtime.themes().draft())?" *":""),w-91),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3),false);
            for(int i=0;i<SWATCHES.length;i++)g.fill(x+w-78+i*14,y+3,x+w-66+i*14,y+11,0xFF000000|SWATCHES[i].rgb(theme.menuColors));
        },(theme,clickX,clickY,button,twice)->{if(runtime.themes().previewTheme(theme.id)){selected(theme.id);rebuildWidgets();}},theme->theme.name));
        var items=new ArrayList<ColorTheme>();items.add(ColorTheme.graphite());items.addAll(runtime.library().settings.savedThemes);rows=items;themes.items(items);themes.offset(offset);themes.active=ready;
        var imported=ColorTheme.find(runtime.library().settings,runtime.themes().importedId());
        if(imported!=null&&!seenImport.equals(imported.id)){themes.reveal(imported);seenImport=imported.id;}
        else if(revealSelection)for(var theme:rows)if(theme.id.equals(selectedId)){themes.reveal(theme);break;}
        revealSelection=false;
        button("Import…",left,bodyBottom()-53,half,()->NativePicker.open(runtime,NativePicker.Kind.THEME_IMPORT,"")).active=ready&&runtime.library().settings.savedThemes.size()<ColorTheme.MAX_SAVED;
        button("Theme actions ⋯",left+half+6,bodyBottom()-53,half,()->openMenu(left+half+6,bodyBottom()-53)).active=ready;
        button("Cancel",left,bodyBottom()-29,half,this::onClose);
        button("Apply",left+half+6,bodyBottom()-29,half,this::applyAndClose).active=ready;
        var text=Component.translatable("afmr.copy.themes.scope");if(!text.getString().isBlank())themes.setTooltip(Tooltip.create(text));
    }
    boolean applyAndClose(){if(!runtime.themes().commitPreview(selectedId))return false;super.onClose();return true;}
    @Override public void onClose(){runtime.themes().cancelPreview();super.onClose();}
    @Override public void added(){if(themes!=null)rebuildWidgets();}
    @Override public void tick(){if(revision!=runtime.themes().revision()||themes.active==runtime.themes().busy())rebuildWidgets();}
    private void openMenu(int x,int y){
        menu.clear();int w=Math.min(210,contentWidth),mx=Math.max(left,Math.min(x,left+contentWidth-w)),my=Math.max(49,Math.min(y,bodyBottom()-101));
        boolean saved=!selectedId.isEmpty()&&!selectedId.equals(ColorTheme.GRAPHITE)&&ColorTheme.find(runtime.library().settings,selectedId)!=null;
        item("Rename theme",mx,my,w,()->minecraft.setScreen(new ThemeNameScreen(this,runtime,selectedId)),saved);
        item("Export theme…",mx,my,w,()->{var selected=ColorTheme.find(runtime.library().settings,selectedId);String name=selected!=null?selected.name:"My theme";NativePicker.saveTheme(runtime,runtime.themes().snapshot(name));},true);
        item("Delete theme",mx,my,w,()->{var selected=ColorTheme.find(runtime.library().settings,selectedId);if(selected==null)return;String id=selectedId;minecraft.setScreen(new ConfirmScreen(yes->{if(yes){runtime.themes().delete(id);selectedId="";}minecraft.setScreen(this);},Component.literal("Delete "+selected.name+"?"),Component.literal("Remove saved theme?")));},saved);
        menuFocus=0;while(menuFocus<menu.size()-1&&!menu.get(menuFocus).active)menuFocus++;menu.get(menuFocus).setFocused(true);
    }
    private void item(String label,int x,int y,int w,Runnable action,boolean active){var button=new FlatButton(label,x,y+menu.size()*24,w,24,1,()->{menu.clear();action.run();});button.active=active;button.opaque=true;menu.add(button);}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(!menu.isEmpty()){for(var control:List.copyOf(menu))if(control.mouseClicked(x,y,button))return true;menu.clear();return true;}
        if(button==InputConstants.MOUSE_BUTTON_RIGHT){var theme=themes.itemAt(x,y);if(theme!=null&&!runtime.themes().busy()&&runtime.themes().previewTheme(theme.id)){selected(theme.id);revision=runtime.themes().revision();openMenu((int)x,(int)y);return true;}}
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(!menu.isEmpty()){
            if(key==InputConstants.KEY_ESCAPE){menu.clear();return true;}
            if(key==InputConstants.KEY_UP||key==InputConstants.KEY_DOWN){menu.get(menuFocus).setFocused(false);do{menuFocus=Math.floorMod(menuFocus+(key==InputConstants.KEY_UP?-1:1),menu.size());}while(!menu.get(menuFocus).active);menu.get(menuFocus).setFocused(true);return true;}
            menu.get(menuFocus).keyPressed(key,scan,modifiers);return true;
        }
        int actionKey=key;
        if(getFocused()==themes&&!runtime.themes().busy()&&(actionKey==InputConstants.KEY_UP||actionKey==InputConstants.KEY_DOWN||actionKey==InputConstants.KEY_HOME||actionKey==InputConstants.KEY_END)){
            int index=0;for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(selectedId)){index=i;break;}
            index=actionKey==InputConstants.KEY_HOME?0:actionKey==InputConstants.KEY_END?rows.size()-1:Math.max(0,Math.min(rows.size()-1,index+(actionKey==InputConstants.KEY_UP?-1:1)));
            var target=rows.get(index);if(runtime.themes().previewTheme(target.id)){selected(target.id);rebuildWidgets();setFocused(themes);themes.setFocused(true);}return true;
        }
        return super.keyPressed(key,scan,modifiers);

    }
    @Override public void render(GuiGraphics g,int x,int y,float dt){super.render(g,x,y,dt);for(var button:menu)button.render(g,x,y,dt);}
    @Override protected void drawBody(GuiGraphics g,int x,int y,float dt){g.drawString(menuFont,fit(runtime.themes().status(),contentWidth),left,bodyBottom()-78,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF),false);}
}

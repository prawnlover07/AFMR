package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.ArrayList;
import java.util.List;

abstract class MusicScreen extends Screen {
    protected Screen parent;
    protected final ClientRuntime runtime;
    protected int left, contentWidth;
    private IntConsumer scrollPage;
    private List<MenuSurface> backgrounds;
    protected MusicScreen(String title, Screen parent, ClientRuntime runtime) {
        super(Component.literal(title)); this.parent = parent; this.runtime = runtime;
    }
    @Override protected void init() {
        backgrounds=null;
        contentWidth = Math.min(760, width - 24); left = (width-contentWidth)/2;
        if(parent instanceof LibraryScreen editor){
            int count=WorkspacePage.values().length,gap=3,w=(contentWidth-gap*(count-1))/count;
            WorkspacePage selected=this instanceof SourcesScreen?WorkspacePage.LIBRARY:this instanceof SettingsScreen?WorkspacePage.TIMING
                :this instanceof NowPlayingSettingsScreen?WorkspacePage.DISPLAY:this instanceof OverrideBindingsScreen?WorkspacePage.KEYS
                :this instanceof PlaybackInspectorScreen?WorkspacePage.INSPECTOR:this instanceof ListeningScreen?WorkspacePage.ACTIVITY
                :this instanceof ProfilesScreen?WorkspacePage.PROFILES:null;
            int i=0;
            if(selected!=null)for(WorkspacePage page:WorkspacePage.values()){
                var tab=addRenderableWidget(new FlatButton(page.label,left+i++*(w+gap),27,w,16,1,()->{if(saveChanges())editor.openPage(page);}));
                tab.selected=page==selected;
            }
        }
    }
    protected boolean saveChanges(){return true;}
    @Override protected void repositionElements() {
        if (MusicMenu.retainWidgets(this)) return;
        // Resizing a hidden page still needs new bounds, but must keep draft text and list positions.
        var drafts = children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).map(EditBox::getValue).toList();
        var offsets = children().stream().filter(c -> c instanceof DenseList<?>).map(c -> ((DenseList<?>) c).offset()).toList();
        super.repositionElements();
        int field = 0, list = 0;
        for (var child : children()) {
            if (child instanceof EditBox box && field < drafts.size()) box.setValue(drafts.get(field++));
            if (child instanceof DenseList<?> rows && list < offsets.size()) rows.offset(offsets.get(list++));
        }
    }
    protected FlatButton button(String text, int x, int y, int w, Runnable action) {
        return addRenderableWidget(new FlatButton(text,x,y,w,20,1,action));
    }
    protected EditBox edit(String text, String hint, int x, int y, int w, Consumer<String> action) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.literal(hint));
        box.setMaxLength(4096); box.setValue(text); box.setHint(Component.literal(hint));
        if (action != null) box.setResponder(action); addRenderableWidget(box); return box;
    }
    protected String fit(String text, int pixels) { return font.plainSubstrByWidth(text, Math.max(10, pixels)); }
    protected int pageSize(int startY, int bottom) { return Math.max(1, (height - bottom - startY + 2) / 22); }
    protected void pages(int page, int size, int total, int y, Consumer<Integer> change) {
        int last = Math.max(0, (total-1)/size);
        scrollPage = delta -> change.accept(Math.max(0,Math.min(page+delta,last)));
        button("<", left, y, 26, () -> change.accept(Math.max(0, page-1))).active = page > 0;
        button((page+1) + " / " + (last+1), left+30, y, 68, () -> {}).active = false;
        button(">", left+102, y, 26, () -> change.accept(Math.min(last, page+1))).active = page < last;
    }
    @Override public boolean mouseScrolled(double x,double y,double vertical) {
        if (scrollPage != null && vertical != 0 && y > 95 && x >= left && x <= left+contentWidth) {
            scrollPage.accept(vertical < 0 ? 1 : -1); return true;
        }
        return super.mouseScrolled(x,y,vertical);
    }
    protected void drawBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        runtime.menuBackground().draw(g,width,height);
        if(backgrounds==null){
            backgrounds=new ArrayList<>();
            backgrounds.add(new MenuSurface(0,0,width,45,MenuColor.PANELS,0x1D2727));
            backgrounds.add(new MenuSurface(0,45,width,height,MenuColor.BACKGROUND,0x141B1B));
            for(var child:children())if(child instanceof DenseList<?> list)
                MenuSurface.cutOut(backgrounds,list.getX(),list.getY(),(list.getX()+list.getWidth()),(list.getY()+list.getHeight()));
        }
        for(var surface:backgrounds)g.fill(surface.x0(),surface.y0(),surface.x1(),surface.y1(),MenuAppearance.background(surface.color(),surface.rgb()));
        if (runtime.scanning()) g.drawString(font,fit("Working / "+runtime.scanProgress().processed()+" / "+runtime.scanProgress().discovered(),contentWidth),left,12,MenuAppearance.color(MenuColor.ACCENT,0xFFB9D7B7));
        else {
            g.drawString(font, title, left, 12, MenuAppearance.color(MenuColor.TEXT,0xFFE5EEE6));
        }
        drawBody(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.ArrayList;
import java.util.List;

abstract class MusicScreen extends Screen {
    protected Screen parent;
    protected net.minecraft.client.gui.Font menuFont;
    protected final ClientRuntime runtime;
    protected int left, contentWidth;
    private SeekBar seekBar;
    private NowPlayingLink currentTitle;
    private String focusSetting="";
    private AbstractWidget searchTarget;
    private long highlightUntil;
    private IntConsumer scrollPage;
    private List<MenuSurface> backgrounds;
    private WorkspaceLayout layout;
    protected MusicScreen(String title, Screen parent, ClientRuntime runtime) {
        super(Component.literal(title)); this.parent = parent; this.runtime = runtime;
    }
    @Override protected void init() {
        menuFont=runtime.fonts().font(CustomFonts.Target.MENU);
        backgrounds=null;
        var window=minecraft.getWindow();layout=WorkspaceLayout.fit(window.getWidth(),window.getHeight(),window.getGuiScale());
        contentWidth = Math.min(760, width - 24); left = (width-contentWidth)/2;
        LibraryScreen editor=workspace();
        if(editor!=null){
            float headerScale=layout.scale();
            var find=addRenderableWidget(new WorkspaceButton("Search settings...",layout.width()-484,4,126,18,headerScale,()->{
                onClose();if(minecraft.gui.screen()!=this){runtime.themes().cancelPreview();editor.openSearch();}
            }));find.visible=!(this instanceof SettingsSearchScreen);
            int titleWidth=Math.min(220,Math.max(60,width/3));
            currentTitle=addRenderableWidget(new NowPlayingLink(12,height-23,titleWidth,1,()->{onClose();if(minecraft.gui.screen()!=this){runtime.themes().cancelPreview();editor.locatePlayingSong();}}));
            seekBar=addRenderableWidget(new SeekBar(runtime,titleWidth+24,height-25,Math.max(100,width-titleWidth-36)));

            float scale=layout.scale();int navX=12;
            for(WorkspacePage page:WorkspacePage.values()){
                var tab=addRenderableWidget(new WorkspaceButton(page.label,navX,24,page.width,22,scale,()->{
                    onClose();if(minecraft.gui.screen()!=this){runtime.themes().cancelPreview();editor.openPage(page);}
                }));tab.selected=page==selectedPage();navX+=page.width+4;
            }
        }
    }
    protected LibraryScreen workspace(){Screen cursor=parent;while(cursor instanceof MusicScreen page)cursor=page.parent;return cursor instanceof LibraryScreen editor?editor:null;}
    private WorkspacePage selectedPage(){
        for(Screen cursor=this;cursor instanceof MusicScreen page;cursor=page.parent){
            if(page instanceof SourcesScreen)return WorkspacePage.LIBRARY;if(page instanceof SettingsScreen)return WorkspacePage.TIMING;
            if(page instanceof NowPlayingSettingsScreen)return WorkspacePage.DISPLAY;if(page instanceof OverrideBindingsScreen)return WorkspacePage.KEYS;
            if(page instanceof PlaybackInspectorScreen)return WorkspacePage.INSPECTOR;if(page instanceof ListeningScreen)return WorkspacePage.ACTIVITY;
            if(page instanceof ProfilesScreen)return WorkspacePage.PROFILES;
        }return WorkspacePage.PLAYLISTS;
    }

    @Override public void added(){
        var next=runtime.fonts().font(CustomFonts.Target.MENU);
        if(menuFont!=null&&menuFont!=next)repositionElements();
    }
    protected int bodyBottom(){return height-(workspace()==null?0:30);}
    protected void revealControl(AbstractWidget control){}
    void focusSetting(String label){focusSetting=label;}
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
    protected ToggleButton toggle(String text,int x,int y,int w,java.util.function.BooleanSupplier checked,Runnable action){
        return addRenderableWidget(new ToggleButton(text,x,y,w,20,1,checked,action));
    }
    protected EditBox edit(String text, String hint, int x, int y, int w, Consumer<String> action) {
        EditBox box = new EditBox(menuFont, x, y, w, 20, Component.literal(hint));
        box.setMaxLength(4096); box.setValue(text); box.setHint(Component.literal(hint));
        if (action != null) box.setResponder(action); addRenderableWidget(box); return box;
    }
    protected String fit(String text, int pixels) { return menuFont.plainSubstrByWidth(text, Math.max(10, pixels)); }
    protected int pageSize(int startY, int bottom) { return Math.max(1, (bodyBottom() - bottom - startY + 2) / 22); }
    protected void pages(int page, int size, int total, int y, Consumer<Integer> change) {
        int last = Math.max(0, (total-1)/size);
        scrollPage = delta -> change.accept(Math.clamp(page+delta,0,last));
        button("<", left, y, 26, () -> change.accept(Math.max(0, page-1))).active = page > 0;
        button((page+1) + " / " + (last+1), left+30, y, 68, () -> {}).active = false;
        button(">", left+102, y, 26, () -> change.accept(Math.min(last, page+1))).active = page < last;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if (scrollPage != null && vertical != 0 && y > 95 && x >= left && x <= left+contentWidth) {
            scrollPage.accept(vertical < 0 ? 1 : -1); return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    protected void drawBody(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float delta) {
        if(minecraft.level==null)extractPanorama(g,delta);
        // Blur starts before the current stratum, so the image must occupy the preceding one.
        if(runtime.menuBackground().draw(g,width,height,1))g.nextStratum();
        if(runtime.library().settings.menuBlur&&runtime.library().settings.menuBackgroundOpacity<1&&minecraft.gui.screen()==this)extractBlurredBackground(g);
        minecraft.gui.hud.extractDeferredSubtitles();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if(currentTitle!=null){
            runtime.engine().requestCurrentDuration();var track=runtime.engine().current();
            currentTitle.active=track!=null&&runtime.library().playlist(runtime.engine().sourcePlaylistId())!=null;
            currentTitle.label(runtime.minecraftMusicMuted()?"Music muted in Minecraft":track==null?"":track.title);
        }
        if(!focusSetting.isEmpty()){
            for(var child:children())if(child instanceof AbstractWidget control&&control.getMessage().getString().startsWith(focusSetting)){
                revealControl(control);setFocused(control);control.setFocused(true);searchTarget=control;highlightUntil=System.nanoTime()+3_000_000_000L;break;
            }focusSetting="";
        }
        if(backgrounds==null){
            backgrounds=new ArrayList<>();
            backgrounds.add(new MenuSurface(0,0,width,Math.round(64*layout.scale()),MenuColor.PANELS,0x202329));
            backgrounds.add(new MenuSurface(0,Math.round(64*layout.scale()),width,bodyBottom(),MenuColor.BACKGROUND,0x17191D));
            if(seekBar!=null)backgrounds.add(new MenuSurface(0,bodyBottom(),width,height,MenuColor.PANELS,0x202329));
            for(var child:children())if(child instanceof DenseList<?> list)
                MenuSurface.cutOut(backgrounds,list.getX(),list.getY(),list.getRight(),list.getBottom());
        }
        for(var surface:backgrounds)g.fill(surface.x0(),surface.y0(),surface.x1(),surface.y1(),MenuAppearance.background(surface.color(),surface.rgb()));
        float headerScale=layout.scale();
        g.pose().pushMatrix();g.pose().scale(headerScale,headerScale);
        String heading=runtime.scanning()?"Working / "+runtime.scanProgress().processed()+" / "+runtime.scanProgress().discovered():title.getString();
        g.text(menuFont,fit(heading,layout.width()-500),12,6,MenuAppearance.color(MenuColor.TEXT,0xFFEFF0F3),false);
        g.pose().popMatrix();
        drawBody(g, mouseX, mouseY, partialTick);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if(searchTarget!=null&&searchTarget.visible&&System.nanoTime()<highlightUntil){
            int x=searchTarget.getX()-1,y=searchTarget.getY()-1,w=searchTarget.getWidth()+2,h=searchTarget.getHeight()+2,color=MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF);
            g.fill(x,y,x+w,y+1,color);g.fill(x,y+h-1,x+w,y+h,color);g.fill(x,y,x+1,y+h,color);g.fill(x+w-1,y,x+w,y+h,color);
        }
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE&&seekBar!=null&&seekBar.cancelDrag())return true;return super.keyPressed(event);}
    void cancelSeek(){if(seekBar!=null)seekBar.cancel();}
    @Override public void removed(){cancelSeek();}
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

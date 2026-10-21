package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

abstract class MusicScreen extends Screen {
    protected final Screen parent;
    protected final ClientRuntime runtime;
    protected int left, contentWidth;
    private IntConsumer scrollPage;
    protected MusicScreen(String title, Screen parent, ClientRuntime runtime) {
        super(Component.literal(title)); this.parent = parent; this.runtime = runtime;
    }
    @Override protected void init() { contentWidth = Math.min(760, width - 24); left = (width-contentWidth)/2; }
    protected Button button(String text, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, w, 20).build());
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
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0,0,width,height,0xF511181C);
        g.fill(left,12,left+3,35,0xFF8DE0BE);
        if (runtime.scanning()) ScanProgressView.draw(g, font, runtime.scanProgress(), left+12, 13, contentWidth-12);
        else {
            g.text(font, title, left+12, 15, 0xFFF0F7F3);
            g.text(font, "animal's final music replacer", left+12, 28, 0xFF8FA7A0);
        }
        drawBody(g, mouseX, mouseY, partialTick);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Keeps the current menu page and its widgets while the menu is hidden. Client-thread only. */
public final class MusicMenu {
    private static Screen saved, restoring;
    private static int savedWidth, savedHeight;

    private MusicMenu() {}

    public static Screen reopen(Screen parent, ClientRuntime runtime) {
        runtime.themes().suspendPreview(false);
        if (saved == null) saved = new LibraryScreen(parent, runtime, "day_outside");
        Screen root = saved;
        while (root instanceof MusicScreen page && (page.parent instanceof MusicScreen || page.parent instanceof LibraryScreen)) root = page.parent;
        if (root instanceof LibraryScreen library) library.parent = parent;
        else if (root instanceof MusicScreen page) page.parent = parent;
        restoring = saved;
        savedWidth = saved.width;
        savedHeight = saved.height;
        return saved;
    }

    public static boolean hide(Screen screen) {
        if (!(screen instanceof LibraryScreen) && !(screen instanceof MusicScreen)) return false;
        if(screen instanceof MusicScreen page)page.cancelSeek();
        remember(screen);
        ClientRuntime.get().themes().suspendPreview(true);
        Screen root = screen;
        while (root instanceof MusicScreen page && (page.parent instanceof MusicScreen || page.parent instanceof LibraryScreen)) root = page.parent;
        Minecraft.getInstance().gui.setScreen(root instanceof LibraryScreen library ? library.parent : ((MusicScreen) root).parent);
        return true;
    }

    static void remember(Screen screen) { saved = screen; }

    static boolean retainWidgets(Screen screen) {
        if (restoring != screen) return false;
        restoring = null;
        if (screen.width != savedWidth || screen.height != savedHeight) return false;
        // Closing a screen stops native text input even when its focused widget survives.
        if (screen.getFocused() != null) screen.getFocused().setFocused(true);
        return true;
    }

    public static void clear() { saved = restoring = null; }
}

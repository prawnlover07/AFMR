package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.library.MusicFiles.ScanProgress;
import dev.animal.afmr.library.MusicFiles.ScanStage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Fits the existing information header, without covering music rows or controls. */
final class ScanProgressView {
    static void draw(GuiGraphics g, Font font, ScanProgress progress, int x, int y, int width) {
        String label = switch (progress.stage()) {
            case DISCOVERING -> "Finding music: " + progress.discovered() + " files found";
            case READING_TAGS -> "Reading tags: " + progress.processed() + " / " + progress.discovered()
                + " (" + (int)(progress.fraction() * 100) + "%)";
            case FINALIZING -> "Saving library: " + progress.processed() + " files processed";
            case CHECKING -> "Checking sources: " + progress.processed() + " / " + progress.discovered();
            case HASHING -> "Matching file contents: " + progress.processed() + " files checked";
            case REPAIRING -> "Preparing repair: " + progress.processed() + " / " + progress.discovered();
            case READING_PRESET -> "Reading playlist setup...";
            case WRITING_PRESET -> "Exporting playlist setup...";
        };
        g.drawString(font, font.plainSubstrByWidth(label, width), x, y, MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6), false);
        String path = progress.currentPath();
        // Keep the filename visible even when its parent path is very long.
        int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String file = path.substring(separator + 1);
        g.drawString(font, font.plainSubstrByWidth(file, width), x, y + 12, MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA1B4BC), false);
        int top = y + 24, innerWidth = Math.max(0, width - 2);
        g.fill(x, top, x + width, top + 6, MenuAppearance.color(MenuColor.CONTROLS,0xFF444E4D));
        g.fill(x + 1, top + 1, x + width - 1, top + 5, MenuAppearance.color(MenuColor.CONTROLS,0xFF24272F));
        if (progress.stage() == ScanStage.DISCOVERING || progress.stage() == ScanStage.HASHING) {
            int segment = Math.max(1, innerWidth / 5);
            int offset = (int)((System.nanoTime() / 12_000_000L) % Math.max(1, innerWidth + segment)) - segment;
            g.fill(x + 1 + Math.max(0, offset), top + 1,
                x + 1 + Math.min(innerWidth, offset + segment), top + 5, MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        } else {
            g.fill(x + 1, top + 1, x + 1 + (int)(innerWidth * progress.fraction()), top + 5, MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        }
    }
}

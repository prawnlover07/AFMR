package dev.animal.afmr.ui;

/** Geometry only. The canvas fits the window without changing Minecraft's GUI scale. */
record WorkspaceLayout(float scale, int width, int height, int sidebarWidth, int tableX,
                       int tableWidth, int rulesX, int rulesWidth, int priorityHeight,
                       int libraryY, int libraryListY, int libraryListHeight, int footerY,
                       int songHeaderY, int songListY, int songListHeight) {
    static WorkspaceLayout fit(int windowWidth, int windowHeight) {
        float scale = Math.min(1f, Math.min(Math.max(1, windowWidth) / 1000f, Math.max(1, windowHeight) / 540f));
        int width = Math.round(windowWidth / scale), height = Math.round(windowHeight / scale);
        int sidebar = 230, tableX = sidebar + 12, rulesWidth = 284, rulesX = width - rulesWidth - 12;
        int footer = height - 94, priorityHeight = Math.clamp((footer - 180) / 3, 98, 168);
        int libraryY = 94 + priorityHeight + 70, libraryListY = libraryY + 44;
        return new WorkspaceLayout(scale, width, height, sidebar, tableX, rulesX - tableX - 12,
            rulesX, rulesWidth, priorityHeight, libraryY, libraryListY, footer - 32 - libraryListY,
            footer, 164, 182, footer - 190);
    }
}

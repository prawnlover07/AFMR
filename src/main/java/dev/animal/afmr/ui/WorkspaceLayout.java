package dev.animal.afmr.ui;

/** Geometry only. Fit within Minecraft's GUI scale, on whole physical pixels where that costs little size. */
record WorkspaceLayout(float scale, int width, int height, int sidebarWidth, int tableX,
                       int tableWidth, int rulesX, int rulesWidth, int priorityHeight,
                       int libraryY, int libraryListY, int libraryListHeight, int footerY,
                       int songHeaderY, int songListY, int songListHeight) {
    // 350px table fits search/actions; 516px height retains six library rows and the footer.
    static final int MIN_WIDTH=900,MIN_HEIGHT=516;
    // Snapping 1.49 down to 1 on a 1366x768 laptop shrinks text by a third; keep the fractional size instead.
    static final double MAX_SNAP_SHRINK=.15;
    static WorkspaceLayout fit(int physicalWidth, int physicalHeight, double guiScale) {
        physicalWidth=Math.max(1,physicalWidth);physicalHeight=Math.max(1,physicalHeight);
        if(!Double.isFinite(guiScale)||guiScale<1)throw new IllegalArgumentException("GUI scale must be at least one");
        double fit=Math.min(Math.min((double)physicalWidth/MIN_WIDTH,(double)physicalHeight/MIN_HEIGHT),Math.floor(guiScale));
        double whole=Math.floor(fit),pixels=whole>=1&&whole>=fit*(1-MAX_SNAP_SHRINK)?whole:fit;
        float scale=(float)(pixels/guiScale);
        int width=Math.max(MIN_WIDTH,(int)(physicalWidth/pixels)),height=Math.max(MIN_HEIGHT,(int)(physicalHeight/pixels));
        int sidebar = 230, tableX = sidebar + 12, rulesWidth = 284, rulesX = width - rulesWidth - 12;
        int footer = height - 94, priorityHeight = Math.clamp((footer - 180) / 3, 98, 168);
        int libraryY = 94 + priorityHeight + 70, libraryListY = libraryY + 44;
        return new WorkspaceLayout(scale, width, height, sidebar, tableX, rulesX - tableX - 12,
            rulesX, rulesWidth, priorityHeight, libraryY, libraryListY, footer - 32 - libraryListY,
            footer, 164, 182, footer - 190);
    }
}

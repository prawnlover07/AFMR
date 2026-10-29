package dev.animal.afmr.ui;

enum WorkspacePage {
    PLAYLISTS("Playlists",76), LIBRARY("Library",68), TIMING("Timing",64), DISPLAY("Display",68),
    KEYS("Shortcuts",80), INSPECTOR("Playback status",110), ACTIVITY("Queue / history",112), PROFILES("Profiles",72);
    final String label;
    final int width;
    WorkspacePage(String label,int width){this.label=label;this.width=width;}
}

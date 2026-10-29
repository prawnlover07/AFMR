package dev.animal.afmr.ui;

import java.util.*;

/** Small settings-only index. Never queries the music catalog or mutates settings. */
final class SettingsSearchIndex {
    enum Destination { TIMING, COMBAT, DEATH, WIDGET, MENUS, MENU_FONT, WIDGET_FONT, THEMES, MENU_IMAGE, WIDGET_IMAGE, PLAYLIST, RULE, TRANSITIONS, COLUMNS, SHORTCUTS, LIBRARY, PROFILES, BACKUP }
    record Entry(String label,String section,Destination destination,String control,String keywords){
        boolean matches(String query){String text=(label+" "+section+" "+keywords).toLowerCase(Locale.ROOT);for(String word:query.strip().toLowerCase(Locale.ROOT).split("\\s+"))if(!text.contains(word))return false;return true;}
    }
    private static final List<Entry> ENTRIES=entries();
    private static List<Entry> entries(){
        var rows=new ArrayList<Entry>();
        add(rows,"Shortest gap between songs","Timing",Destination.TIMING,"Min silence (sec)","silence minimum wait delay");
        add(rows,"Longest gap between songs","Timing",Destination.TIMING,"Max silence (sec)","silence maximum wait delay");
        add(rows,"Fade in","Timing",Destination.TIMING,"Fade in (sec)","start volume");
        add(rows,"Fade out","Timing",Destination.TIMING,"Fade out (sec)","end volume");
        add(rows,"Context buffer","Timing",Destination.TIMING,"Context buffer (sec)","delay condition switching");
        add(rows,"Play immediately on join","Timing",Destination.TIMING,"Play immediately on join","world server initial silence");
        add(rows,"Normalization","Timing",Destination.TIMING,"Normalization","replaygain loudness volume");
        add(rows,"Continue playing on match","Timing",Destination.TIMING,"Continue playing on match","interrupt conditions");
        add(rows,"Jukebox behavior","Timing",Destination.TIMING,"Jukebox","disc fade pause resume silence");
        add(rows,"Songs before silence","Timing",Destination.TIMING,"Songs before silence","groups count gap");
        for(String label:List.of("Combat music","On attack","On damage","Combatants only","Combat quiet period"))add(rows,label,"Timing / Combat",Destination.COMBAT,label,"combat fighting threat");
        for(String label:List.of("On death","Death fade seconds","Prevent new music while dead"))add(rows,label,"Timing / Combat / Death",Destination.DEATH,label,"respawn death combat");
        String[][] widget={{"Now playing","notification visibility"},{"Corner","position screen"},{"Style","toast card"},{"Delay seconds","delay popup"},{"Popup seconds","duration"},{"Scale percent","size"},{"Width","size"},{"Wrap text","text wrap"},{"Artwork / note","cover album"},{"Song title","metadata"},{"Artist","metadata"},{"Album","metadata"},{"Source playlist","metadata"},{"Scroll long text","marquee"},{"Background color","colour rgb"},{"Background opacity percent","transparency opacity"}};
        for(var item:widget)add(rows,item[0],"Display / Widget",Destination.WIDGET,item[0],"widget "+item[1]);
        add(rows,"Menu font","Display / Menus",Destination.MENU_FONT,"","text typeface ttf resource pack");
        add(rows,"Widget font","Display / Widget",Destination.WIDGET_FONT,"","text typeface ttf resource pack");
        add(rows,"Themes and colors","Display / Menus",Destination.THEMES,"","theme colour text accent palette opacity sharing import export");
        add(rows,"Menu opacity","Display / Menus",Destination.MENUS,"Background opacity","transparency background");
        add(rows,"Menu blur","Display / Menus",Destination.MENUS,"Blur background","pause effect");
        add(rows,"Menu background image","Display / Menus",Destination.MENU_IMAGE,"","gif webp png wallpaper");
        add(rows,"Widget background image","Display / Widget",Destination.WIDGET_IMAGE,"","gif webp png wallpaper");
        String[][] playlist={{"Play automatically","automatic enabled"},{"Set playback area here","home location zone radius"},{"Volume (%)","loudness"},{"Custom gap between songs","silence wait delay minimum maximum"},{"Songs before gap","group count"},{"Include Minecraft music","vanilla"},{"Let the previous song finish","finish entry interrupt"},{"Finish songs before switching away","finish exit interrupt"},{"Skip wait on automatic start","play immediately trigger"},{"Condition groups","nested rules any all not"}};
        for(var item:playlist)add(rows,item[0],"Selected playlist",Destination.PLAYLIST,item[0],item[1]);
        for(var option:ConditionOption.values())add(rows,option.label,"Selected playlist / Rules",Destination.RULE,option.name(),switch(option){case AREA->"home zone location radius world";case TIME->"clock ticks fit songs time window";case NEARBY_PLAYER->"proximity volume distance player";default->"condition filter";});
        add(rows,"Playlist switching","Selected playlist",Destination.TRANSITIONS,"","transition buffer fade silence finish");
        add(rows,"Song list columns","Playlists",Destination.COLUMNS,"","album artist track number replaygain metadata");
        add(rows,"Keyboard shortcuts","Shortcuts",Destination.SHORTCUTS,"","key binds override hotkeys controls");
        add(rows,"Music folders and rescanning","Library",Destination.LIBRARY,"","files rescan import sources repair missing");
        add(rows,"Profiles","Profiles",Destination.PROFILES,"","world server settings");
        add(rows,"Full configuration backup","Import / export",Destination.BACKUP,"","export import transfer all settings playlists");
        return List.copyOf(rows);
    }
    private static void add(List<Entry> rows,String label,String section,Destination destination,String control,String keywords){rows.add(new Entry(label,section,destination,control,keywords));}
    static List<Entry> search(String query){return ENTRIES.stream().filter(row->row.matches(query)).toList();}
}

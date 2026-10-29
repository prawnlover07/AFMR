package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;
import dev.animal.afmr.ui.MenuField;

final class SettingsSearchScreen extends MusicScreen {
    private final LibraryScreen editor;
    private String query="";
    private int selected;
    private java.util.List<SettingsSearchIndex.Entry> matches=SettingsSearchIndex.search("");
    private MenuField search;
    private DenseList<SettingsSearchIndex.Entry> results;
    SettingsSearchScreen(LibraryScreen editor,ClientRuntime runtime){super("Search settings",editor,runtime);this.editor=editor;}
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();search=edit(query,"Search settings",left,52,contentWidth,value->{query=value;selected=0;results.top();results.items(matches=SettingsSearchIndex.search(query));});
        results=addRenderableWidget(new DenseList<>(left,80,contentWidth,Math.max(14,bodyBottom()-120),(g,row,x,y,w,hover)->{
            if(hover||matches.indexOf(row)==selected)net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
            menuFont.draw(g,fit(row.section()+" / "+row.label()+" >",w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFEBEEF3));
        },(row,x,y,button,twice)->open(row),SettingsSearchIndex.Entry::label));
        results.items(matches=SettingsSearchIndex.search(query));button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);setInitialFocus(search);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN&&!matches.isEmpty()){open(matches.get(selected));return true;}
        if((key==com.mojang.blaze3d.platform.InputConstants.KEY_DOWN||key==com.mojang.blaze3d.platform.InputConstants.KEY_UP)&&!matches.isEmpty()){
            selected=Math.max(0,Math.min(matches.size()-1,selected+(key==com.mojang.blaze3d.platform.InputConstants.KEY_DOWN?1:-1)));results.reveal(matches.get(selected));return true;
        }return super.keyPressed(key,scan,modifiers);
    }
    private void open(SettingsSearchIndex.Entry entry){
        var playlist=runtime.library().playlist(editor.selectedPlaylistId());
        if(entry.destination()==SettingsSearchIndex.Destination.PLAYLIST){minecraft.setScreen(editor);editor.revealPlaylistSetting(entry.control());return;}
        if(entry.destination()==SettingsSearchIndex.Destination.RULE){if(playlist!=null){runtime.playlistEdits().watch(runtime.library(),playlist);minecraft.setScreen(ConditionEditors.create(ConditionOption.valueOf(entry.control()),editor,runtime,playlist.conditions));}return;}
        MusicScreen next=switch(entry.destination()){
            case TIMING->new SettingsScreen(editor,runtime);
            case COMBAT,DEATH->new CombatSettingsScreen(editor,runtime,entry.destination()==SettingsSearchIndex.Destination.DEATH);
            case WIDGET,MENUS->new NowPlayingSettingsScreen(editor,runtime,entry.destination()==SettingsSearchIndex.Destination.MENUS);
            case MENU_FONT->new FontSettingsScreen(editor,runtime,CustomFonts.Target.MENU);
            case WIDGET_FONT->new FontSettingsScreen(editor,runtime,CustomFonts.Target.WIDGET);
            case THEMES->new ThemesScreen(editor,runtime);
            case MENU_IMAGE->new CustomBackgroundScreen(editor,runtime,CustomBackground.Target.MENU);
            case WIDGET_IMAGE->new CustomBackgroundScreen(editor,runtime,CustomBackground.Target.WIDGET);
            case TRANSITIONS->playlist==null?null:new PlaylistTransitionsScreen(editor,runtime,playlist.id);
            case COLUMNS->new SongColumnsScreen(editor,runtime);
            case SHORTCUTS->new OverrideBindingsScreen(editor,runtime);
            case LIBRARY->new SourcesScreen(editor,runtime);
            case PROFILES->new ProfilesScreen(editor,runtime);
            case BACKUP->new ConfigBackupScreen(editor,runtime);
            default->throw new IllegalStateException();
        };
        if(next!=null){next.focusSetting(entry.control());minecraft.setScreen(next);}
    }
}

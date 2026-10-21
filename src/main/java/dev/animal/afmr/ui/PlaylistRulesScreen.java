package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class PlaylistRulesScreen extends MusicScreen {
    private final Playlist playlist;
    private final LibraryScreen editor;
    PlaylistRulesScreen(Screen parent,LibraryScreen editor,ClientRuntime runtime,Playlist playlist){super("Playlist settings",parent,runtime);this.editor=editor;this.playlist=playlist;}
    @Override protected void init(){
        super.init();
        int half=(contentWidth-6)/2;
        button("Automatic: "+(playlist.enabled?"on":"off"),left,47,half,()->{playlist.enabled=!playlist.enabled;changed();})
            .setTooltip(Tooltip.create(Component.literal("Controls automatic detection and playback. Turning this off still allows overrides and previews; individually disabled songs remain excluded from overrides.")));
        button("Manual only: "+(playlist.manualOnly?"on":"off"),left+half+6,47,half,()->{playlist.manualOnly=!playlist.manualOnly;changed();})
            .setTooltip(Tooltip.create(Component.literal("Exclude this playlist from automatic selection. You can still play it using overrides or manual previews.")));
        button("Finish current song on entry: "+(playlist.finishCurrentSong?"on":"off"),left,71,contentWidth,()->{playlist.finishCurrentSong=!playlist.finishCurrentSong;changed();})
            .setTooltip(Tooltip.create(Component.literal("When this playlist becomes active, let music already playing finish. Either entry or the song's source playlist's exit setting can keep it playing. Normal silence follows.")));
        button("Finish current song on exit: "+(playlist.finishCurrentSongOnExit?"on":"off"),left,95,contentWidth,()->{playlist.finishCurrentSongOnExit=!playlist.finishCurrentSongOnExit;changed();})
            .setTooltip(Tooltip.create(Component.literal("Let a song started by this playlist finish after another playlist becomes active, or no playlist matches. Combat death fades/cuts and manual skipping still take precedence.")));
        button("Vanilla songs: "+(playlist.includeVanilla?"automatic":"manual only"),left,119,contentWidth,()->{playlist.includeVanilla=!playlist.includeVanilla;changed();});
        button("View / change playlist priority...",left,143,contentWidth,()->{
            if(parent instanceof PlaylistManagerScreen manager){manager.select(playlist.id);minecraft.gui.setScreen(manager);}
            else minecraft.gui.setScreen(new PlaylistManagerScreen(editor,runtime,playlist.id));
        });
        if(playlist.custom())button("Name & conditions...",left,167,contentWidth,()->minecraft.gui.setScreen(new CustomConditionsScreen(this,runtime,playlist)));
        else if(playlist.id.equals("underground"))button("Underground conditions...",left,167,contentWidth,()->minecraft.gui.setScreen(new UndergroundSettingsScreen(this,runtime)));
        else if(playlist.id.equals("dark_exploration"))button("Darkness conditions...",left,167,contentWidth,()->minecraft.gui.setScreen(new DarknessSettingsScreen(this,runtime)));
        else if(playlist.id.equals("combat"))button("Combat settings...",left,167,contentWidth,()->minecraft.gui.setScreen(new CombatSettingsScreen(this,runtime)));
        else if(playlist.id.equals("home"))button("Home area...",left,167,contentWidth,()->minecraft.gui.setScreen(new HomeScreen(this,runtime)));
        button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    private void changed(){runtime.save();rebuildWidgets();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit("#"+(runtime.library().playlists.indexOf(playlist)+1)+" / "+playlist.name,contentWidth),left,height-48,0xFF8DE0BE);
    }
}

package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.client.gui.screens.Screen;

final class RenamePlaylistScreen extends MusicScreen {
    private final Playlist playlist;
    private MenuField name;
    RenamePlaylistScreen(Screen parent,ClientRuntime runtime,Playlist playlist){super("Rename playlist",parent,runtime);this.playlist=playlist;}
    @Override protected void init(){
        super.init();name=edit(playlist.name,"Playlist name",left,65,contentWidth,null);name.setMaxLength(80);setInitialFocus(name);
        var save=button("Rename",left,bodyBottom()-29,(contentWidth-6)/2,()->{
            if(name.getValue().isBlank())return;playlist.name=name.getValue().strip();runtime.save();super.onClose();
        });name.setResponder(value->save.active=!value.isBlank());
        button("Cancel",left+(contentWidth-6)/2+6,bodyBottom()-29,(contentWidth-6)/2,this::onClose);
    }
}

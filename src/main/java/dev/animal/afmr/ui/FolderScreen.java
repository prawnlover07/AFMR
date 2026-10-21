package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Folder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import java.nio.file.Path;
import java.util.List;

final class FolderScreen extends MusicScreen {
    private final Folder folder;
    FolderScreen(Screen parent,ClientRuntime runtime,Folder folder){super("Linked folder",parent,runtime);this.folder=folder;}
    @Override protected void init(){
        super.init();
        button("Include subfolders: "+(folder.recursive?"yes":"no"),left,86,contentWidth,()->{boolean value=!folder.recursive;runtime.library().folders.stream().filter(f->f.path.equals(folder.path)).forEach(f->f.recursive=value);runtime.save();rebuildWidgets();});
        button("Rescan this folder",left,112,contentWidth,()->runtime.importPaths(List.of(Path.of(folder.path)),folder.playlistId,folder.recursive)).active=!runtime.scanning();
        button("Unlink folder (keep songs)",left,138,contentWidth,()->{runtime.library().folders.removeIf(f->f.path.equals(folder.path));runtime.save();onClose();});
        button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit(folder.path,contentWidth),left,50,0xFF8DE0BE);
        var playlist=runtime.library().playlist(folder.playlistId);
        String target=folder.playlistId.equals("ambient")?"Day / night / underground (migrated link)":playlist==null?"Music library":playlist.name;
        g.text(font,fit("Rescan destination: "+target,contentWidth),left,65,0xFFD9E8E0);
        g.text(font,fit(runtime.status(),contentWidth),left,height-48,0xFFABC3B9);
    }
}

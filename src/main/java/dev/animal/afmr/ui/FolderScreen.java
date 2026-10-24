package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
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
        button("Rescan this folder",left,112,contentWidth,()->runtime.rescanFolder(folder)).active=!runtime.scanning();
        var unlinkFolderHelpControl=button("Unlink folder (keep songs)",left,138,contentWidth,()->{
            runtime.library().folders.removeIf(f->f.path.equals(folder.path));
            for(var p:runtime.library().profiles.inactive)p.folderDestinations.remove(folder.path);
            runtime.save();onClose();
        });
        var unlinkFolderHelpText=Component.translatable("afmr.copy.folder.unlink_folder");
        if(!unlinkFolderHelpText.getString().isBlank())unlinkFolderHelpControl.setTooltip(Tooltip.create(unlinkFolderHelpText));
        button("Locate moved folder...",left,164,contentWidth,()->{
            minecraft.gui.setScreen(new LibraryRepairScreen(this,runtime));
            NativePicker.open(runtime,NativePicker.Kind.REPAIR_FOLDER,folder.path);
        }).active=!runtime.scanning()&&runtime.pendingRepair()==null;
        button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit(folder.path,contentWidth),left,50,MenuAppearance.color(MenuColor.ACCENT,0xFF8DE0BE));
        var playlist=runtime.library().playlist(folder.playlistId);
        String target=folder.playlistId.equals("ambient")?"Day / night / underground (migrated link)":playlist==null?"Music library":playlist.name;
        g.text(font,fit("Rescan destination: "+target,contentWidth),left,65,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        g.text(font,fit(runtime.status(),contentWidth),left,height-48,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABC3B9));
    }
}

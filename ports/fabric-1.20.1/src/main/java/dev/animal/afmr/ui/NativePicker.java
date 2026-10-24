package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.PlaylistPresets;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class NativePicker {
    public enum Kind { FILES,FOLDER,PLAYLISTS,REPAIR_FOLDER,REPAIR_FILE,PRESET_IMPORT,PRESET_EXPORT,PRESET_FOLDER,BACKUP_IMPORT,BACKUP_EXPORT,BACKGROUND }
    private static boolean active;
    private static final ExecutorService PICKER=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"AFMR file picker");t.setDaemon(true);return t;});
    private NativePicker(){}
    public static void open(ClientRuntime runtime,Kind kind,String destination){show(runtime,kind,destination,null);}
    public static void savePreset(ClientRuntime runtime,PlaylistPresets.FileData snapshot){show(runtime,Kind.PRESET_EXPORT,"",snapshot);}
    private static void show(ClientRuntime runtime,Kind kind,String destination,PlaylistPresets.FileData preset){
        if(active){runtime.message("A file picker is already open.");return;}
        if(kind!=Kind.BACKGROUND&&(runtime.scanning()||runtime.pendingRepair()!=null)){runtime.message("Finish the current scan or repair first.");return;}
        active=true;
        String profile=runtime.library().profiles.activeId;
        var pending=runtime.pendingPreset();
        var client=Minecraft.getInstance();
        CompletableFuture.supplyAsync(()->select(kind),PICKER).whenComplete((paths,failure)->client.execute(()->{
            active=false;
            if(failure!=null){runtime.message("Native file picker unavailable: "+failure.getMessage());return;}
            if(paths.isEmpty()){runtime.message("File selection cancelled.");return;}
            if(kind==Kind.REPAIR_FOLDER||kind==Kind.REPAIR_FILE)
                runtime.prepareRepair(new dev.animal.afmr.library.LibraryRepair.Request(kind==Kind.REPAIR_FOLDER,destination,paths.get(0)));
            else if(kind==Kind.PRESET_FOLDER){if(runtime.pendingPreset()==pending)runtime.locatePresetFolder(destination,paths.get(0));}
            else if(kind==Kind.PRESET_IMPORT)runtime.preparePresetImport(paths.get(0),profile,destination);
            else if(kind==Kind.PRESET_EXPORT)runtime.exportPresets(paths.get(0),preset);
            else if(kind==Kind.BACKUP_IMPORT)runtime.prepareBackupImport(paths.get(0));
                    else if(kind==Kind.BACKUP_EXPORT)runtime.exportBackup(paths.get(0));
                    else if(kind==Kind.BACKGROUND)runtime.menuBackground().choose(paths.get(0));
            else if(kind==Kind.PLAYLISTS)runtime.importPlaylists(paths,profile,destination);
            else runtime.importPaths(paths,profile,destination,true);
        }));
    }
    private static List<Path> select(Kind kind){
        String initial=System.getProperty("user.home")+java.io.File.separator;
        String appData=System.getenv("APPDATA");
        if(kind==Kind.PLAYLISTS&&appData!=null){var foobar=Path.of(appData,"foobar2000-v2","playlists-v2.0");if(Files.isDirectory(foobar))initial=foobar.toString()+java.io.File.separator;}
        String selected;
        if(kind==Kind.FOLDER||kind==Kind.REPAIR_FOLDER||kind==Kind.PRESET_FOLDER)selected=TinyFileDialogs.tinyfd_selectFolderDialog("Choose music folder",initial);
        else try(var stack=MemoryStack.stackPush()){
            boolean setup=kind==Kind.PRESET_IMPORT||kind==Kind.PRESET_EXPORT;
            boolean backup=kind==Kind.BACKUP_IMPORT||kind==Kind.BACKUP_EXPORT;
            String[] patterns=backup?new String[]{"*.afmrb"}:kind==Kind.BACKGROUND?new String[]{"*.png","*.jpg","*.jpeg","*.webp","*.gif"}:setup?new String[]{"*.afmrp"}:kind==Kind.PLAYLISTS?new String[]{"*.fplite"}:new String[]{"*.mp3","*.flac","*.ogg","*.wav"};
            var filters=stack.mallocPointer(patterns.length);for(var pattern:patterns)filters.put(stack.UTF8(pattern));filters.flip();
            String title=backup?"AFMR full configuration backup":kind==Kind.BACKGROUND?"Choose background image":setup?"AFMR playlist setup":kind==Kind.PLAYLISTS?"foobar2000 playlists":"Choose music";
            if(kind==Kind.BACKUP_EXPORT)selected=TinyFileDialogs.tinyfd_saveFileDialog(title,initial+"afmr-backup.afmrb",filters,title);
            else if(kind==Kind.PRESET_EXPORT)selected=TinyFileDialogs.tinyfd_saveFileDialog(title,initial+"playlists.afmrp",filters,"AFMR playlist setup");
            else selected=TinyFileDialogs.tinyfd_openFileDialog(title,initial,filters,title,kind==Kind.FILES||kind==Kind.PLAYLISTS);
        }
        if(selected==null||selected.isBlank())return List.of();
        return Arrays.stream(selected.split("\\|",-1)).map(Path::of).toList();
    }
}

package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.library.PlaylistPresets;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import static org.lwjgl.sdl.SDLDialog.*;

/** Uses SDL's Windows Explorer dialogs; no AWT or in-game filesystem browser. */
public final class NativePicker {
    public enum Kind { FILES, FOLDER, PLAYLISTS, REPAIR_FOLDER, REPAIR_FILE, PRESET_IMPORT, PRESET_EXPORT, PRESET_FOLDER, BACKUP_IMPORT, BACKUP_EXPORT, BACKGROUND }
    private static NativePicker active;
    private SDL_DialogFileCallback callback;
    private SDL_DialogFileFilter.Buffer filters;
    private ByteBuffer name,pattern,location;
    private final ClientRuntime runtime;
    private PlaylistPresets.FileData preset;
    private NativePicker(ClientRuntime runtime){this.runtime=runtime;}
    public static void open(ClientRuntime runtime,Kind kind,String destination){
        if(active!=null){runtime.message("A file picker is already open.");return;}
        if(kind!=Kind.BACKGROUND&&runtime.scanning()){runtime.message("Wait for the current scan to finish.");return;}
        if(kind!=Kind.BACKGROUND&&runtime.pendingRepair()!=null){runtime.message("Apply or cancel the pending repair first.");return;}
        active=new NativePicker(runtime);active.show(kind,destination);
    }
    public static void savePreset(ClientRuntime runtime,PlaylistPresets.FileData snapshot){
        if(active!=null){runtime.message("A file picker is already open.");return;}
        if(runtime.scanning()||runtime.pendingRepair()!=null){runtime.message("Finish the current scan or repair first.");return;}
        active=new NativePicker(runtime);active.preset=snapshot;active.show(Kind.PRESET_EXPORT,"");
    }
    private void show(Kind kind,String destination){
        String profileId=runtime.library().profiles.activeId;
        var pending=runtime.pendingPreset();
        Minecraft mc=Minecraft.getInstance();
        String initial=System.getProperty("user.home"),appData=System.getenv("APPDATA");
        if(kind==Kind.PLAYLISTS&&appData!=null){
            Path foobar=Path.of(appData,"foobar2000-v2","playlists-v2.0");
            if(Files.isDirectory(foobar))initial=foobar.toString();
        }
        location=MemoryUtil.memUTF8(initial+java.io.File.separator+(kind==Kind.BACKUP_EXPORT?"afmr-backup.afmrb":kind==Kind.PRESET_EXPORT?"playlists.afmrp":""));
        callback=SDL_DialogFileCallback.create((unused,files,filter)->{
            List<Path> paths=new ArrayList<>();String failure="";
            try{
                if(files==0)failure="Could not open file picker: "+SDLError.SDL_GetError();
                else for(int i=0;;i++){
                    long item=MemoryUtil.memGetAddress(files+(long)i*org.lwjgl.system.Pointer.POINTER_SIZE);if(item==0)break;
                    paths.add(Path.of(MemoryUtil.memUTF8(item)));
                }
            }catch(Exception e){failure="File selection failed: "+e.getMessage();}
            String error=failure;
            mc.schedule(()->{
                close();
                if(!error.isBlank())runtime.message(error);
                else if(!paths.isEmpty()){
                    if(kind==Kind.REPAIR_FOLDER||kind==Kind.REPAIR_FILE)
                        runtime.prepareRepair(new dev.animal.afmr.library.LibraryRepair.Request(kind==Kind.REPAIR_FOLDER,destination,paths.getFirst()));
                    else if(kind==Kind.PRESET_FOLDER){if(runtime.pendingPreset()==pending)runtime.locatePresetFolder(destination,paths.getFirst());}
                    else if(kind==Kind.PRESET_IMPORT)runtime.preparePresetImport(paths.getFirst(),profileId,destination);
                    else if(kind==Kind.PRESET_EXPORT)runtime.exportPresets(paths.getFirst(),preset);
                    else if(kind==Kind.BACKUP_IMPORT)runtime.prepareBackupImport(paths.getFirst());
                    else if(kind==Kind.BACKUP_EXPORT)runtime.exportBackup(paths.getFirst());
                    else if(kind==Kind.BACKGROUND)runtime.menuBackground().choose(paths.getFirst());
                    else if(kind==Kind.PLAYLISTS)runtime.importPlaylists(paths,profileId,destination);
                    else runtime.importPaths(paths,profileId,destination,true);
                }
                else runtime.message("File selection cancelled.");
            });
        });
        try{
            if(kind==Kind.FOLDER||kind==Kind.REPAIR_FOLDER||kind==Kind.PRESET_FOLDER)SDL_ShowOpenFolderDialog(callback,0,mc.getWindow().handle(),location,false);
            else{
                boolean backup=kind==Kind.BACKUP_IMPORT||kind==Kind.BACKUP_EXPORT;
                boolean setup=kind==Kind.PRESET_IMPORT||kind==Kind.PRESET_EXPORT;
                name=MemoryUtil.memUTF8(backup?"AFMR full backup (*.afmrb)":kind==Kind.BACKGROUND?"Background image (PNG, JPEG, WebP, GIF)":setup?"AFMR playlist setup (*.afmrp)":kind==Kind.PLAYLISTS?"foobar2000 playlists (*.fplite)":"Music (MP3, FLAC, OGG, WAV)");
                pattern=MemoryUtil.memUTF8(backup?"afmrb":kind==Kind.BACKGROUND?"png;jpg;jpeg;webp;gif":setup?"afmrp":kind==Kind.PLAYLISTS?"fplite":"mp3;flac;ogg;wav");
                filters=SDL_DialogFileFilter.calloc(1);filters.get(0).set(name,pattern);
                if(kind==Kind.PRESET_EXPORT||kind==Kind.BACKUP_EXPORT)SDL_ShowSaveFileDialog(callback,0,mc.getWindow().handle(),filters,location);
                else SDL_ShowOpenFileDialog(callback,0,mc.getWindow().handle(),filters,location,kind!=Kind.REPAIR_FILE&&kind!=Kind.PRESET_IMPORT&&kind!=Kind.BACKGROUND&&kind!=Kind.BACKUP_IMPORT);
            }
        }catch(Throwable error){close();runtime.message("Native file picker unavailable: "+error.getMessage());}
    }
    private void close(){
        if(callback!=null)callback.free();if(filters!=null)filters.free();
        MemoryUtil.memFree(name);MemoryUtil.memFree(pattern);MemoryUtil.memFree(location);active=null;
    }
}

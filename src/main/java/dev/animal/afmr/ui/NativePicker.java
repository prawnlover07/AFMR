package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
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
    public enum Kind { FILES, FOLDER, PLAYLISTS }
    private static NativePicker active;
    private SDL_DialogFileCallback callback;
    private SDL_DialogFileFilter.Buffer filters;
    private ByteBuffer name,pattern,location;
    private final ClientRuntime runtime;
    private NativePicker(ClientRuntime runtime){this.runtime=runtime;}
    public static void open(ClientRuntime runtime,Kind kind,String destination){
        if(active!=null){runtime.message("A file picker is already open.");return;}
        if(runtime.scanning()){runtime.message("Wait for the current scan to finish.");return;}
        active=new NativePicker(runtime);active.show(kind,destination);
    }
    private void show(Kind kind,String destination){
        Minecraft mc=Minecraft.getInstance();
        String initial=System.getProperty("user.home"),appData=System.getenv("APPDATA");
        if(kind==Kind.PLAYLISTS&&appData!=null){
            Path foobar=Path.of(appData,"foobar2000-v2","playlists-v2.0");
            if(Files.isDirectory(foobar))initial=foobar.toString();
        }
        location=MemoryUtil.memUTF8(initial+java.io.File.separator);
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
                    if(kind==Kind.PLAYLISTS)runtime.importPlaylists(paths,destination);
                    else runtime.importPaths(paths,destination,true);
                }
                else runtime.message("File selection cancelled.");
            });
        });
        try{
            if(kind==Kind.FOLDER)SDL_ShowOpenFolderDialog(callback,0,mc.getWindow().handle(),location,false);
            else{
                name=MemoryUtil.memUTF8(kind==Kind.PLAYLISTS?"foobar2000 playlists (*.fplite)":"Music (MP3, FLAC, OGG, WAV)");
                pattern=MemoryUtil.memUTF8(kind==Kind.PLAYLISTS?"fplite":"mp3;flac;ogg;wav");
                filters=SDL_DialogFileFilter.calloc(1);filters.get(0).set(name,pattern);
                SDL_ShowOpenFileDialog(callback,0,mc.getWindow().handle(),filters,location,true);
            }
        }catch(Throwable error){close();runtime.message("Native file picker unavailable: "+error.getMessage());}
    }
    private void close(){
        if(callback!=null)callback.free();if(filters!=null)filters.free();
        MemoryUtil.memFree(name);MemoryUtil.memFree(pattern);MemoryUtil.memFree(location);active=null;
    }
}

package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.ColorTheme;
import dev.animal.afmr.core.ThemePreview;
import dev.animal.afmr.library.ThemeFiles;
import net.minecraft.client.Minecraft;
import java.nio.file.Path;
import java.util.concurrent.*;

/** Client-owned mutations; one bounded file operation owns its isolated snapshot. */
public final class Themes implements AutoCloseable {
    private final ClientRuntime runtime;
    private boolean busy,closed;
    private int revision;
    private ThemePreview preview;
    private boolean suspended;
    private String status="",importedId="";
    public Themes(ClientRuntime runtime){this.runtime=runtime;}
    public boolean busy(){return busy;}
    public int revision(){return revision;}
    public String status(){return busy?"Working…":status;}
    public String importedId(){return importedId;}
    private boolean available(){if(closed||busy||!runtime.enabled()||runtime.backupBusy()||runtime.backupQueued()){status="Finish the current theme or backup operation first.";return false;}return true;}
    public void beginPreview(){preview=new ThemePreview(runtime.library().settings);suspended=false;}
    public ColorTheme preview(){return suspended||preview==null?null:preview.draft();}
    public ColorTheme draft(){if(preview==null)beginPreview();return preview.draft();}
    public void restoreDraft(ColorTheme value){if(preview==null)beginPreview();preview.choose(value);revision++;}
    public void suspendPreview(boolean value){suspended=value;}
    public void cancelPreview(){preview=null;suspended=false;revision++;}
    public ColorTheme snapshot(String name){return preview==null?ColorTheme.capture(name,runtime.library().settings):preview.snapshot(name);}
    public boolean previewTheme(String id){
        if(!available())return false;var theme=ColorTheme.find(runtime.library().settings,id);if(theme==null)return false;
        if(preview==null)beginPreview();preview.choose(theme);status="";revision++;return true;
    }
    public boolean commitPreview(String id){
        if(!available())return false;if(preview==null)return true;
        try{preview.commit(runtime.library().settings,id);preview=null;suspended=false;runtime.save();status="";revision++;return true;}
        catch(IllegalArgumentException ex){status=ex.getMessage();return false;}
    }
    public String saveCurrent(String name){
        if(!available())return "";var settings=runtime.library().settings;
        if(settings.savedThemes.size()>=ColorTheme.MAX_SAVED){status="Saved theme limit reached (128).";return "";}
        try{var theme=snapshot(ColorTheme.uniqueName(settings,name,""));settings.savedThemes.add(theme);runtime.save();revision++;status="Theme saved.";return theme.id;}
        catch(IllegalArgumentException ex){status=ex.getMessage();return "";}
    }
    public boolean rename(String id,String name){
        if(!available()||ColorTheme.GRAPHITE.equals(id))return false;var theme=ColorTheme.find(runtime.library().settings,id);if(theme==null)return false;
        try{theme.name=ColorTheme.uniqueName(runtime.library().settings,name,id);runtime.save();revision++;status="";return true;}
        catch(IllegalArgumentException ex){status=ex.getMessage();return false;}
    }
    public void delete(String id){if(available()&&!ColorTheme.GRAPHITE.equals(id)){runtime.library().settings.savedThemes.removeIf(theme->theme.id.equals(id));runtime.save();revision++;status="Theme removed.";}}
    public void importFile(Path path){
        if(!available())return;if(runtime.library().settings.savedThemes.size()>=ColorTheme.MAX_SAVED){status="Saved theme limit reached (128).";return;}
        busy=true;status="";
        CompletableFuture.supplyAsync(()->{try{return ThemeFiles.read(path);}catch(Exception ex){throw new CompletionException(ex);}}).whenComplete((theme,error)->Minecraft.getInstance().execute(()->{
            busy=false;if(closed)return;
            if(error!=null){status="Could not import theme: "+error.getCause().getMessage();revision++;return;}
            var settings=runtime.library().settings;if(settings.savedThemes.size()>=ColorTheme.MAX_SAVED){status="Saved theme limit reached (128).";revision++;return;}
            theme.name=ColorTheme.uniqueName(settings,theme.name,"");settings.savedThemes.add(theme);importedId=theme.id;runtime.save();status="Theme imported.";revision++;
        }));
    }
    public void exportFile(Path path,ColorTheme snapshot){
        if(!available())return;ColorTheme owned=snapshot.copy();busy=true;status="";
        CompletableFuture.runAsync(()->{try{ThemeFiles.write(path,owned);}catch(Exception ex){throw new CompletionException(ex);}}).whenComplete((unused,error)->Minecraft.getInstance().execute(()->{
            busy=false;if(closed)return;status=error==null?"Theme exported.":"Could not export theme: "+error.getCause().getMessage();revision++;
        }));
    }
    @Override public void close(){closed=true;preview=null;}
}

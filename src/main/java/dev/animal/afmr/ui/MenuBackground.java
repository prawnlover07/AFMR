package dev.animal.afmr.ui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuImageLayout;
import dev.animal.afmr.core.MenuImageAnimation;
import dev.animal.afmr.library.MenuBackgroundImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Client owns the texture and settings; one worker job owns all file/image preparation. */
public final class MenuBackground implements AutoCloseable {
    private final ClientRuntime runtime;
    private final Path file;
    private final Minecraft mc=Minecraft.getInstance();
    private boolean busy,attempted,uploaded,closed;
    private long enabledRevision;
    private int imageWidth,imageHeight,viewportWidth,viewportHeight;
    private MenuImageLayout.Fit fit;
    private MenuImageLayout layout;
    private String status="";
    private Identifier[] textures=new Identifier[0];
    private MenuImageAnimation animation;
    private long animationStart;

    public MenuBackground(ClientRuntime runtime,Path file){this.runtime=runtime;this.file=file;}
    public boolean busy(){return busy;}
    public String status(){return busy?"Preparing background image…":status;}

    public void choose(Path source){
        if(busy||closed||runtime.backupBusy()||!runtime.enabled())return;
        prepare(source,true);
    }
    private void prepare(Path source,boolean importing){
        busy=true;attempted=true;status="";
        long requestedRevision=enabledRevision;
        CompletableFuture.supplyAsync(()->{
            try{return importing?MenuBackgroundImage.install(source,file):MenuBackgroundImage.read(MenuBackgroundImage.installed(file));}
            catch(Exception e){throw new CompletionException(e);}
        }).whenComplete((image,failure)->mc.execute(()->{
            busy=false;if(closed)return;
            if(failure!=null){
                Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();
                status="Background unavailable: "+cause.getMessage();runtime.message(status);return;
            }
            if(importing){
                var s=runtime.library().settings;
                s.menuImageName=source.getFileName().toString();
                // A reset/off action after the picker must survive the worker finishing.
                if(requestedRevision==enabledRevision)s.menuImageEnabled=true;
                runtime.save();
            }
            if(runtime.library().settings.menuImageEnabled)upload(image);
        }));
    }
    private void upload(MenuBackgroundImage.Prepared prepared){
        NativeImage image=null;DynamicTexture texture=null;
        release();textures=new Identifier[prepared.frames().length];
        try{
            int[] durations=new int[textures.length];
            for(int i=0;i<textures.length;i++){
                var frame=prepared.frames()[i];durations[i]=frame.millis();
                // The stream API also avoids 1.20.1's small LWJGL byte[] scratch stack.
                image=NativeImage.read(new ByteArrayInputStream(frame.png()));
                texture=new DynamicTexture(()->"AFMR menu background",image);image=null;
                var id=Identifier.fromNamespaceAndPath("afmr","menu_background/"+i);
                mc.getTextureManager().register(id,texture);textures[i]=id;texture=null;
            }
            animation=new MenuImageAnimation(durations);animationStart=System.nanoTime();
            imageWidth=prepared.width();imageHeight=prepared.height();uploaded=true;layout=null;status="";
        }catch(Exception e){release();status="Background unavailable: "+e.getMessage();runtime.message(status);}
        finally{if(texture!=null)texture.close();if(image!=null)image.close();}
    }
    public void enabled(boolean enabled){
        enabledRevision++;runtime.library().settings.menuImageEnabled=enabled;
        if(!enabled)release();else attempted=false;
        runtime.save();
    }
    public void remove(){
        if(busy||closed||runtime.backupBusy())return;
        enabled(false);busy=true;
        CompletableFuture.runAsync(()->{
            try{Files.deleteIfExists(MenuBackgroundImage.animationFile(file));Files.deleteIfExists(file);}catch(Exception e){throw new CompletionException(e);}
        }).whenComplete((unused,failure)->mc.execute(()->{
            busy=false;if(closed)return;
            if(failure==null){runtime.library().settings.menuImageName="";status="";runtime.save();}
            else status="Could not remove AFMR's image copy: "+failure.getCause().getMessage();
        }));
    }
    /** Resource reload invalidates the texture once; it is recreated only when a menu needs it. */
    public void invalidate(){release();attempted=false;}
    private void release(){
        for(var id:textures)if(id!=null)mc.getTextureManager().release(id);
        textures=new Identifier[0];animation=null;uploaded=false;
        layout=null;
    }
    public boolean draw(GuiGraphicsExtractor g,int width,int height){
        var s=runtime.library().settings;
        if(closed||!runtime.enabled()||!s.menuImageEnabled||s.menuImageName.isBlank())return false;
        if(!uploaded){if(!attempted&&!busy)prepare(file,false);return false;}
        if(s.menuImageOpacity<=0)return false;
        if(layout==null||viewportWidth!=width||viewportHeight!=height||fit!=s.menuImageFit){
            viewportWidth=width;viewportHeight=height;fit=s.menuImageFit;
            layout=MenuImageLayout.calculate(imageWidth,imageHeight,width,height,fit);
        }
        var r=layout;
        int tint=((int)Math.round(s.menuImageOpacity*255)<<24)|0xFFFFFF;
        var texture=textures[animation.frame((System.nanoTime()-animationStart)/1_000_000)];
        g.blit(RenderPipelines.GUI_TEXTURED,texture,r.x(),r.y(),r.u(),r.v(),r.width(),r.height(),
            r.sourceWidth(),r.sourceHeight(),imageWidth,imageHeight,tint);
        return true;
    }
    @Override public void close(){closed=true;release();}
}

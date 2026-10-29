package dev.animal.afmr.ui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuImageLayout;
import dev.animal.afmr.core.MenuImageAnimation;
import dev.animal.afmr.library.MenuBackgroundImage;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Client owns the texture and settings; one worker job owns all file/image preparation. */
public final class CustomBackground implements AutoCloseable {
    private final ClientRuntime runtime;
    public enum Target { MENU, WIDGET }
    private final Target target;
    private final Path file;
    private final Minecraft mc=Minecraft.getInstance();
    private boolean busy,attempted,uploaded,closed;
    private long enabledRevision;
    private int imageWidth,imageHeight,viewportWidth,viewportHeight;
    private MenuImageLayout.Fit fit;
    private MenuImageLayout layout;
    private String status="";
    private ResourceLocation[] textures=new ResourceLocation[0];
    private MenuImageAnimation animation;
    private long animationStart;

    public CustomBackground(ClientRuntime runtime,Path file,Target target){this.runtime=runtime;this.file=file;this.target=target;}
    public boolean enabled(){var s=runtime.library().settings;return target==Target.MENU?s.menuImageEnabled:s.widgetImageEnabled;}
    public String name(){var s=runtime.library().settings;return target==Target.MENU?s.menuImageName:s.widgetImageName;}
    public MenuImageLayout.Fit fit(){var s=runtime.library().settings;return target==Target.MENU?s.menuImageFit:s.widgetImageFit;}
    public double opacity(){var s=runtime.library().settings;return target==Target.MENU?s.menuImageOpacity:s.widgetImageOpacity;}
    public void fit(MenuImageLayout.Fit fit){var s=runtime.library().settings;if(target==Target.MENU)s.menuImageFit=fit;else s.widgetImageFit=fit;}
    public void opacity(double opacity){var s=runtime.library().settings;if(target==Target.MENU)s.menuImageOpacity=opacity;else s.widgetImageOpacity=opacity;}
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
                if(target==Target.MENU)s.menuImageName=source.getFileName().toString();
                else s.widgetImageName=source.getFileName().toString();
                // A reset/off action after the picker must survive the worker finishing.
                if(requestedRevision==enabledRevision){if(target==Target.MENU)s.menuImageEnabled=true;else s.widgetImageEnabled=true;}
                runtime.save();
            }
            if(enabled())upload(image);
        }));
    }
    private void upload(MenuBackgroundImage.Prepared prepared){
        NativeImage image=null;DynamicTexture texture=null;
        release();textures=new ResourceLocation[prepared.frames().length];
        try{
            int[] durations=new int[textures.length];
            for(int i=0;i<textures.length;i++){
                var frame=prepared.frames()[i];durations[i]=frame.millis();
                image=NativeImage.read(new ByteArrayInputStream(frame.png()));
                texture=new DynamicTexture(image);image=null;
                var id=new ResourceLocation("afmr",target.name().toLowerCase(java.util.Locale.ROOT)+"_background/"+i);
                mc.getTextureManager().register(id,texture);textures[i]=id;texture=null;
            }
            animation=new MenuImageAnimation(durations);animationStart=System.nanoTime();
            imageWidth=prepared.width();imageHeight=prepared.height();uploaded=true;layout=null;status="";
        }catch(Exception e){release();status="Background unavailable: "+e.getMessage();runtime.message(status);}
        finally{if(texture!=null)texture.close();if(image!=null)image.close();}
    }
    public void enabled(boolean enabled){
        enabledRevision++;var s=runtime.library().settings;
        if(target==Target.MENU)s.menuImageEnabled=enabled;else s.widgetImageEnabled=enabled;
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
            if(failure==null){var s=runtime.library().settings;if(target==Target.MENU)s.menuImageName="";else s.widgetImageName="";status="";runtime.save();}
            else status="Could not remove AFMR's image copy: "+failure.getCause().getMessage();
        }));
    }
    /** Resource reload invalidates the texture once; it is recreated only when the background is drawn. */
    public void invalidate(){release();attempted=false;}
    private void release(){
        for(var id:textures)if(id!=null)mc.getTextureManager().release(id);
        textures=new ResourceLocation[0];animation=null;uploaded=false;
        layout=null;
    }
    public boolean draw(PoseStack g,int width,int height,float alpha){
        if(closed||!runtime.enabled()||!enabled()||name().isBlank())return false;
        if(!uploaded){if(!attempted&&!busy)prepare(file,false);return false;}
        double opacity=opacity()*alpha;
        if(opacity<=0)return target==Target.WIDGET;
        if(layout==null||viewportWidth!=width||viewportHeight!=height||fit!=fit()){
            viewportWidth=width;viewportHeight=height;fit=fit();
            layout=MenuImageLayout.calculate(imageWidth,imageHeight,width,height,fit);
        }
        var r=layout;
        RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,(float)opacity);
        var texture=textures[animation.frame((System.nanoTime()-animationStart)/1_000_000)];
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,texture);net.minecraft.client.gui.GuiComponent.blit(g,r.x(),r.y(),r.width(),r.height(),r.u(),r.v(),r.sourceWidth(),r.sourceHeight(),imageWidth,imageHeight);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();
        return true;
    }
    @Override public void close(){closed=true;release();}
}

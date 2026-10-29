package dev.animal.afmr.ui;
import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.library.MusicFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.concurrent.CompletableFuture;
import java.io.ByteArrayInputStream;
final class EditorArtwork {
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("afmr","editor_cover");
    private Track track;private long generation;private boolean loaded;
    void track(Track next){
        if(next==track)return;close();track=next;if(next==null)return;long token=generation;
        CompletableFuture.supplyAsync(()->MusicFiles.cover(next)).thenAccept(data->Minecraft.getInstance().execute(()->{
            if(token!=generation||data==null)return;
            NativeImage image=null;DynamicTexture texture=null;boolean registered=false;
            try{
                // The byte[] overload can overflow the 1.20.1 LWJGL scratch stack.
                image=NativeImage.read(new ByteArrayInputStream(data));texture=new DynamicTexture(image);
                Minecraft.getInstance().getTextureManager().register(ID,texture);registered=true;loaded=true;
            }catch(Exception ignored){}
            finally{if(!registered){if(texture!=null)texture.close();else if(image!=null)image.close();}}
        }));
    }
    void draw(GuiGraphics g,int x,int y,int size){if(loaded)g.blit(ID,x,y,size,size,0,0,256,256,256,256);else g.drawString(MenuAppearance.font(),"♪",x+size/2-3,y+size/2-4,0xFF8DE0BE,false);}
    void close(){generation++;track=null;if(loaded){Minecraft.getInstance().getTextureManager().release(ID);loaded=false;}}
}

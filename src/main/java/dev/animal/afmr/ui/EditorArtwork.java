package dev.animal.afmr.ui;
import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.library.MusicFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.concurrent.CompletableFuture;
final class EditorArtwork {
    private static final Identifier ID=Identifier.fromNamespaceAndPath("afmr","editor_cover");
    private Track track;private long generation;private boolean loaded;
    void track(Track next){
        if(next==track)return;close();track=next;if(next==null)return;long token=generation;
        CompletableFuture.supplyAsync(()->MusicFiles.cover(next)).thenAccept(data->Minecraft.getInstance().execute(()->{
            if(token!=generation||data==null)return;
            try{Minecraft.getInstance().getTextureManager().register(ID,new DynamicTexture(()->"AFMR selected cover",NativeImage.read(data)));loaded=true;}catch(Exception ignored){}
        }));
    }
    void draw(GuiGraphicsExtractor g,int x,int y,int size){if(loaded)g.blit(RenderPipelines.GUI_TEXTURED,ID,x,y,0,0,size,size,256,256,256,256);else g.text(Minecraft.getInstance().font,"♪",x+size/2-3,y+size/2-4,0xFF8DE0BE,false);}
    void close(){generation++;track=null;if(loaded){Minecraft.getInstance().getTextureManager().release(ID);loaded=false;}}
}

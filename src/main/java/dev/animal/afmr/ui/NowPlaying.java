package dev.animal.afmr.ui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.library.MusicFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.util.concurrent.CompletableFuture;

public final class NowPlaying {
    private static final Identifier COVER=Identifier.fromNamespaceAndPath("afmr","now_playing_cover");
    private final ClientRuntime runtime;
    private final Minecraft mc=Minecraft.getInstance();
    private Track track;
    private double started;
    private long generation;
    private boolean hasCover;
    public NowPlaying(ClientRuntime runtime){this.runtime=runtime;}
    public void tick(){
        Track current=runtime.engine().current();
        if(current==track)return;
        clear();track=current;if(track==null)return;
        started=runtime.now();long token=generation;
        CompletableFuture.supplyAsync(()->MusicFiles.cover(current)).thenAccept(data->mc.execute(()->{
            if(token!=generation||data==null)return;
            try{
                var image=NativeImage.read(data);
                mc.getTextureManager().register(COVER,new DynamicTexture(()->"AFMR album cover",image));hasCover=true;
            }catch(Exception e){dev.animal.afmr.Afmr.LOG.debug("Album artwork unavailable",e);}
        }));
    }
    public void clear(){
        generation++;track=null;
        if(hasCover){mc.getTextureManager().release(COVER);hasCover=false;}
    }
    private static int color(int rgb,float alpha){return ((int)(255*alpha)<<24)|(rgb&0xFFFFFF);}
    public void draw(GuiGraphicsExtractor g){
        if(track==null||!runtime.library().settings.nowPlaying||mc.gui.hud.isHidden()||mc.level==null)return;
        double age=runtime.now()-started,duration=runtime.library().settings.popupSeconds;
        if(age>=duration)return;
        float alpha=(float)Math.min(1,Math.min(age/0.6,(duration-age)/0.8));if(alpha<=0.02)return;
        int w=Math.min(260,g.guiWidth()-16),x=8;
        // Small GUI sizes leave no room beside the centered hotbar; lift the card above it.
        int y=g.guiHeight()-(w+16>(g.guiWidth()-182)/2?112:66);
        g.fill(x,y,x+w,y+54,color(0x111D22,alpha*0.92f));
        g.fill(x,y,x+2,y+54,color(0x8DE0BE,alpha));
        if(hasCover)g.blit(RenderPipelines.GUI_TEXTURED,COVER,x+7,y+7,0,0,40,40,256,256,256,256,color(0xFFFFFF,alpha));
        else{
            g.fill(x+7,y+7,x+47,y+47,color(0x30473F,alpha));
            g.text(mc.font,"♪",x+21,y+21,color(0x8DE0BE,alpha));
        }
        g.text(mc.font,"NOW PLAYING",x+55,y+7,color(0x8DE0BE,alpha));
        g.text(mc.font,mc.font.plainSubstrByWidth(track.title,w-63),x+55,y+21,color(0xF0F7F3,alpha));
        String subtitle=track.artist.isBlank()?(track.album.isBlank()?"Local music":track.album):track.artist;
        g.text(mc.font,mc.font.plainSubstrByWidth(subtitle,w-63),x+55,y+35,color(0xB4C9BF,alpha));
    }
}

package dev.animal.afmr.probe;

import com.mojang.blaze3d.platform.NativeImage;
import dev.animal.afmr.library.MenuBackgroundImage;
import org.lwjgl.system.MemoryStack;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Random;
import javax.imageio.ImageIO;

/** Exercises prepared wallpapers through the actual native decoder, without a game window. */
public final class MenuBackgroundProbe {
    private static int passed;
    private static void check(boolean okay,String name){
        if(!okay)throw new AssertionError(name);passed++;System.out.println("PASS "+name);
    }
    public static void main(String[] args)throws Exception{
        Path directory=Files.createTempDirectory("afmr-background-probe-");
        try{
            var input=new BufferedImage(4096,1024,BufferedImage.TYPE_INT_ARGB);
            var random=new Random(37471);
            for(int y=0;y<input.getHeight();y++)for(int x=0;x<input.getWidth();x++)input.setRGB(x,y,0xff000000|random.nextInt(0x1000000));
            Path original=directory.resolve("wallpaper.png"),copy=directory.resolve("config/menu-background.png");
            ImageIO.write(input,"png",original.toFile());input.flush();byte[] before=Files.readAllBytes(original);
            var prepared=MenuBackgroundImage.install(original,copy);
            check(java.util.Arrays.equals(before,Files.readAllBytes(original)),"selected source remains unchanged");
            check(prepared.width()==2048&&prepared.height()==512,"large wallpaper is bounded to 2048 pixels with its aspect ratio");
            var loaded=MenuBackgroundImage.read(copy);
            check(loaded.width()==2048&&loaded.height()==512,"managed copy reloads after restart");
            int pointer=MemoryStack.stackGet().getPointer(),frame=MemoryStack.stackGet().getFrameIndex();
            check(loaded.frames()[0].png().length>pointer,"wallpaper exceeds the native scratch-stack size");
            for(int i=0;i<100;i++){
                NativeImage image=NativeImage.read(new ByteArrayInputStream(loaded.frames()[0].png()));
                try{if(image.getWidth()!=2048||image.getHeight()!=512)throw new AssertionError("Native dimensions changed");}
                finally{image.close();}
                boolean released=false;
                try{image.getPixel(0,0);}catch(IllegalStateException expected){released=true;}
                if(!released)throw new AssertionError("Native image pixels retained after close");
                if(MemoryStack.stackGet().getPointer()!=pointer||MemoryStack.stackGet().getFrameIndex()!=frame)throw new AssertionError("Scratch usage changed");
            }
            check(true,"100 wallpaper stream decodes release native pixels with stable scratch usage");
            System.out.println("COMPLETE: "+passed+" passed / 0 failed; generated images only, no game window.");
        }finally{
            try(var paths=Files.walk(directory)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
        }
    }
}

package dev.animal.afmr.smoke;

import com.mojang.blaze3d.platform.NativeImage;
import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.library.MusicFiles;
import org.lwjgl.system.MemoryStack;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Random;
import java.util.logging.*;
import javax.imageio.ImageIO;

/** Real Minecraft/STB decoding without creating a window, world or Minecraft singleton. */
public final class ArtworkMemoryProbe {
    private static final Logger TAG_LOG=Logger.getLogger("org.jaudiotagger");
    private static int passed;
    private static void check(boolean okay,String name){
        if(!okay)throw new AssertionError(name);passed++;System.out.println("PASS "+name);
    }
    public static void main(String[] args)throws Exception{
        TAG_LOG.setLevel(Level.WARNING);
        var source=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
        var random=new Random(209031);
        for(int y=0;y<256;y++)for(int x=0;x<256;x++)source.setRGB(x,y,0xff000000|random.nextInt(0x1000000));
        var output=new ByteArrayOutputStream();ImageIO.write(source,"png",output);byte[] png=output.toByteArray();
        source.flush();
        int pointer=MemoryStack.stackGet().getPointer(),frame=MemoryStack.stackGet().getFrameIndex();
        System.out.println("Generated PNG bytes="+png.length+"; initial native scratch bytes="+pointer);
        check(png.length>pointer,"detailed 256px artwork exceeds the default native scratch stack");
        boolean reproduced=false;
        try(var ignored=NativeImage.read(png)){}
        catch(OutOfMemoryError error){reproduced="Out of stack space.".equals(error.getMessage());}
        check(reproduced,"old byte-array image API reproduces the reported exact failure");
        check(MemoryStack.stackGet().getPointer()==pointer&&MemoryStack.stackGet().getFrameIndex()==frame,"failed byte-array decode restores its scratch frame");

        for(int i=0;i<1000;i++){
            NativeImage image=NativeImage.read(new ByteArrayInputStream(png));
            try{
                if(image.getWidth()!=256||image.getHeight()!=256)throw new AssertionError("Dimensions changed");
                image.getPixelRGBA(i%256,(i/256)%256);
            }finally{image.close();}
            if(MemoryStack.stackGet().getPointer()!=pointer||MemoryStack.stackGet().getFrameIndex()!=frame)throw new AssertionError("Scratch usage grew at decode "+i);
            boolean closed=false;
            try{image.getPixelRGBA(0,0);}catch(IllegalStateException expected){closed=true;}
            if(!closed)throw new AssertionError("Decoded pixels retained after close");
        }
        check(true,"1000 large stream decodes succeed with stable scratch usage and released pixels");
        for(int i=0;i<100;i++){
            boolean rejected=false;
            try(var ignored=NativeImage.read(new ByteArrayInputStream(new byte[]{1,2,3,4}))){}
            catch(IOException expected){rejected=true;}
            if(!rejected||MemoryStack.stackGet().getPointer()!=pointer||MemoryStack.stackGet().getFrameIndex()!=frame)throw new AssertionError("Invalid artwork leaked a scratch frame");
        }
        check(true,"100 invalid-image failures leave scratch usage unchanged");
        if(args.length>0){
            String path=Files.readString(Path.of(args[0])).strip();
            byte[] actual=MusicFiles.cover(new Track("read-only-probe",path,"Private artwork inspection",false));
            check(actual!=null,"reported crash track supplies processed artwork");
            System.out.println("Reported crash-track PNG bytes="+actual.length);
            boolean actualFailure=false;
            try(var ignored=NativeImage.read(actual)){}
            catch(OutOfMemoryError error){actualFailure="Out of stack space.".equals(error.getMessage());}
            check(actualFailure,"reported crash-track artwork reproduces the old failure");
            try(var image=NativeImage.read(new ByteArrayInputStream(actual))){
                check(image.getWidth()==256&&image.getHeight()==256,"stream loader decodes reported crash-track artwork");
            }
            check(MemoryStack.stackGet().getPointer()==pointer&&MemoryStack.stackGet().getFrameIndex()==frame,"real artwork leaves scratch usage unchanged");
        }
        System.out.println("COMPLETE: "+passed+" passed / 0 failed; no game window or personal-file writes.");
    }
}

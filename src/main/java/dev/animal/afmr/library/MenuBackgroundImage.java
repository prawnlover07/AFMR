package dev.animal.afmr.library;

import javax.imageio.ImageIO;
import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;

/** Bounded image import. The selected source is read-only; only AFMR's copy is replaced. */
public final class MenuBackgroundImage {
    public static final int MAX_EDGE=2048;
    public static final int MAX_BYTES=20*1024*1024;
    private static final long MAX_PIXELS=64_000_000;
    public record Frame(byte[] png,int millis) {}
    public record Prepared(byte[] stored,Frame[] frames,int width,int height) {
        public boolean animated(){return frames.length>1;}
    }
    private MenuBackgroundImage(){}

    public static Prepared read(Path source)throws IOException{
        if(!Files.isRegularFile(source))throw new IOException("Image file not found. Choose it again.");
        if(Files.size(source)>MAX_BYTES)throw new IOException("Choose an image no larger than 20 MiB.");
        // Bound even a file that grows after the size check. No ImageIO disk cache or global settings.
        byte[] bytes;
        try(var input=Files.newInputStream(source)){bytes=input.readNBytes(MAX_BYTES+1);}
        return decode(bytes);
    }
    public static Prepared decode(byte[] bytes)throws IOException{
        if(bytes.length>MAX_BYTES)throw new IOException("Image exceeds 20 MiB.");
        try(var input=new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            // Use the bundled WebP provider directly; mod loaders need not expose it through
            // a worker thread's ImageIO service registry. Do not change global ImageIO state.
            var webp=new WebPImageReaderSpi();
            javax.imageio.ImageReader reader;
            if(bytes.length>=16&&webp.canDecodeInput(input))reader=webp.createReaderInstance();
            else{
                var readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext())throw new IOException("Choose a valid PNG, JPEG, WebP or GIF image.");
                reader=readers.next();
            }
            try{
                String format=reader.getFormatName();
                if(format.equalsIgnoreCase("gif")){
                    reader.setInput(input,false,false);
                    return GifBackgroundImage.read(reader,bytes);
                }
                if(!format.equalsIgnoreCase("png")&&!format.equalsIgnoreCase("jpeg")&&!format.equalsIgnoreCase("webp"))
                    throw new IOException("Choose a PNG, JPEG, WebP or GIF image.");
                reader.setInput(input,true,true);
                if(format.equalsIgnoreCase("webp")&&reader.getNumImages(false)!=1)
                    throw new IOException("Animated WebP is not supported. Choose a GIF for animation.");
                int w=reader.getWidth(0),h=reader.getHeight(0);
                dimensions(w,h);
                var parameters=reader.getDefaultReadParam();
                int sample=Math.max(1,(Math.max(w,h)+MAX_EDGE-1)/MAX_EDGE);
                parameters.setSourceSubsampling(sample,sample,0,0);
                BufferedImage decoded=reader.read(0,parameters);
                if(decoded==null)throw new IOException("Image could not be decoded.");
                double scale=Math.min(1,(double)MAX_EDGE/Math.max(w,h));
                int targetW=Math.max(1,(int)Math.round(w*scale)),targetH=Math.max(1,(int)Math.round(h*scale));
                var normalized=new BufferedImage(targetW,targetH,BufferedImage.TYPE_INT_ARGB);
                try{
                    var graphics=normalized.createGraphics();
                    try{
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        graphics.drawImage(decoded,0,0,targetW,targetH,null);
                    }finally{graphics.dispose();decoded.flush();}
                    byte[] png=png(normalized);
                    return new Prepared(png,new Frame[]{new Frame(png,100)},targetW,targetH);
                }finally{normalized.flush();}
            }finally{reader.dispose();}
        }catch(IllegalArgumentException|IndexOutOfBoundsException e){throw new IOException("Invalid image: "+e.getMessage(),e);}
    }

    static void dimensions(int w,int h)throws IOException{
        if(w<1||h<1||w>32768||h>32768||(long)w*h>MAX_PIXELS)throw new IOException("Image dimensions exceed the 64 megapixel limit.");
    }
    static byte[] png(BufferedImage image)throws IOException{
        var output=new ByteArrayOutputStream();
        if(!ImageIO.write(image,"png",output))throw new IOException("PNG encoder unavailable.");
        return output.toByteArray();
    }
    public static Path animationFile(Path still){return still.resolveSibling("menu-background.gif");}
    public static Path installed(Path still){Path animation=animationFile(still);return Files.exists(animation)?animation:still;}

    public static Prepared install(Path source,Path destination)throws IOException{
        Prepared image=read(source);
        Path other=image.animated()?destination:animationFile(destination);
        if(image.animated())destination=animationFile(destination);
        Files.createDirectories(destination.getParent());
        Path temporary=Files.createTempFile(destination.getParent(),"background-",".tmp");
        try{
            Files.write(temporary,image.stored());
            try{Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING);}
            Files.deleteIfExists(other);
        }finally{Files.deleteIfExists(temporary);}
        return image;
    }
}

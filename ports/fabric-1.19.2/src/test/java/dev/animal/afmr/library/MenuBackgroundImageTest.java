package dev.animal.afmr.library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.zip.CRC32;
import static org.junit.jupiter.api.Assertions.*;

class MenuBackgroundImageTest {
    @TempDir Path directory;
    private Path image(String name,int width,int height,String format)throws IOException{
        var image=new BufferedImage(width,height,format.equals("jpeg")?BufferedImage.TYPE_INT_RGB:BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,0x7F123456);
        Path path=directory.resolve(name);assertTrue(ImageIO.write(image,format,path.toFile()));image.flush();return path;
    }
    @Test void importingCopiesAndNormalizesWithoutChangingTheOriginal()throws Exception{
        Path source=image("wallpaper.jpg",320,200,"jpeg"),target=directory.resolve("config/menu-background.png");
        byte[] original=Files.readAllBytes(source);
        var result=MenuBackgroundImage.install(source,target);
        assertArrayEquals(original,Files.readAllBytes(source));assertArrayEquals(result.frames()[0].png(),Files.readAllBytes(target));
        assertEquals(320,result.width());assertEquals(200,result.height());assertNotNull(ImageIO.read(target.toFile()));
    }
    @Test void downsamplingKeepsAspectAndBoundsPixelsBeforeFullDecode()throws Exception{
        Path source=image("large.png",4096,1024,"png");
        var result=MenuBackgroundImage.read(source);
        assertEquals(2048,result.width());assertEquals(512,result.height());
        var decoded=ImageIO.read(new ByteArrayInputStream(result.frames()[0].png()));
        assertEquals(2048,decoded.getWidth());assertEquals(512,decoded.getHeight());
    }
    @Test void pngTransparencySurvivesAndContentDeterminesTheFormat()throws Exception{
        var result=MenuBackgroundImage.read(image("renamed.jpg",10,7,"png"));
        var decoded=ImageIO.read(new ByteArrayInputStream(result.frames()[0].png()));
        assertEquals(0x7F123456,decoded.getRGB(0,0));assertEquals(0,decoded.getRGB(9,6));
    }
    @Test void invalidReplacementLeavesThePreviousCopyIntact()throws Exception{
        Path good=image("good.png",10,10,"png"),target=directory.resolve("owned/background.png");
        MenuBackgroundImage.install(good,target);byte[] before=Files.readAllBytes(target);
        Path corrupt=directory.resolve("broken.png");Files.writeString(corrupt,"not an image");
        assertThrows(IOException.class,()->MenuBackgroundImage.install(corrupt,target));
        assertArrayEquals(before,Files.readAllBytes(target));
        try(var children=Files.list(target.getParent())){assertEquals(1,children.count());}
    }
    @Test void unsupportedContentMissingFilesAndOversizeInputsAreRejected()throws Exception{
        assertThrows(IOException.class,()->MenuBackgroundImage.read(directory.resolve("missing.png")));
        Path bmp=image("unsupported.bmp",10,10,"jpeg");
        var rgb=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);ImageIO.write(rgb,"bmp",bmp.toFile());rgb.flush();
        assertThrows(IOException.class,()->MenuBackgroundImage.read(bmp));
        Path large=directory.resolve("huge.png");
        try(var channel=Files.newByteChannel(large,StandardOpenOption.CREATE,StandardOpenOption.WRITE)){
            channel.position(MenuBackgroundImage.MAX_BYTES);channel.write(ByteBuffer.wrap(new byte[]{0}));
        }
        assertThrows(IOException.class,()->MenuBackgroundImage.read(large));
    }
    @Test void declaredPixelBombIsRejectedBeforeDecoding()throws Exception{
        Path source=image("bomb.png",1,1,"png");byte[] bytes=Files.readAllBytes(source);
        ByteBuffer.wrap(bytes).putInt(16,20000).putInt(20,20000);
        CRC32 crc=new CRC32();crc.update(bytes,12,17);ByteBuffer.wrap(bytes).putInt(29,(int)crc.getValue());
        Files.write(source,bytes);
        IOException error=assertThrows(IOException.class,()->MenuBackgroundImage.read(source));assertTrue(error.getMessage().contains("64 megapixel"));
    }
    @Test void truncatedPngIsRejected()throws Exception{
        Path source=image("truncated.png",64,64,"png");byte[] bytes=Files.readAllBytes(source);
        Files.write(source,java.util.Arrays.copyOf(bytes,36));assertThrows(IOException.class,()->MenuBackgroundImage.read(source));
    }
}

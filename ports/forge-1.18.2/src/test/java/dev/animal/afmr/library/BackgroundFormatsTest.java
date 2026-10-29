package dev.animal.afmr.library;

import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class BackgroundFormatsTest {
    @TempDir Path directory;
    static byte[] fixture(String name)throws IOException{
        try(var input=BackgroundFormatsTest.class.getResourceAsStream("/backgrounds/"+name)){
            assertNotNull(input);return input.readAllBytes();
        }
    }
    private int pixel(MenuBackgroundImage.Prepared image,int frame,int x,int y)throws IOException{
        var decoded=ImageIO.read(new ByteArrayInputStream(image.frames()[frame].png()));
        try{return decoded.getRGB(x,y);}finally{decoded.flush();}
    }
    @Test void losslessWebpRetainsAlphaAndUsesContentInsteadOfFilename()throws Exception{
        var path=directory.resolve("renamed.jpg");byte[] source=fixture("lossless.webp");Files.write(path,source);
        var result=MenuBackgroundImage.install(path,directory.resolve("menu-background.png"));
        assertArrayEquals(source,Files.readAllBytes(path));assertFalse(result.animated());
        assertEquals(96,result.width());assertEquals(64,result.height());
        assertEquals(0,pixel(result,0,0,0));assertEquals(0x80ff0000,pixel(result,0,1,1));assertEquals(0xff0078ff,pixel(result,0,95,63));
        assertEquals((byte)0x89,result.stored()[0]);
    }
    @Test void lossyWebpDecodesColor()throws Exception{
        var result=MenuBackgroundImage.decode(fixture("lossy.webp"));int color=pixel(result,0,40,30);
        assertTrue(Math.abs(((color>>>16)&255)-30)<=6);assertTrue(Math.abs(((color>>>8)&255)-140)<=6);assertTrue(Math.abs((color&255)-220)<=6);
    }
    @Test void wideWebpResizesToTextureLimit()throws Exception{
        var result=MenuBackgroundImage.decode(fixture("wide.webp"));assertEquals(2048,result.width());assertEquals(512,result.height());
    }
    @Test void animatedWebpFailsClearlyWithoutReplacingExistingImage()throws Exception{
        Path source=directory.resolve("animated.webp"),target=directory.resolve("menu-background.png");
        Files.write(source,fixture("animated.webp"));Files.write(target,new byte[]{1,2,3});
        var error=assertThrows(IOException.class,()->MenuBackgroundImage.install(source,target));
        assertTrue(error.getMessage().contains("GIF"));assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(target));
    }
    @Test void gifCompositionMatchesIndependentDecoderIncludingDisposalAndOffsets()throws Exception{
        var result=MenuBackgroundImage.decode(fixture("disposal.gif"));assertTrue(result.animated());assertEquals(4,result.frames().length);
        for(int i=0;i<4;i++){
            assertEquals((i+1)*100,result.frames()[i].millis());
            var expected=ImageIO.read(new ByteArrayInputStream(fixture("disposal-"+i+".png")));
            var actual=ImageIO.read(new ByteArrayInputStream(result.frames()[i].png()));
            for(int y=0;y<40;y++)for(int x=0;x<64;x++){
                int pixel=expected.getRGB(x,y);assertEquals(pixel>>>24==0?0:pixel,actual.getRGB(x,y),"frame "+i+" at "+x+","+y);
            }
            expected.flush();actual.flush();
        }
    }
    @Test void animatedInstallReloadAndStaticReplacementUseOnlyTheCurrentOwnedAsset()throws Exception{
        Path gif=directory.resolve("source.gif"),still=directory.resolve("menu-background.png");byte[] bytes=fixture("disposal.gif");Files.write(gif,bytes);
        Files.write(still,new byte[]{1});MenuBackgroundImage.install(gif,still);
        assertFalse(Files.exists(still));assertArrayEquals(bytes,Files.readAllBytes(gif));
        assertArrayEquals(bytes,Files.readAllBytes(MenuBackgroundImage.installed(still)));
        assertEquals(4,MenuBackgroundImage.read(MenuBackgroundImage.installed(still)).frames().length);
        var webp=directory.resolve("source.webp");Files.write(webp,fixture("lossless.webp"));MenuBackgroundImage.install(webp,still);
        assertTrue(Files.exists(still));assertFalse(Files.exists(MenuBackgroundImage.animationFile(still)));
        assertFalse(MenuBackgroundImage.read(MenuBackgroundImage.installed(still)).animated());
    }
    @Test void stillGifNormalizesToPng()throws Exception{
        var result=MenuBackgroundImage.decode(fixture("still.gif"));assertFalse(result.animated());
        assertEquals(12,result.width());assertEquals(8,result.height());assertEquals((byte)0x89,result.stored()[0]);assertEquals(0xff0000ff,pixel(result,0,0,0));
    }
    @Test void retainedAnimationPixelsAreBoundedByResizing()throws Exception{
        var result=MenuBackgroundImage.decode(fixture("bounded.gif"));assertEquals(20,result.frames().length);
        assertTrue((long)result.width()*result.height()*result.frames().length<=GifBackgroundImage.FRAME_PIXELS);
        assertTrue(result.width()<1024);assertEquals(result.width(),result.height());
    }
    @Test void excessFramesAreRejectedBeforeDecoding()throws Exception{
        var error=assertThrows(IOException.class,()->MenuBackgroundImage.decode(fixture("too-many.gif")));assertTrue(error.getMessage().contains("512"));
    }
    @Test void truncatedGifAndWebpAreRejected()throws Exception{
        for(var name:new String[]{"disposal.gif","lossless.webp","lossy.webp"}){
            byte[] bytes=fixture(name);assertThrows(IOException.class,()->MenuBackgroundImage.decode(java.util.Arrays.copyOf(bytes,bytes.length/2)),name);
        }
    }
    @Test void gifCanvasBombIsRejectedBeforeDecoding()throws Exception{
        byte[] bytes=fixture("disposal.gif");bytes[6]=bytes[8]=0x20;bytes[7]=bytes[9]=0x4e;
        assertTrue(assertThrows(IOException.class,()->MenuBackgroundImage.decode(bytes)).getMessage().contains("64 megapixel"));
    }
    @Test void failedReplacementPreservesCurrentAnimationAndDoesNotLeaveTemporaryFiles()throws Exception{
        var source=directory.resolve("valid.gif");Files.write(source,fixture("disposal.gif"));var still=directory.resolve("owned/menu-background.png");
        MenuBackgroundImage.install(source,still);byte[] before=Files.readAllBytes(MenuBackgroundImage.installed(still));Files.writeString(source,"broken");
        assertThrows(IOException.class,()->MenuBackgroundImage.install(source,still));assertArrayEquals(before,Files.readAllBytes(MenuBackgroundImage.installed(still)));
        try(var files=Files.list(still.getParent())){assertEquals(1,files.count());}
    }
}

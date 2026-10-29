package dev.animal.afmr.library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class FontFilesTest {
    @TempDir Path temp;
    private byte[] fixture()throws IOException{try(var stream=getClass().getResourceAsStream("/fonts/afmr-fixture.ttf")){return stream.readAllBytes();}}
    @Test void generatedFontParsesAndTheOriginalRemainsReadOnly()throws Exception{
        byte[] bytes=fixture();Path source=temp.resolve("original.ttf");Files.write(source,bytes);var asset=FontFiles.read(source);
        assertEquals("AFMR Fixture Regular",asset.name());FontFiles.install(temp.resolve("config/menu-font.ttf"),asset);assertArrayEquals(bytes,Files.readAllBytes(source));assertArrayEquals(bytes,Files.readAllBytes(temp.resolve("config/menu-font.ttf")));
    }
    @Test void refusesNonTrueTypeOutlinesAndTruncatedHeaders()throws Exception{assertThrows(IOException.class,()->FontFiles.validate(new byte[5]));byte[] bytes=fixture();ByteBuffer.wrap(bytes).putInt(0,0x4F54544F);assertThrows(IOException.class,()->FontFiles.validate(bytes));}
    @Test void boundsTableOffsetsBeforeNativeLoading()throws Exception{byte[] bytes=fixture();ByteBuffer.wrap(bytes).putInt(12+8,Integer.MAX_VALUE);assertThrows(IOException.class,()->FontFiles.validate(bytes));}
    @Test void rejectsDuplicateTables()throws Exception{byte[] bytes=fixture();System.arraycopy(bytes,12,bytes,28,4);assertThrows(IOException.class,()->FontFiles.validate(bytes));}
    @Test void refusesAnOversizedSourceWithoutReadingIt()throws Exception{
        Path source=temp.resolve("large.ttf");try(var channel=Files.newByteChannel(source,StandardOpenOption.CREATE,StandardOpenOption.WRITE)){channel.position(FontFiles.MAX_BYTES);channel.write(ByteBuffer.wrap(new byte[]{0}));}assertThrows(IOException.class,()->FontFiles.read(source));
    }
    @Test void packOwnsOnlyItsManagedDirectoryAndRetainsIndependentFonts()throws Exception{
        Path config=temp.resolve("config"),packs=temp.resolve("packs");Files.createDirectories(config);Files.write(config.resolve("menu-font.ttf"),fixture());
        String metadata="{\"pack\":{\"description\":\"AFMR fixture\",\"pack_format\":15}}";FontFiles.buildPack(config,packs,metadata);
        Path root=packs.resolve(FontFiles.PACK);assertTrue(Files.readString(root.resolve("assets/afmr/font/custom_menu.json")).contains("menu.ttf"));assertFalse(Files.readString(root.resolve("assets/afmr/font/custom_widget.json")).contains("widget.ttf"));assertTrue(FontFiles.currentPack(config,packs,metadata));
        Files.write(config.resolve("widget-font.ttf"),fixture());assertFalse(FontFiles.currentPack(config,packs,metadata));FontFiles.buildPack(config,packs,metadata);assertTrue(Files.exists(root.resolve("assets/afmr/font/widget.ttf")));assertTrue(FontFiles.currentPack(config,packs,metadata));
    }
    @Test void unrelatedPackDirectoryIsNeverOverwritten()throws Exception{Path packs=temp.resolve("packs"),existing=packs.resolve(FontFiles.PACK);Files.createDirectories(existing);Files.writeString(existing.resolve("pack.mcmeta"),"original");assertThrows(IOException.class,()->FontFiles.buildPack(temp,packs,"new"));assertEquals("original",Files.readString(existing.resolve("pack.mcmeta")));}
}

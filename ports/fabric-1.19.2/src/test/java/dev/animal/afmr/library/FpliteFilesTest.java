package dev.animal.afmr.library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FpliteFilesTest {
    @TempDir Path directory;

    @Test void readsNativePathsLiterallyAndDoesNotChangeSourceOrIndex() throws Exception {
        String id="12345678-1234-1234-1234-123456789ABC";
        Path source=directory.resolve("playlist-"+id+".FPLITE"),index=directory.resolve("index.txt");
        Path music=directory.resolve("音楽 100% + #20.flac");
        Files.writeString(source,"\uFEFFfile://"+music+"\r\n\r\nfile://"+music+"\n");
        Files.writeString(index,"\uFEFF"+id.toLowerCase(Locale.ROOT)+":Night: quiet music\n");
        byte[] before=Files.readAllBytes(source),names=Files.readAllBytes(index);
        var modified=Files.getLastModifiedTime(source);List<Path> paths=new ArrayList<>();
        var parsed=FpliteFiles.read(source,paths::add);
        assertEquals(List.of(music.toAbsolutePath()),paths);
        assertEquals("Night: quiet music",parsed.name());assertEquals(2,parsed.entries());
        assertEquals(1,parsed.duplicates());assertEquals(0,parsed.skippedCount());
        assertArrayEquals(before,Files.readAllBytes(source));assertArrayEquals(names,Files.readAllBytes(index));
        assertEquals(modified,Files.getLastModifiedTime(source));
    }

    @Test void decodesStandardFileUrisAndResolvesRelativePaths() throws Exception {
        Path music=directory.resolve("spaces + 50% #2.flac");
        Path source=Files.writeString(directory.resolve("Music.fplite"),music.toUri()+"\nsub/../relative.wav\n");
        List<Path> paths=new ArrayList<>();var parsed=FpliteFiles.read(source,paths::add);
        assertEquals(List.of(music.toAbsolutePath(),directory.resolve("relative.wav")),paths);
        assertEquals("Music",parsed.name());
    }

    @Test void countsArchiveStreamsSubsongAndMalformedLocationsWithoutStopping() throws Exception {
        Path source=Files.writeString(directory.resolve("skip.fplite"),
            "unpack://zip|12|file://archive.zip|song.mp3\nhttps://example.invalid/music.mp3\n"
            +"file://C:\\album.flac|2\nfile://\nfile:///invalid%ZZ.mp3\nvalid.wav\n");
        List<Path> paths=new ArrayList<>();var parsed=FpliteFiles.read(source,paths::add);
        assertEquals(6,parsed.entries());assertEquals(List.of(directory.resolve("valid.wav")),paths);
        assertEquals(Map.of(FpliteFiles.Skip.ARCHIVE,1,FpliteFiles.Skip.PROTOCOL,1,
            FpliteFiles.Skip.SUBSONG,1,FpliteFiles.Skip.INVALID,2),parsed.skipped());
    }

    @Test void missingOrUnmatchedNameIndexUsesFilename() throws Exception {
        Path source=Files.writeString(directory.resolve("playlist-12345678-1234-1234-1234-123456789ABC.fplite"),"");
        assertEquals("playlist-12345678-1234-1234-1234-123456789ABC",FpliteFiles.read(source,p->{}).name());
        Files.writeString(directory.resolve("index.txt"),"unrelated:Name\n");
        assertEquals("playlist-12345678-1234-1234-1234-123456789ABC",FpliteFiles.read(source,p->{}).name());
    }

    @Test void binaryFplAndMalformedUtf8AreRejected() throws Exception {
        Path binary=Files.write(directory.resolve("old.fpl"),new byte[]{0,1,2});
        assertThrows(IOException.class,()->FpliteFiles.read(binary,p->fail("Binary file must not emit paths")));
        Path invalid=Files.write(directory.resolve("bad.fplite"),new byte[]{(byte)0xC3,0x28});
        assertThrows(IOException.class,()->FpliteFiles.read(invalid,p->{}));
    }

    @Test void emptyPlaylistIsValid() throws Exception {
        Path source=Files.writeString(directory.resolve("Empty.fplite"),"\uFEFF\r\n\n");
        var parsed=FpliteFiles.read(source,p->fail("Empty file must not emit paths"));
        assertEquals(0,parsed.entries());assertEquals(0,parsed.skippedCount());
    }
}

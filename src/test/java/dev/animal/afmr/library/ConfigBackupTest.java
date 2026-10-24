package dev.animal.afmr.library;

import com.google.gson.Gson;
import dev.animal.afmr.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigBackupTest {
    @TempDir Path temp;
    private Map<String,ConfigBackup.Key> keys(){
        var keys=new LinkedHashMap<String,ConfigBackup.Key>();
        for(String name:List.of("open","skip","next","pause","inspect","overrides","automatic"))keys.put("key.afmr."+name,new ConfigBackup.Key("key.keyboard.m","NONE"));
        for(int i=1;i<=8;i++)keys.put("key.afmr.override_"+i,new ConfigBackup.Key("key.keyboard.unknown","NONE"));
        return keys;
    }
    private ConfigBackup.Data capture(Library library)throws IOException{return ConfigBackup.capture(library,List.of(),keys(),temp.resolve("image-not-set.png"));}
    private Path archive(Map<String,byte[]> files)throws IOException{
        Path path=temp.resolve("test.afmrb");
        try(var zip=new ZipOutputStream(Files.newOutputStream(path))){for(var e:files.entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}
        return path;
    }
    @Test void roundTripKeepsSettingsAllProfilesPathsFlagsMetadataAndHistory()throws Exception{
        Library lib=Library.defaults();var song=new Library.Track("track:fixture",temp.resolve("music/one.wav").toString(),"Title",false);
        song.artist="Artist";song.albumArtist="Album artist";song.trackNumber="3";song.loudness=new TrackLoudness(100,"fixture",-2,0.5);
        lib.tracks.add(song);lib.playlists.getFirst().tracks.put(song.id,false);
        lib.playlists.getFirst().customSilence=true;lib.playlists.getFirst().silenceMinSeconds=lib.playlists.getFirst().silenceMaxSeconds=600;
        lib.settings.fadeInSeconds=3;lib.settings.fadeOutSeconds=5;lib.settings.playImmediatelyOnJoin=true;lib.settings.menuColors.put(MenuColor.ACCENT,0x123456);
        lib.settings.overrideSlots[3].playlistId=lib.playlists.getFirst().id;
        lib.folders.add(new Library.Folder(temp.resolve("music").toString(),lib.playlists.getFirst().id));
        var profile=lib.profiles.create(lib,"Second",lib.profiles.activeId);lib.profiles.bind("server:example.invalid",profile);
        lib.profiles.activate(lib,profile);lib.playlists.getFirst().volume=0.42;lib.playlists.getFirst().playOnTrigger=true;
        var history=new ListeningHistory();history.record(song,lib.playlists.getFirst(),ListeningHistory.Origin.PREVIEW,"fixture");
        lib.normalize();var data=ConfigBackup.capture(lib.snapshot(),history.snapshot(),keys(),temp.resolve("no-image"));
        Path path=temp.resolve("complete.afmrb");ConfigBackup.write(path,data);var restored=ConfigBackup.read(path);
        assertEquals(new Gson().toJsonTree(lib),new Gson().toJsonTree(restored.library()));
        assertEquals(keys(),restored.keys());assertEquals(1,restored.historyCount());
        assertEquals(Set.of("library.json","history.json","keys.json"),restored.files().keySet());
        assertFalse(Files.exists(temp.resolve("music/one.wav"))); // Export never opens referenced songs.
    }
    @Test void backgroundIsIncludedWithoutChangingItsSource()throws Exception{
        var image=new java.awt.image.BufferedImage(7,5,java.awt.image.BufferedImage.TYPE_INT_RGB);
        Path source=temp.resolve("source.png");javax.imageio.ImageIO.write(image,"png",source.toFile());byte[] original=Files.readAllBytes(source);
        Library lib=Library.defaults();lib.settings.menuImageEnabled=true;lib.settings.menuImageName="source.png";
        var data=ConfigBackup.capture(lib,List.of(),keys(),source);Path archive=temp.resolve("image.afmrb");ConfigBackup.write(archive,data);
        var read=ConfigBackup.read(archive);assertTrue(read.files().containsKey("menu-background.png"));
        assertArrayEquals(original,Files.readAllBytes(source));
    }
    @Test void stagingDoesNotReplaceAnythingUntilStartup()throws Exception{
        Path directory=temp.resolve("config");Files.createDirectories(directory);Files.writeString(directory.resolve("library.json"),"old library");
        Files.writeString(directory.resolve("history.json"),"old history");Files.writeString(directory.resolve("menu-background.png"),"old image");
        var data=capture(Library.defaults());ConfigBackup.stage(directory,data);
        assertEquals("old library",Files.readString(directory.resolve("library.json")));
        assertTrue(ConfigBackup.installPending(directory));assertFalse(ConfigBackup.installPending(directory));
        assertArrayEquals(data.files().get("library.json"),Files.readAllBytes(directory.resolve("library.json")));
        assertEquals("old library",Files.readString(directory.resolve("restore-previous/library.json")));
        assertEquals("old image",Files.readString(directory.resolve("restore-previous/menu-background.png")));
        assertFalse(Files.exists(directory.resolve("menu-background.png")));
        assertEquals(keys(),ConfigBackup.restoredKeys(directory));
    }
    @Test void interruptedRestoreRollsBackBeforeReadingPendingArchive()throws Exception{
        Path directory=temp.resolve("config"),previous=directory.resolve("restore-previous");Files.createDirectories(previous);
        Files.writeString(previous.resolve("library.json"),"original");Files.writeString(directory.resolve("library.json"),"half-installed");
        Files.writeString(directory.resolve("history.json"),"new history");Files.writeString(directory.resolve("restore-in-progress"),"library.json");
        Files.writeString(directory.resolve(ConfigBackup.PENDING),"invalid archive");
        assertThrows(IOException.class,()->ConfigBackup.installPending(directory));
        assertEquals("original",Files.readString(directory.resolve("library.json")));assertFalse(Files.exists(directory.resolve("history.json")));
        assertTrue(Files.exists(directory.resolve(ConfigBackup.PENDING)));
    }
    @Test void failedInstallRollsBackFilesAndKeepsPendingBackup()throws Exception{
        Path directory=temp.resolve("config");Files.createDirectories(directory);
        Files.writeString(directory.resolve("library.json"),"original");
        // A non-empty directory at a managed file path forces a replacement failure.
        Path obstruction=directory.resolve("menu-background.png");Files.createDirectory(obstruction);Files.writeString(obstruction.resolve("keep"),"preserve");
        ConfigBackup.stage(directory,capture(Library.defaults()));
        assertThrows(IOException.class,()->ConfigBackup.installPending(directory));
        assertEquals("original",Files.readString(directory.resolve("library.json")));assertTrue(Files.exists(directory.resolve(ConfigBackup.PENDING)));
        assertEquals("preserve",Files.readString(obstruction.resolve("keep")));
    }
    @Test void unrelatedOrTraversalEntriesAreRejectedWithoutExtracting()throws Exception{
        var files=new LinkedHashMap<>(capture(Library.defaults()).files());files.put("../escape.txt",new byte[]{1});Path path=archive(files);
        assertThrows(IOException.class,()->ConfigBackup.read(path));assertFalse(Files.exists(temp.resolve("escape.txt")));
    }
    @Test void incompleteMalformedAndFutureConfigurationsAreRejected()throws Exception{
        var files=new LinkedHashMap<>(capture(Library.defaults()).files());files.remove("history.json");
        assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
        files.put("history.json","[]".getBytes());files.put("library.json","{\"schemaVersion\":999}".getBytes());
        assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
        files.put("library.json","{broken".getBytes());assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
    }
    @Test void decompressionAndNestingAreBounded()throws Exception{
        var files=new LinkedHashMap<>(capture(Library.defaults()).files());files.put("keys.json",new byte[64*1024+1]);
        assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
        files.put("keys.json",("[".repeat(50)+"0"+"]".repeat(50)).getBytes());
        assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
    }
    @Test void onlyAfmrKeysAndRecognizedModifiersAreAccepted()throws Exception{
        var files=new LinkedHashMap<>(capture(Library.defaults()).files());var keys=keys();keys.put("key.attack",new ConfigBackup.Key("key.mouse.left","NONE"));
        files.put("keys.json",new Gson().toJson(keys).getBytes());assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
        keys.remove("key.attack");keys.put("key.afmr.open",new ConfigBackup.Key("key.keyboard.m","BOGUS"));
        files.put("keys.json",new Gson().toJson(keys).getBytes());assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
    }
    @Test void corruptOrMissingEnabledBackgroundRejectsTheWholeBackup()throws Exception{
        var lib=Library.defaults();lib.settings.menuImageEnabled=true;assertThrows(IOException.class,()->capture(lib));
        var files=new LinkedHashMap<>(capture(Library.defaults()).files());files.put("menu-background.png","invalid".getBytes());
        assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
    }
    @Test void unsupportedExportNamePreservesExistingFile()throws Exception{
        Path path=temp.resolve("settings.json");Files.writeString(path,"keep");
        assertThrows(IOException.class,()->ConfigBackup.write(path,capture(Library.defaults())));assertEquals("keep",Files.readString(path));
    }
    @Test void animationSurvivesExportReviewAndRestoreWithRecovery()throws Exception{
        Path directory=temp.resolve("animated-config");Files.createDirectories(directory);
        Path gif=directory.resolve("menu-background.gif"),png=directory.resolve("menu-background.png");
        byte[] bytes=BackgroundFormatsTest.fixture("disposal.gif");Files.write(gif,bytes);
        var lib=Library.defaults();lib.settings.menuImageEnabled=true;lib.settings.menuImageName="animation.gif";
        var data=ConfigBackup.capture(lib,List.of(),keys(),png);
        assertTrue(data.files().containsKey("menu-background.gif"));assertFalse(data.files().containsKey("menu-background.png"));
        var archive=temp.resolve("animation.afmrb");ConfigBackup.write(archive,data);var read=ConfigBackup.read(archive);
        Files.writeString(gif,"previous");ConfigBackup.stage(directory,read);assertTrue(ConfigBackup.installPending(directory));
        assertArrayEquals(bytes,Files.readAllBytes(gif));assertEquals("previous",Files.readString(directory.resolve("restore-previous/menu-background.gif")));
        assertEquals(4,MenuBackgroundImage.read(MenuBackgroundImage.installed(png)).frames().length);
        ConfigBackup.stage(directory,capture(Library.defaults()));assertTrue(ConfigBackup.installPending(directory));assertFalse(Files.exists(gif));
    }
    @Test void conflictingOrInvalidAnimationEntriesRejectTheBackup()throws Exception{
        var files=new LinkedHashMap<>(capture(Library.defaults()).files());
        files.put("menu-background.gif","invalid".getBytes());assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
        files.put("menu-background.gif",BackgroundFormatsTest.fixture("still.gif"));assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
        files.put("menu-background.gif",BackgroundFormatsTest.fixture("disposal.gif"));files.put("menu-background.png",BackgroundFormatsTest.fixture("disposal-0.png"));
        assertThrows(IOException.class,()->ConfigBackup.read(archive(files)));
    }
}

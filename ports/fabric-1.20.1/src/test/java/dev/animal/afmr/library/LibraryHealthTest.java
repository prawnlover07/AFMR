package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LibraryHealthTest {
    @TempDir Path directory;
    private Track add(Library l,String name){var t=new Track("file:"+name,directory.resolve(name).toString(),name,false);l.tracks.add(t);return t;}
    private LibraryHealth.Report check(Library l){return LibraryHealth.check(LibraryRepair.capture(l),List.of(),p->{},()->false);}
    @Test void missingFolderAndEveryContainedSongAreReportedWithoutDroppingAssignments(){
        var l=Library.defaults();l.folders.add(new Folder(directory.resolve("gone").toString(),"home"));
        var a=add(l,"gone/a.wav");var b=add(l,"gone/sub/b.mp3");l.playlist("home").tracks.put(a.id,false);
        var report=check(l);assertEquals(3,report.issues().size());assertEquals(LibraryHealth.Kind.FOLDER,report.issues().getFirst().kind());
        assertTrue(report.issues().stream().filter(i->i.kind()==LibraryHealth.Kind.TRACK).allMatch(i->i.message().contains("Parent folder")));
        assertEquals(2,l.tracks.size());assertFalse(l.playlist("home").tracks.get(a.id));assertEquals(b.id,report.issues().getLast().id());
    }
    @Test void standaloneSongsAreCheckedWithoutAnyLinkedFolders(){
        var l=Library.defaults();add(l,"missing.flac");var report=check(l);
        assertEquals(0,report.foldersChecked());assertEquals(1,report.tracksChecked());assertEquals("Song not found",report.issues().getFirst().message());
    }
    @Test void aDeletedSongInsideAnExistingFolderIsDetected()throws Exception{
        var l=Library.defaults();l.folders.add(new Folder(directory.toString(),"home"));add(l,"deleted.mp3");
        assertEquals(1,check(l).issues().size());assertEquals(LibraryHealth.Kind.TRACK,check(l).issues().getFirst().kind());
    }
    @Test void presentSourcesAndVanillaMusicProduceNoFalseMissingEntries()throws Exception{
        var l=Library.defaults();l.folders.add(new Folder(directory.toString(),"home"));Files.write(directory.resolve("present.wav"),new byte[]{1});add(l,"present.wav");
        l.tracks.add(new Track("vanilla:x","minecraft:music/x","Minecraft",true));
        var report=check(l);assertTrue(report.issues().isEmpty());assertEquals(1,report.tracksChecked());
    }
    @Test void wrongSourceTypesAreReported()throws Exception{
        var l=Library.defaults();Files.write(directory.resolve("file.wav"),new byte[]{1});l.folders.add(new Folder(directory.resolve("file.wav").toString(),"home"));
        Files.createDirectories(directory.resolve("folder.wav"));add(l,"folder.wav");var report=check(l);
        assertEquals(2,report.issues().size());assertEquals("Expected a folder",report.issues().getFirst().message());assertEquals("Expected a music file",report.issues().getLast().message());
    }
    @Test void missingSourceDetailsAreNotCappedAtOneHundred(){
        var l=Library.defaults();for(int i=0;i<250;i++)add(l,"missing/"+i+".wav");l.folders.add(new Folder(directory.resolve("missing").toString(),"home"));
        assertEquals(251,check(l).issues().size());
    }
    @Test void progressAndScanErrorsUseOwnedSnapshots(){
        var l=Library.defaults();add(l,"missing.wav");var sources=LibraryRepair.capture(l);l.tracks.getFirst().path="changed";
        var progress=new ArrayList<MusicFiles.ScanProgress>();var errors=new ArrayList<String>(List.of("Decoder detail"));
        var report=LibraryHealth.check(sources,errors,progress::add,()->false);errors.clear();
        assertEquals(directory.resolve("missing.wav").toString(),report.issues().getFirst().path());assertEquals(List.of("Decoder detail"),report.scanErrors());
        assertEquals(MusicFiles.ScanStage.CHECKING,progress.getLast().stage());assertEquals(1,progress.getLast().processed());
        assertThrows(UnsupportedOperationException.class,()->report.issues().clear());
    }
}

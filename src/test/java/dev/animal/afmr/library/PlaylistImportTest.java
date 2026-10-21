package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static dev.animal.afmr.library.MusicFiles.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistImportTest {
    @TempDir Path directory;

    @Test void importsExistingDestinationReportsSkipsAndFinishesProgress() throws Exception {
        Path music=Files.write(directory.resolve("tone.wav"),new byte[0]);
        Path source=Files.writeString(directory.resolve("Generated.fplite"),"\uFEFFfile://"+music+"\nfile://"+music+"\n"
            +"missing.mp3\nunsupported.opus\nunpack://zip|5|file://x.zip|song.mp3\nhttps://example.invalid/stream\n");
        List<ScanProgress> updates=new ArrayList<>();
        var result=scan(List.of(new Request(source,"night_outside",false)),List.of(),updates::add);
        assertEquals(1,result.found().size());assertEquals("night_outside",result.found().getFirst().playlistId());
        assertEquals("tone",result.found().getFirst().track().title);assertEquals(List.of("Generated"),result.playlists());
        assertEquals(4,result.skipped());assertEquals(1,result.duplicates());assertEquals(0,result.reused());
        assertEquals(4,result.errors().size());assertEquals(ScanProgress.starting(),updates.getFirst());
        assertEquals(new ScanProgress(ScanStage.FINALIZING,2,2,""),updates.getLast());
    }

    @Test void knownMetadataIsReusedButMissingFilesStillSkip() throws Exception {
        Path music=Files.write(directory.resolve("known.wav"),new byte[0]);
        var known=new Library.Track("file:known",music.toString(),"Cached title",false);known.durationSeconds=42;
        var missing=new Library.Track("file:missing",directory.resolve("gone.wav").toString(),"Gone",false);
        Path source=Files.writeString(directory.resolve("Known.fplite"),"file://"+music+"\nfile://"+missing.path+"\n");
        var result=scan(List.of(new Request(source,"combat",false)),List.of(known,missing),p->{});
        assertEquals(1,result.reused());assertEquals(1,result.skipped());assertSame(known,result.found().getFirst().track());
        assertEquals(42,result.found().getFirst().track().durationSeconds);
        var rescan=scan(List.of(new Request(directory,"combat",true)),List.of(known),p->{});
        assertEquals(0,rescan.reused());assertEquals("known",rescan.found().getFirst().track().title);
        assertNotEquals(known.id,rescan.found().getFirst().track().id);
    }

    @Test void mergingAndRepeatedImportsKeepDisabledAssignments() throws Exception {
        Path music=Files.write(directory.resolve("tone.wav"),new byte[0]);
        Path source=Files.writeString(directory.resolve("Generated.fplite"),"file://"+music+"\n");
        var first=scan(List.of(new Request(source,"home",false)),List.of(),p->{});
        Library library=Library.defaults();var track=first.found().getFirst().track();library.tracks.add(track);
        library.playlist("home").tracks.put(track.id,false);library.playlist("home").enabled=false;
        var repeated=scan(List.of(new Request(source,"home",false)),library.tracks,p->{});
        for(var found:repeated.found())library.assignImported(found.playlistId(),found.track().id);
        assertEquals(Boolean.FALSE,library.playlist("home").tracks.get(track.id));
        assertFalse(library.playlist("home").enabled);assertEquals(1,library.playlist("home").tracks.size());
        assertEquals(1,repeated.reused());assertEquals(6,library.playlists.size());
    }

    @Test void overlappingPlaylistsDeduplicateWithinDestinationAndKeepDifferentDestinations() throws Exception {
        Path music=Files.write(directory.resolve("tone.wav"),new byte[0]);
        Path one=Files.writeString(directory.resolve("One.fplite"),"file://"+music+"\n");
        Path two=Files.writeString(directory.resolve("Two.fplite"),music.toUri()+"\n");
        var result=scan(List.of(new Request(one,"home",false),new Request(two,"home",false),new Request(one,"combat",false)),List.of(),p->{});
        assertEquals(2,result.found().size());assertEquals(1,result.duplicates());
        assertEquals(1,result.reused());
        assertEquals(List.of("home","combat"),result.found().stream().map(Found::playlistId).toList());
        assertEquals(result.found().getFirst().track().id,result.found().getLast().track().id);
    }

    @Test void largeFailureListsRetainAccurateTotalsWithBoundedDetails() throws Exception {
        StringBuilder contents=new StringBuilder();for(int i=0;i<250;i++)contents.append("missing-").append(i).append(".wav\n");
        Path source=Files.writeString(directory.resolve("Missing.fplite"),contents);
        var result=scan(List.of(new Request(source,"home",false)),List.of(),p->{});
        assertEquals(250,result.skipped());assertEquals(100,result.errors().size());assertTrue(result.found().isEmpty());
    }
}

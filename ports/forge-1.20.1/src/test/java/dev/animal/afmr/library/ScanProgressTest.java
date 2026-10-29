package dev.animal.afmr.library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static dev.animal.afmr.library.MusicFiles.*;
import static org.junit.jupiter.api.Assertions.*;

class ScanProgressTest {
    @TempDir Path directory;

    @Test void discoversBeforeReadingAndReportsEveryCompletedFile() throws Exception {
        Path nested = Files.createDirectories(directory.resolve("nested"));
        Files.write(directory.resolve("a.wav"), new byte[0]);
        Files.write(nested.resolve("b.flac"), new byte[0]);
        Files.writeString(nested.resolve("cover.txt"), "not music");
        List<ScanProgress> updates = new ArrayList<>();
        var result = scan(List.of(new Request(directory, "library", true)), List.of(), updates::add);
        assertEquals(2, result.found().size());
        assertEquals(ScanProgress.starting(), updates.getFirst());
        assertTrue(updates.stream().anyMatch(p -> p.stage() == ScanStage.DISCOVERING && p.discovered() == 2));
        var reading = updates.stream().filter(p -> p.stage() == ScanStage.READING_TAGS).toList();
        assertEquals(List.of(0, 1, 1, 2), reading.stream().map(ScanProgress::processed).toList());
        assertTrue(reading.stream().allMatch(p -> p.discovered() == 2 && !p.currentPath().isBlank()));
        assertEquals(0.5, reading.get(1).fraction());
        assertEquals(new ScanProgress(ScanStage.FINALIZING, 2, 2, ""), updates.getLast());
    }

    @Test void fileThatDisappearsStillAdvancesProgressAndDoesNotStopNextFile() throws Exception {
        Path removed = Files.write(directory.resolve("a.wav"), new byte[0]);
        Files.write(directory.resolve("b.wav"), new byte[0]);
        List<ScanProgress> updates = new ArrayList<>();
        var result = scan(List.of(new Request(directory, "library", true)), List.of(), p -> {
            updates.add(p);
            if (p.stage() == ScanStage.READING_TAGS && p.processed() == 0) {
                try { Files.delete(removed); } catch (java.io.IOException e) { throw new AssertionError(e); }
            }
        });
        assertEquals(1, result.found().size());
        assertEquals(1, result.errors().size());
        assertEquals(2, updates.getLast().processed());
        assertEquals(1, updates.getLast().fraction());
    }

    @Test void emptyOrMissingSourcesFinishWithoutDividingByZero() {
        List<ScanProgress> updates = new ArrayList<>();
        var empty = scan(List.of(new Request(directory, "library", true)), List.of(), updates::add);
        assertTrue(empty.found().isEmpty());
        assertEquals(0, updates.getFirst().fraction());
        assertEquals(1, updates.getLast().fraction());
        var missing = scan(List.of(new Request(directory.resolve("missing.wav"), "library", false)), List.of(), updates::add);
        assertEquals(1, missing.errors().size());
        assertEquals(0, updates.getLast().processed());
        assertEquals(ScanStage.FINALIZING, updates.getLast().stage());
    }

    @Test void overlappingRequestsKeepBothPlaylistAssignmentsAndCountEachTask() throws Exception {
        Path file = Files.write(directory.resolve("a.wav"), new byte[0]);
        List<ScanProgress> updates = new ArrayList<>();
        var result = scan(List.of(new Request(directory, "day_outside", true), new Request(file, "combat", false)), List.of(), updates::add);
        assertEquals(List.of("day_outside", "combat"), result.found().stream().map(Found::playlistId).toList());
        assertEquals(result.found().getFirst().track().id, result.found().getLast().track().id);
        assertEquals(2, updates.getLast().processed());
    }
}

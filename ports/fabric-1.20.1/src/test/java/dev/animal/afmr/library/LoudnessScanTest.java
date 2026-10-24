package dev.animal.afmr.library;

import dev.animal.afmr.audio.*;
import dev.animal.afmr.core.*;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.wav.WavTag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LoudnessScanTest {
    @TempDir Path directory;
    private Path tone(String name,double amplitude) throws Exception {
        Path path=directory.resolve(name);LoudnessMeterTest.wav(path,LoudnessMeterTest.tone(48000,2,amplitude,2),48000,2);return path;
    }
    private static LoudnessScan.Request request(Path path,TrackLoudness cached){return new LoudnessScan.Request(path.toString(),path.toString(),path.getFileName().toString(),cached);}
    private static LoudnessScan.Report await(LoudnessScan.Job job){
        return assertTimeoutPreemptively(Duration.ofSeconds(15),()->{
            while(job.report()==null)Thread.sleep(5);return job.report();
        });
    }
    private static void tag(Path path,String gain,String peak) throws Exception {
        var audio=AudioFileIO.read(path.toFile());var wav=(WavTag)audio.getTagOrCreateAndSetDefault();var id3=new ID3v23Tag();
        for(var field:Map.of("REPLAYGAIN_TRACK_GAIN",gain,"REPLAYGAIN_TRACK_PEAK",peak).entrySet()){
            var frame=new ID3v23Frame("TXXX");frame.setBody(new FrameBodyTXXX((byte)0,field.getKey(),field.getValue()));id3.setField(frame);
        }
        wav.setID3Tag(id3);audio.commit();
    }
    @Test void scansBalanceTracksAndPreserveSourceBytesAndTimes() throws Exception {
        Path quiet=tone("quiet.wav",0.025),loud=tone("loud.wav",0.4);byte[] original=Files.readAllBytes(quiet);var time=Files.getLastModifiedTime(quiet);
        try(var scanner=new LoudnessScan()){
            var report=await(scanner.start("Test",List.of(request(quiet,null),request(loud,null)),true));assertEquals(2,report.count(LoudnessScan.Source.SCANNED));
            var a=report.rows().get(0);var b=report.rows().get(1);assertEquals(20*Math.log10(16),a.appliedDb()-b.appliedDb(),0.01);
            assertEquals(a.requestedDb(),a.appliedDb(),1e-10);assertNotNull(a.measurement());assertTrue(a.measurement().valid());
        }
        assertArrayEquals(original,Files.readAllBytes(quiet));assertEquals(time,Files.getLastModifiedTime(quiet));
    }
    @Test void validZeroTagsWinOverCacheAndInvalidGainTriggersAnalysis() throws Exception {
        Path zero=tone("zero.wav",0.1),bad=tone("bad.wav",0.1);tag(zero,"0 dB","0.2");tag(bad,"NaN","0.2");
        var cache=new TrackLoudness(Files.size(zero),Files.getLastModifiedTime(zero).toString(),12,0.1);
        try(var scanner=new LoudnessScan()){
            var report=await(scanner.start("Tags",List.of(request(zero,cache),request(bad,null)),true));
            assertEquals(LoudnessScan.Source.TAGS,report.rows().get(0).source());assertEquals(0,report.rows().get(0).appliedDb());
            assertNull(report.rows().get(0).measurement());assertEquals(LoudnessScan.Source.SCANNED,report.rows().get(1).source());
        }
    }
    @Test void missingPeakTagDoesNotCauseUnverifiedBoostAndReportShowsLimit() throws Exception {
        Path file=tone("nopeak.wav",0.1);tag(file,"+6 dB","");
        try(var scanner=new LoudnessScan()){
            var row=await(scanner.start("Tags",List.of(request(file,null)),true)).rows().getFirst();
            assertEquals(LoudnessScan.Source.TAGS,row.source());assertTrue(row.limited());assertEquals(6,row.requestedDb());assertEquals(0,row.appliedDb());
        }
    }
    @Test void cacheReusedAndBothTimestampAndSizeChangesInvalidateIt() throws Exception {
        Path file=tone("cached.wav",0.1);
        try(var scanner=new LoudnessScan()){
            var cached=await(scanner.start("First",List.of(request(file,null)),true)).rows().getFirst().measurement();
            assertEquals(LoudnessScan.Source.CACHED,await(scanner.start("Second",List.of(request(file,cached)),true)).rows().getFirst().source());
            Files.setLastModifiedTime(file,FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis()+2000));
            assertEquals(LoudnessScan.Source.SCANNED,await(scanner.start("Changed",List.of(request(file,cached)),true)).rows().getFirst().source());
            var differentSize=new TrackLoudness(Files.size(file)+1,Files.getLastModifiedTime(file).toString(),0,0.1);
            assertEquals(LoudnessScan.Source.SCANNED,await(scanner.start("Size",List.of(request(file,differentSize)),true)).rows().getFirst().source());
        }
    }
    @Test void pausedJobCanResumeOrCancelAndDoesNotAcceptConcurrentBatch() throws Exception {
        Path file=tone("paused.wav",0.1);
        try(var scanner=new LoudnessScan()){
            var job=scanner.start("Paused",List.of(request(file,null)),false);
            assertNull(job.report());assertEquals(0,job.completed());
            assertThrows(IllegalStateException.class,()->scanner.start("Other",List.of(),true));
            job.setAllowed(true);assertFalse(await(job).cancelled());
            job=scanner.start("Cancel",List.of(request(file,null)),false);job.cancel();var report=await(job);
            assertTrue(report.cancelled());assertTrue(report.rows().isEmpty());
        }
    }
    @Test void cancelKeepsAlreadyCompletedRows() throws Exception {
        Path first=tone("first.wav",0.1),second=tone("second.wav",0.1);
        List<LoudnessScan.Request> requests=new ArrayList<>();for(int i=0;i<100;i++)requests.add(request(i%2==0?first:second,null));
        try(var scanner=new LoudnessScan()){
            var job=scanner.start("Partial",requests,true);
            assertTimeoutPreemptively(Duration.ofSeconds(10),()->{while(job.completed()==0)Thread.sleep(1);});
            job.cancel();var report=await(job);assertTrue(report.cancelled());assertFalse(report.rows().isEmpty());assertTrue(report.rows().size()<100);
        }
    }
    @Test void badFilesDoNotPreventOtherTracksAndAllDecoderFormatsCanBeScanned() throws Exception {
        Path bad=directory.resolve("broken.mp3");Files.writeString(bad,"bad");List<LoudnessScan.Request> requests=new ArrayList<>();requests.add(request(bad,null));
        for(String name:List.of("test.wav","test.mp3","test.flac","test.ogg","test24.wav","test24.flac","testfloat.wav")){
            Path file=directory.resolve(name);try(var in=getClass().getResourceAsStream("/audio/"+name)){Files.copy(in,file);}requests.add(request(file,null));
        }
        try(var scanner=new LoudnessScan()){
            var report=await(scanner.start("Formats",requests,true));assertEquals(1,report.count(LoudnessScan.Source.FAILED));assertEquals(7,report.count(LoudnessScan.Source.SCANNED));
            assertEquals(LoudnessScan.Source.FAILED,report.rows().getFirst().source());
        }
    }
    @Test void requestsExcludeVanillaAndOtherPlaylistsButIncludeDisabledLocalAssignments() throws Exception {
        var library=Library.defaults();var playlist=library.playlists.getFirst();playlist.tracks.clear();
        var local=new Library.Track("file:one","one.wav","One",false);var other=new Library.Track("file:two","two.wav","Two",false);
        var vanilla=new Library.Track("vanilla:test","minecraft:music/test","Vanilla",true);library.tracks.addAll(List.of(local,other,vanilla));
        playlist.tracks.put(local.id,false);playlist.tracks.put(vanilla.id,true);playlist.tracks.put("missing",true);
        assertEquals(List.of(new LoudnessScan.Request(local.id,local.path,local.title,null)),LoudnessScan.requests(library,playlist));
    }
    @Test void measurementSurvivesOwnedSnapshotsAndPersistenceWithoutChangingAssignments() throws Exception {
        Path file=tone("saved.wav",0.1);var library=Library.defaults();var track=new Library.Track("file:test",file.toString(),"Test",false);library.tracks.add(track);
        track.loudness=new TrackLoudness(Files.size(file),Files.getLastModifiedTime(file).toString(),3,0.2);
        var copy=library.snapshot();track.loudness=null;assertNotNull(copy.track(track.id).loudness);
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(copy);var loaded=store.load();assertEquals(copy.track(track.id).loudness,loaded.track(track.id).loudness);
            assertEquals(copy.playlists.getFirst().tracks,loaded.playlists.getFirst().tracks);
        }
    }
}

package dev.animal.afmr.library;

import com.google.gson.Gson;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.MusicFiles.ScanProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class RepairCancellationTest {
    @TempDir Path directory;
    private Library library(int songs){
        var lib=Library.defaults();
        for(int i=0;i<songs;i++){
            var track=new Track("file:"+i,directory.resolve("gone/"+i+".wav").toString(),"Song "+i,false);
            lib.tracks.add(track);lib.playlist("home").tracks.put(track.id,i%2==0);
        }
        return lib;
    }
    @Test void cancelledCheckStopsBeforeItsFirstFolder(){
        var lib=library(1);lib.folders.add(new Folder(directory.resolve("gone").toString(),"home"));
        var progress=new ArrayList<ScanProgress>();
        assertThrows(CancellationException.class,()->LibraryHealth.check(LibraryRepair.capture(lib),List.of(),progress::add,()->true));
        assertTrue(progress.isEmpty());
    }
    @Test void cancellingDuringFolderChecksSkipsTheOtherFoldersAndAllSongs(){
        var lib=library(4);lib.folders.add(new Folder(directory.resolve("gone").toString(),"home"));
        lib.folders.add(new Folder(directory.resolve("other").toString(),"home"));
        var cancel=new AtomicBoolean();var progress=new ArrayList<ScanProgress>();
        assertThrows(CancellationException.class,()->LibraryHealth.check(LibraryRepair.capture(lib),List.of(),p->{progress.add(p);cancel.set(true);},cancel::get));
        assertEquals(1,progress.size());assertEquals(1,progress.getFirst().processed());
        assertEquals(6,progress.getFirst().discovered());
    }
    @Test void aLargeCheckStopsBetweenSongsAndPreservesEveryAssignment(){
        var lib=library(5000);String before=new Gson().toJson(lib);var cancel=new AtomicBoolean();
        var progress=new ArrayList<ScanProgress>();
        assertThrows(CancellationException.class,()->LibraryHealth.check(LibraryRepair.capture(lib),List.of(),p->{
            progress.add(p);if(p.processed()==3)cancel.set(true);
        },cancel::get));
        assertEquals(3,progress.size());assertEquals(before,new Gson().toJson(lib));
    }
    @Test void cancellationAfterTheFinalSongStillRejectsTheReport(){
        var cancel=new AtomicBoolean();var sources=LibraryRepair.capture(library(1));
        assertThrows(CancellationException.class,()->LibraryHealth.check(sources,List.of(),p->cancel.set(true),cancel::get));
    }
    @Test void anEmptyCheckAlsoHonorsCancellation(){
        assertThrows(CancellationException.class,()->LibraryHealth.check(LibraryRepair.capture(library(0)),List.of(),p->{},()->true));
    }
    @Test void cancellationFromTheClientThreadStopsTheWorkerAndLeavesItReusable()throws Exception{
        var lib=library(20);var sources=LibraryRepair.capture(lib);var cancel=new AtomicBoolean();
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var worker=Executors.newSingleThreadExecutor();
        try{
            var future=worker.submit(()->LibraryHealth.check(sources,List.of(),p->{
                entered.countDown();
                try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("Test worker was not released");}
                catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
            },cancel::get));
            assertTrue(entered.await(5,TimeUnit.SECONDS));cancel.set(true);release.countDown();
            var failure=assertThrows(ExecutionException.class,()->future.get(5,TimeUnit.SECONDS));
            assertInstanceOf(CancellationException.class,failure.getCause());
            var next=worker.submit(()->LibraryHealth.check(sources,List.of(),p->{},()->false)).get(5,TimeUnit.SECONDS);
            assertEquals(20,next.tracksChecked());assertEquals(20,next.issues().size());
            assertFalse(worker.submit(()->Thread.currentThread().isInterrupted()).get(5,TimeUnit.SECONDS));
        }finally{release.countDown();worker.shutdownNow();}
    }
    @Test void interruptedChecksRespectWorkerShutdownWithoutClearingTheInterrupt(){
        Thread.currentThread().interrupt();
        try{
            assertThrows(CancellationException.class,()->LibraryHealth.check(LibraryRepair.capture(library(1)),List.of(),p->{},()->false));
            assertTrue(Thread.currentThread().isInterrupted());
        }finally{Thread.interrupted();}
    }
    private LibraryRepair.Request replacement(Library lib)throws Exception{
        lib.folders.add(new Folder(directory.resolve("gone").toString(),"home"));
        Path replacement=directory.resolve("found");Files.createDirectories(replacement);
        // Generated unsupported-header fixtures exercise the existing metadata fallback.
        for(var track:lib.tracks)Files.write(replacement.resolve(Path.of(track.path).getFileName()),new byte[]{0,1,2});
        return new LibraryRepair.Request(true,directory.resolve("gone").toString(),replacement);
    }
    @Test void cancelledPlanningStopsBeforePathValidation(){
        assertThrows(CancellationException.class,()->LibraryRepair.plan(new LibraryRepair.Request(false,"absent",null),
            LibraryRepair.capture(library(1)),p->{},()->true));
    }
    @Test void cancellingRepairPlanningDiscardsPartialWorkAndLeavesReferencesUntouched()throws Exception{
        var lib=library(3);var request=replacement(lib);String before=new Gson().toJson(lib);
        var cancel=new AtomicBoolean();var progress=new ArrayList<ScanProgress>();
        assertThrows(CancellationException.class,()->LibraryRepair.plan(request,LibraryRepair.capture(lib),p->{
            progress.add(p);cancel.set(true);
        },cancel::get));
        assertEquals(1,progress.size());assertEquals(before,new Gson().toJson(lib));
        var plan=LibraryRepair.plan(request,LibraryRepair.capture(lib),p->{},()->false);
        assertEquals(3,plan.found());assertEquals(before,new Gson().toJson(lib));
    }
    @Test void cancellingAfterTheLastRepairFileCannotProduceAReviewablePlan()throws Exception{
        var lib=library(1);var request=replacement(lib);var cancel=new AtomicBoolean();
        assertThrows(CancellationException.class,()->LibraryRepair.plan(request,LibraryRepair.capture(lib),p->cancel.set(true),cancel::get));
        assertEquals(directory.resolve("gone/0.wav").toString(),lib.tracks.getFirst().path);
    }
}

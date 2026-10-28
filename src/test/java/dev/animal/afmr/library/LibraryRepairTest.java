package dev.animal.afmr.library;

import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryRepair.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LibraryRepairTest {
    @TempDir Path directory;
    private Track track(Library l,Path path,String id){var t=new Track(id,path.toString(),path.getFileName().toString(),false);l.tracks.add(t);return t;}
    private Path file(String name)throws Exception{
        Path p=directory.resolve(name);Files.createDirectories(p.getParent());return Files.write(p,new byte[]{1,2,3});
    }
    private Plan plan(Library l,boolean folder,String source,Path replacement)throws Exception{
        return LibraryRepair.plan(new Request(folder,source,replacement),LibraryRepair.capture(l),p->{},()->false);
    }
    @Test void movingFolderPreservesRelativePathsIdentityAndAssignmentsWithoutWritingMusic()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("old"),a=file("new/Album A/same.wav"),b=file("new/Album B/same.wav");
        l.folders.add(new Folder(old.toString(),"home"));var one=track(l,old.resolve("Album A/same.wav"),"file:one");var two=track(l,old.resolve("Album B/same.wav"),"file:two");
        l.playlist("home").tracks.put(one.id,false);l.playlist("combat").tracks.put(two.id,true);byte[] before=Files.readAllBytes(a);
        var plan=plan(l,true,old.toString(),directory.resolve("new"));assertEquals(2,plan.found());assertEquals(0,plan.missing());
        assertEquals(old.toString(),l.folders.getFirst().path);assertEquals(one.path,l.track(one.id).path);
        LibraryRepair.apply(l,plan);assertEquals(a.toRealPath().toString(),l.track(one.id).path);assertEquals(b.toRealPath().toString(),l.track(two.id).path);
        assertFalse(l.playlist("home").tracks.get(one.id));assertTrue(l.playlist("combat").tracks.get(two.id));assertArrayEquals(before,Files.readAllBytes(a));
    }
    @Test void partialRepairUpdatesExpectedPathsButLeavesUnfoundSongsReportedForIndividualRepair()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("old");l.folders.add(new Folder(old.toString(),"home"));file("new/found.wav");
        track(l,old.resolve("found.wav"),"file:found");track(l,old.resolve("renamed.wav"),"file:missing");file("new/different.wav");
        var plan=plan(l,true,old.toString(),directory.resolve("new"));assertEquals(1,plan.found());assertEquals(1,plan.missing());LibraryRepair.apply(l,plan);
        assertEquals(directory.resolve("new/renamed.wav").toString(),l.track("file:missing").path);
        var report=LibraryHealth.check(LibraryRepair.capture(l),List.of(),p->{},()->false);assertEquals(1,report.issues().size());assertEquals("file:missing",report.issues().getFirst().id());
    }
    @Test void zeroMatchesRejectsTheWrongFolderAndLeavesSetupUntouched()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("old");l.folders.add(new Folder(old.toString(),"home"));var t=track(l,old.resolve("song.wav"),"file:song");
        Files.createDirectories(directory.resolve("wrong"));assertThrows(IOException.class,()->plan(l,true,old.toString(),directory.resolve("wrong")));
        assertEquals(old.toString(),l.folders.getFirst().path);assertEquals(old.resolve("song.wav").toString(),t.path);
    }
    @Test void individualRepairSupportsRenamedSongsAndKeepsTheirId()throws Exception{
        var l=Library.defaults();var t=track(l,directory.resolve("missing.wav"),"file:original");Path chosen=file("elsewhere/renamed.flac");
        l.playlist("home").tracks.put(t.id,true);var plan=plan(l,false,t.id,chosen);LibraryRepair.apply(l,plan);
        assertEquals(chosen.toRealPath().toString(),l.track(t.id).path);assertEquals("renamed",l.track(t.id).title);assertTrue(l.playlist("home").tracks.get(t.id));
    }
    @Test void nestedLinkedFoldersAndInactiveProfileDestinationsMoveTogether()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("old"),nested=old.resolve("sub");l.folders.add(new Folder(old.toString(),"home"));l.folders.add(new Folder(nested.toString(),"combat"));
        track(l,nested.resolve("a.wav"),"file:a");file("new/sub/a.wav");String other=l.profiles.create(l,"Other","default");
        l.profiles.activate(l,other);l.folders.getFirst().playlistId="day_outside";l.folders.get(1).playlistId="night_outside";l.profiles.activate(l,"default");
        LibraryRepair.apply(l,plan(l,true,old.toString(),directory.resolve("new")));l.profiles.activate(l,other);
        assertEquals(directory.resolve("new").toString(),l.folders.getFirst().path);assertEquals("day_outside",l.folders.getFirst().playlistId);assertEquals("night_outside",l.folders.get(1).playlistId);
        l.profiles.activate(l,"default");assertEquals("home",l.folders.getFirst().playlistId);assertEquals("combat",l.folders.get(1).playlistId);
    }
    @Test void folderRepairDoesNotTouchSiblingFoldersWithSimilarNames()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("music");l.folders.add(new Folder(old.toString(),"home"));track(l,old.resolve("a.wav"),"file:a");
        var unrelated=track(l,directory.resolve("music-other/a.wav"),"file:b");file("new/a.wav");LibraryRepair.apply(l,plan(l,true,old.toString(),directory.resolve("new")));
        assertSame(unrelated,l.track(unrelated.id));assertEquals(directory.resolve("music-other/a.wav").toString(),unrelated.path);
    }
    @Test void emptyLinkedFoldersCanBeRelocatedWithoutSongs()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("old");l.folders.add(new Folder(old.toString(),"home"));Files.createDirectories(directory.resolve("new"));
        var plan=plan(l,true,old.toString(),directory.resolve("new"));assertEquals(0,plan.moves().size());LibraryRepair.apply(l,plan);assertEquals(directory.resolve("new").toString(),l.folders.getFirst().path);
    }
    @Test void unsupportedMissingOrDirectoryReplacementIsRejected()throws Exception{
        var l=Library.defaults();var t=track(l,directory.resolve("missing.wav"),"file:a");Path bad=file("bad.opus");
        assertThrows(IOException.class,()->plan(l,false,t.id,bad));assertThrows(IOException.class,()->plan(l,false,t.id,directory));assertThrows(IOException.class,()->plan(l,false,t.id,directory.resolve("gone.wav")));
    }
    @Test void alreadyLinkedDestinationDoesNotOverwriteRescanAssignments()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("old");l.folders.add(new Folder(old.toString(),"home"));Path chosen=file("new/a.wav");l.folders.add(new Folder(chosen.getParent().toString(),"combat"));track(l,old.resolve("a.wav"),"file:a");
        assertThrows(IOException.class,()->plan(l,true,old.toString(),chosen.getParent()));assertEquals("combat",l.folders.get(1).playlistId);
    }
    @Test void duplicateTargetMergesAcrossProfilesAndLegacyWithoutReenablingSongs()throws Exception{
        var l=Library.defaults();var original=track(l,directory.resolve("gone.wav"),"file:old");Path chosen=file("new.wav");var duplicate=track(l,chosen,"file:duplicate");
        l.playlist("home").tracks.put(original.id,true);l.playlist("home").tracks.put(duplicate.id,false);l.playlist("combat").tracks.put(duplicate.id,true);
        var legacy=new Playlist("legacy","Legacy");legacy.tracks.put(duplicate.id,true);l.legacyPlaylists.add(legacy);String other=l.profiles.create(l,"Other","default");
        var plan=plan(l,false,original.id,chosen);assertEquals(1,plan.merges().size());var remapped=LibraryRepair.apply(l,plan);
        assertEquals(Map.of(duplicate.id,original.id),remapped);assertEquals(1,l.tracks.size());assertNull(l.track(duplicate.id));assertFalse(l.playlist("home").tracks.get(original.id));assertTrue(l.playlist("combat").tracks.get(original.id));assertTrue(legacy.tracks.get(original.id));
        l.profiles.activate(l,other);assertFalse(l.playlist("home").tracks.get(original.id));assertFalse(l.playlist("home").tracks.containsKey(duplicate.id));
    }
    @Test void staleTrackPlanIsRejectedBeforeAnyChanges()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("old");l.folders.add(new Folder(old.toString(),"home"));track(l,old.resolve("a.wav"),"file:a");track(l,old.resolve("b.wav"),"file:b");file("new/a.wav");file("new/b.wav");
        var plan=plan(l,true,old.toString(),directory.resolve("new"));l.track("file:b").path="changed";
        assertThrows(IllegalStateException.class,()->LibraryRepair.apply(l,plan));assertEquals(old.resolve("a.wav").toString(),l.track("file:a").path);assertEquals(old.toString(),l.folders.getFirst().path);
    }
    @Test void removedFolderAndNewDestinationLinksInvalidateAReviewedPlan()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("old");l.folders.add(new Folder(old.toString(),"home"));track(l,old.resolve("a.wav"),"file:a");file("new/a.wav");
        var plan=plan(l,true,old.toString(),directory.resolve("new"));l.folders.clear();assertThrows(IllegalStateException.class,()->LibraryRepair.apply(l,plan));
        l.folders.add(new Folder(old.toString(),"home"));l.folders.add(new Folder(directory.resolve("new").toString(),"combat"));assertThrows(IllegalStateException.class,()->LibraryRepair.apply(l,plan));
    }
    @Test void newlyImportedDestinationInvalidatesPreviewInsteadOfSilentlyChangingMergeCount()throws Exception{
        var l=Library.defaults();var original=track(l,directory.resolve("gone.wav"),"file:old");Path chosen=file("new.wav");var plan=plan(l,false,original.id,chosen);
        track(l,chosen,"file:new");assertThrows(IllegalStateException.class,()->LibraryRepair.apply(l,plan));assertEquals(directory.resolve("gone.wav").toString(),original.path);
    }
    @Test void cachedLoudnessSurvivesOnlyMatchingFileAttributes()throws Exception{
        var l=Library.defaults();var t=track(l,directory.resolve("gone.wav"),"file:old");Path chosen=file("new.wav");
        t.loudness=new TrackLoudness(Files.size(chosen),Files.getLastModifiedTime(chosen).toString(),-4,0.8);
        var measurement=t.loudness;LibraryRepair.apply(l,plan(l,false,t.id,chosen));assertSame(measurement,l.track(t.id).loudness);
        Path different=file("different.wav");Files.setLastModifiedTime(different,java.nio.file.attribute.FileTime.fromMillis(Files.getLastModifiedTime(chosen).toMillis()+2000));
        LibraryRepair.apply(l,plan(l,false,t.id,different));assertNull(l.track(t.id).loudness);
    }
    @Test void rescansRefreshMetadataWithoutChangingRepairedIdentityOrCreatingDuplicates()throws Exception{
        var l=Library.defaults();var original=track(l,directory.resolve("gone.wav"),"file:kept");Path chosen=file("new/renamed.wav");
        LibraryRepair.apply(l,plan(l,false,original.id,chosen));var result=MusicFiles.scan(List.of(new MusicFiles.Request(chosen.getParent(),"home",true)),LibraryRepair.capture(l).tracks(),p->{});
        assertEquals(1,result.found().size());assertEquals(original.id,result.found().getFirst().track().id);assertEquals("renamed",result.found().getFirst().track().title);
    }
    @Test void queuedAndHistoricalDuplicatesRemapWithoutLosingOrderMetadataOrIds()throws Exception{
        var l=Library.defaults();var original=track(l,directory.resolve("gone.wav"),"file:old");Path chosen=file("new.wav");var duplicate=track(l,chosen,"file:duplicate");
        var queue=new PlaybackQueue();queue.add(duplicate,l.playlist("home"));queue.add(original,l.playlist("home"));long request=queue.first().id();
        var history=new ListeningHistory();history.record(duplicate,l.playlist("home"),ListeningHistory.Origin.QUEUE,"queued reason");var entry=history.snapshot().getFirst();
        var ids=LibraryRepair.apply(l,plan(l,false,original.id,chosen));queue.relink(ids);history.relink(ids);
        assertEquals(2,queue.size());assertEquals(request,queue.first().id());assertEquals(original.id,queue.first().trackId());
        var after=history.snapshot().getFirst();assertEquals(original.id,after.trackId());assertEquals(entry.id(),after.id());assertEquals(entry.startedAt(),after.startedAt());assertEquals(entry.reason(),after.reason());
    }
    @Test void repairedPathsAndInactiveAssignmentsSurviveSavingAndReloading()throws Exception{
        var l=Library.defaults();var t=track(l,directory.resolve("gone.wav"),"file:old");l.playlist("home").tracks.put(t.id,false);String other=l.profiles.create(l,"Other","default");
        Path chosen=file("new.wav");LibraryRepair.apply(l,plan(l,false,t.id,chosen));
        try(var store=new LibraryStore(directory.resolve("config/library.json"))){store.save(l.snapshot());var loaded=store.load();loaded.profiles.activate(loaded,other);assertFalse(loaded.playlist("home").tracks.get(t.id));assertEquals(chosen.toRealPath().toString(),loaded.track(t.id).path);}
    }
    @Test void switchingProfilesWhileReviewingStillRepairsSharedPathsAndBothSetups()throws Exception{
        var l=Library.defaults();Path old=directory.resolve("old");l.folders.add(new Folder(old.toString(),"home"));var t=track(l,old.resolve("a.wav"),"file:a");
        l.playlist("home").tracks.put(t.id,false);String other=l.profiles.create(l,"Other","default");file("new/a.wav");
        var plan=plan(l,true,old.toString(),directory.resolve("new"));l.profiles.activate(l,other);l.folders.getFirst().playlistId="combat";
        LibraryRepair.apply(l,plan);assertEquals("combat",l.folders.getFirst().playlistId);assertFalse(l.playlist("home").tracks.get(t.id));
        l.profiles.activate(l,"default");assertEquals("home",l.folders.getFirst().playlistId);assertEquals(directory.resolve("new/a.wav").toString(),l.track(t.id).path);
    }
    @Test void repairCannotPointAFolderAtItselfOrOneOfItsChildren()throws Exception{
        var l=Library.defaults();Path root=directory.resolve("old");Files.createDirectories(root.resolve("child"));l.folders.add(new Folder(root.toString(),"home"));
        assertThrows(IOException.class,()->plan(l,true,root.toString(),root));assertThrows(IOException.class,()->plan(l,true,root.toString(),root.resolve("child")));
    }
    @Test void libraryGrowthDoesNotAddFullCatalogWalksToSteadyPlaybackAfterRepair()throws Exception{
        class Guarded extends ArrayList<Track>{
            boolean locked;
            @Override public Track get(int i){if(locked)throw new AssertionError("Full catalog read during playback");return super.get(i);}
            @Override public Iterator<Track> iterator(){if(locked)throw new AssertionError("Full catalog iteration during playback");return super.iterator();}
            @Override public Spliterator<Track> spliterator(){if(locked)throw new AssertionError("Full catalog stream during playback");return super.spliterator();}
        }
        class Audio implements PlaybackEngine.Audio{
            boolean playing;
            public void start(Track t,double position){playing=true;}public boolean started(){return playing();}
        public boolean playing(){return playing;}public void gain(float v){}public void stop(){playing=false;}public void setPaused(boolean p){}
        }
        var l=Library.defaults();var tracks=new Guarded();l.tracks=tracks;
        var t=track(l,directory.resolve("gone.wav"),"file:kept");for(int i=0;i<70000;i++)track(l,directory.resolve("unrelated/"+i+".wav"),"file:unused"+i);
        l.playlist("day_outside").tracks.put(t.id,true);Path chosen=file("new.wav");LibraryRepair.apply(l,plan(l,false,t.id,chosen));
        tracks.locked=true;l.settings.silenceMinSeconds=l.settings.silenceMaxSeconds=0;
        var engine=new PlaybackEngine(new Audio(),new Random(1));
        var c=new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"",0,70,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
        for(int i=0;i<2000;i++)engine.tick(i/20.0,l,c,Set.of());assertEquals(t.id,engine.current().id);
    }
}

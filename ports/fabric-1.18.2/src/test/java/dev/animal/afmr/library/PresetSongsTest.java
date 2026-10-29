package dev.animal.afmr.library;

import com.google.gson.*;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.PlaylistPresets.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PresetSongsTest {
    @TempDir Path directory;
    private Library source(){var lib=Library.defaults();lib.playlists.clear();lib.createPlaylist("Travel");return lib;}
    private Track track(Library lib,Path path,String id){var t=new Track(id,path.toString(),path.getFileName().toString(),false);lib.tracks.add(t);lib.rebuildTrackIndex();return t;}
    private Path file(String name)throws Exception{Path p=directory.resolve(name);Files.createDirectories(p.getParent());return Files.write(p,new byte[]{1,2,3});}
    private FileData songs(Library lib){return PlaylistPresets.capture(lib,new HashSet<>(lib.playlists.stream().map(p->p.id).toList()),true);}
    private PresetSongs.Plan plan(FileData data,Library target,Map<String,Path> roots)throws Exception{
        return PresetSongs.plan(data,target.tracks.stream().map(Library::copyTrack).toList(),roots,directory.resolve("unresolved"),p->{},List.of(),Map.of(),()->false,false);
    }
    private FileData example(){
        var lib=source();Path root=directory.resolve("PrivateSender/Music");lib.folders.add(new Folder(root.toString(),"library"));
        var a=track(lib,root.resolve("Album A/same.wav"),"sender-one");var b=track(lib,root.resolve("Album B/same.wav"),"sender-two");
        lib.playlists.getFirst().tracks.put(a.id,true);lib.playlists.getFirst().tracks.put(b.id,false);return songs(lib);
    }
    private List<String> apply(Library lib,FileData data,PresetSongs.Plan plan){return PlaylistPresets.apply(lib,"default","","",Placement.TOP,data,"",plan).ids();}

    @Test void optionalSongListContainsOnlyPortablePathsAndDeduplicatesSharedSongs()throws Exception{
        var lib=source();var t=track(lib,directory.resolve("PrivateSender/Music/Album/track.wav"),"private-original-id");
        lib.folders.add(new Folder(directory.resolve("PrivateSender/Music").toString(),"library"));
        lib.playlists.getFirst().tracks.put(t.id,false);lib.createPlaylist("Battle").tracks.put(t.id,true);
        var data=songs(lib);assertEquals(1,data.songs.size());assertEquals("Album/track.wav",data.songs.getFirst().path);assertEquals("Music",data.folders.getFirst().name);
        Path path=directory.resolve("share.afmrp");PlaylistPresets.write(path,data);String json=Files.readString(path);
        assertFalse(json.contains("PrivateSender"));assertFalse(json.contains("private-original-id"));assertFalse(json.contains(directory.toString().replace("\\","\\\\")));
        var loaded=PlaylistPresets.read(path);assertEquals(false,loaded.playlists.get(0).tracks.get("song:0"));assertEquals(true,loaded.playlists.get(1).tracks.get("song:0"));
    }
    @Test void filesImportedIndividuallyUseTheirParentFolderAndNestedLinksUseNearestRoot(){
        var lib=source();Path root=directory.resolve("Music"),album=root.resolve("Album");lib.folders.add(new Folder(root.toString(),"library"));lib.folders.add(new Folder(album.toString(),"library"));
        var a=track(lib,album.resolve("one.wav"),"a");var b=track(lib,directory.resolve("Loose/two.wav"),"b");
        lib.playlists.getFirst().tracks.put(a.id,true);lib.playlists.getFirst().tracks.put(b.id,true);
        var data=songs(lib);assertEquals(List.of("Album","Loose"),data.folders.stream().map(f->f.name).toList());assertEquals(List.of("one.wav","two.wav"),data.songs.stream().map(s->s.path).toList());
    }
    @Test void chosenRootMatchesExactRelativePathsWithSameFilenamesAndRetainsOnOffStates()throws Exception{
        var data=example();Path a=file("Recipient/Album A/same.wav"),b=file("Recipient/Album B/same.wav");byte[] before=Files.readAllBytes(a);
        var target=source();var plan=plan(data,target,Map.of("folder:0",directory.resolve("Recipient")));assertEquals(2,plan.found());assertEquals(0,plan.missing());
        var p=target.playlist(apply(target,data,plan).getFirst());assertEquals(2,p.tracks.size());
        for(var t:target.tracks)assertEquals(t.path.equals(a.toRealPath().toString()),p.tracks.get(t.id));
        assertArrayEquals(before,Files.readAllBytes(a));assertArrayEquals(before,Files.readAllBytes(b));assertTrue(target.folders.isEmpty());
    }
    @Test void missingSongsAndUnlocatedFoldersSurviveImportAndUseOrdinaryFolderRepair()throws Exception{
        var data=example();var target=source();var plan=plan(data,target,Map.of());assertEquals(2,plan.missing());
        String id=apply(target,data,plan).getFirst();var assigned=new LinkedHashMap<>(target.playlist(id).tracks);
        assertEquals(1,target.folders.size());assertFalse(Files.exists(Path.of(target.folders.getFirst().path)));
        var health=LibraryHealth.check(LibraryRepair.capture(target),List.of(),p->{},()->false);assertEquals(3,health.issues().size());
        Path a=file("Found/Album A/same.wav");file("Found/Album B/same.wav");byte[] bytes=Files.readAllBytes(a);
        var repair=LibraryRepair.plan(new LibraryRepair.Request(true,target.folders.getFirst().path,directory.resolve("Found")),LibraryRepair.capture(target),p->{},()->false);
        assertEquals(2,repair.found());LibraryRepair.apply(target,repair);assertEquals(assigned,target.playlist(id).tracks);
        assertTrue(LibraryHealth.check(LibraryRepair.capture(target),List.of(),p->{},()->false).issues().isEmpty());assertArrayEquals(bytes,Files.readAllBytes(a));
    }
    @Test void partialMatchesKeepMissingAssignmentForIndividualRenamedFileRepairAndRestart()throws Exception{
        var data=example();file("Recipient/Album A/same.wav");Path renamed=file("Recipient/Album B/new-name.wav");
        var lib=source();var plan=plan(data,lib,Map.of("folder:0",directory.resolve("Recipient")));assertEquals(1,plan.missing());
        var p=lib.playlist(apply(lib,data,plan).getFirst());String missing=p.tracks.entrySet().stream().filter(e->!e.getValue()).findFirst().orElseThrow().getKey();
        var repair=LibraryRepair.plan(new LibraryRepair.Request(false,missing,renamed),LibraryRepair.capture(lib),s->{},()->false);LibraryRepair.apply(lib,repair);
        assertFalse(p.tracks.get(missing));assertEquals(renamed.toRealPath().toString(),lib.track(missing).path);
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(lib.snapshot());var loaded=store.load();assertEquals(lib.track(missing).path,loaded.track(missing).path);assertEquals(p.tracks,loaded.playlist(p.id).tracks);}
    }
    @Test void existingTracksAreReusedByPathEvenWhenIdsDifferAndOtherPlaylistsAreUntouched()throws Exception{
        var data=example();Path a=file("Recipient/Album A/same.wav");file("Recipient/Album B/same.wav");
        var target=source();target.playlists.getFirst().name="Recipient local playlist";var existing=track(target,a,"recipient-stable-id");target.playlists.getFirst().tracks.put(existing.id,false);
        var plan=plan(data,target,Map.of("folder:0",directory.resolve("Recipient")));String imported=apply(target,data,plan).getFirst();
        assertEquals(2,target.tracks.size());assertEquals(true,target.playlist(imported).tracks.get(existing.id));assertEquals(false,target.playlists.get(1).tracks.get(existing.id));
    }
    @Test void rechecksAndRepeatedImportsReuseTracksInsteadOfDuplicatingThem()throws Exception{
        var data=example();file("Recipient/Album A/same.wav");file("Recipient/Album B/same.wav");var lib=source();
        var first=plan(data,lib,Map.of("folder:0",directory.resolve("Recipient")));var a=apply(lib,data,first).getFirst();
        var b=apply(lib,data,plan(data,lib,Map.of("folder:0",directory.resolve("Recipient")))).getFirst();
        assertEquals(a,b);assertEquals(2,lib.tracks.size());assertEquals(lib.playlist(a).tracks,lib.playlist(b).tracks);
    }
    @Test void settingsOnlyImportOfSongBearingFileDoesNotTouchCatalogOrFolders(){
        var lib=source();var data=example();lib.tracks=new ArrayList<>(){@Override public Iterator<Track> iterator(){throw new AssertionError("Visited catalog during settings-only import");}};
        String id=apply(lib,data,null).getFirst();assertTrue(lib.playlist(id).tracks.isEmpty());assertTrue(lib.folders.isEmpty());
    }
    @Test void matchingUsesUniqueLinkedFolderNamesAndNeverChoosesAmbiguousRoots(){
        var data=example();Path a=directory.resolve("A/Music"),b=directory.resolve("B/Music");
        assertEquals(a,PresetSongs.matchFolders(data,List.of(a.toString())).get("folder:0"));
        assertTrue(PresetSongs.matchFolders(data,List.of(a.toString(),b.toString())).isEmpty());
        assertEquals(a,PresetSongs.matchFolders(data,List.of(a.toString(),a.toString())).get("folder:0"));
    }
    @Test void filenameMatchesElsewhereAreNotGuessed()throws Exception{
        var data=example();file("Recipient/Other Album/same.wav");var lib=source();var plan=plan(data,lib,Map.of("folder:0",directory.resolve("Recipient")));
        assertEquals(0,plan.found());assertEquals(2,plan.missing());assertTrue(lib.tracks.isEmpty());assertEquals(1,lib.playlists.size());
    }
    @Test void duplicateExportedRootNamesRequireExplicitChoices(){
        var lib=source();var a=track(lib,directory.resolve("A/Music/one.wav"),"a");var b=track(lib,directory.resolve("B/Music/two.wav"),"b");
        lib.playlists.getFirst().tracks.put(a.id,true);lib.playlists.getFirst().tracks.put(b.id,true);
        assertTrue(PresetSongs.matchFolders(songs(lib),List.of(directory.resolve("Recipient/Music").toString())).isEmpty());
    }
    @Test void deliberatelyMappingTwoRootsToOneFileReusesOneTrackAndDisabledWins()throws Exception{
        var lib=source();var a=track(lib,directory.resolve("A/Music/same.wav"),"a");var b=track(lib,directory.resolve("B/Music/same.wav"),"b");
        lib.playlists.getFirst().tracks.put(a.id,true);lib.playlists.getFirst().tracks.put(b.id,false);var data=songs(lib);file("Recipient/same.wav");
        var target=source();var plan=plan(data,target,Map.of("folder:0",directory.resolve("Recipient"),"folder:1",directory.resolve("Recipient")));
        var p=target.playlist(apply(target,data,plan).getFirst());assertEquals(1,target.tracks.size());assertEquals(1,p.tracks.size());assertEquals(false,p.tracks.values().iterator().next());
    }
    @Test void cancelledOrReplacedPreviewHasNoLibraryOrFilesystemEffects()throws Exception{
        var data=example();var lib=source();Path audio=file("Recipient/Album A/same.wav");byte[] before=Files.readAllBytes(audio);
        var ignored=plan(data,lib,Map.of("folder:0",directory.resolve("Recipient")));assertEquals(1,ignored.found());
        assertTrue(lib.tracks.isEmpty());assertTrue(lib.folders.isEmpty());assertEquals(1,lib.playlists.size());assertFalse(Files.exists(directory.resolve("unresolved")));assertArrayEquals(before,Files.readAllBytes(audio));
    }
    @Test void importedSongsCanTargetInactiveProfileWhileLibraryStaysShared()throws Exception{
        var lib=source();var data=example();String other=lib.profiles.create(lib,"Other",null);var plan=plan(data,lib,Map.of());
        var ids=PlaylistPresets.apply(lib,other,"","",Placement.TOP,data,"",plan).ids();assertEquals(1,lib.playlists.size());assertEquals(2,lib.tracks.size());
        assertEquals(2,lib.profiles.playlists(lib,other).getFirst().tracks.size());assertEquals(ids.getFirst(),lib.profiles.playlists(lib,other).getFirst().id);
    }
    @Test void vanillaResourceReferencesRetainExplicitExclusionsAndReportUnavailableResources()throws Exception{
        var lib=source();var a=new Track("vanilla:minecraft:music/a","minecraft:music/a","A",true);var b=new Track("vanilla:custom:music/b","custom:music/b","B",true);
        lib.tracks.addAll(List.of(a,b));lib.rebuildTrackIndex();lib.playlists.getFirst().tracks.put(a.id,false);lib.playlists.getFirst().tracks.put(b.id,true);
        var data=songs(lib);assertTrue(data.folders.isEmpty());var target=source();target.tracks.add(a);target.rebuildTrackIndex();var plan=plan(data,target,Map.of());
        assertEquals(1,plan.found());assertEquals(0,plan.missing());assertEquals(1,plan.unavailableResources());
        var p=target.playlist(apply(target,data,plan).getFirst());assertEquals(false,p.tracks.get(a.id));assertEquals(true,p.tracks.get(b.id));
        data.songs.getFirst().path="minecraft:"+"x".repeat(4096);assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(data));
    }
    @Test void deletedProfileOrWrongPlanFailsWithoutChangingLibrary()throws Exception{
        var lib=source();var data=example();var plan=plan(data,lib,Map.of());
        assertThrows(IllegalStateException.class,()->PlaylistPresets.apply(lib,"deleted","","",Placement.TOP,data,"",plan).ids());
        assertThrows(IllegalArgumentException.class,()->apply(lib,example(),plan));assertTrue(lib.tracks.isEmpty());assertTrue(lib.folders.isEmpty());assertEquals(1,lib.playlists.size());
    }
    @Test void untrustedPathsAndWindowsAbsoluteOrNetworkPathsAreRejectedBeforeDiskAccess(){
        for(String path:List.of("../secret.wav","Album/../../secret.wav","/secret.wav","C:/secret.wav","C:secret.wav","//host/share/a.wav","Album\\a.wav","Album//a.wav","./a.wav","Album/a.txt","Album/./a.wav")){
            var data=example();data.songs.getFirst().path=path;assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(data),path);
        }
    }
    @Test void untrustedAssignmentsMustReferToKnownSongsAndUseRealBooleanStates()throws Exception{
        var gson=new Gson();var data=example();var root=gson.toJsonTree(data).getAsJsonObject();var entries=root.getAsJsonArray("playlists").get(0).getAsJsonObject().getAsJsonObject("tracks");
        Path file=directory.resolve("bad.afmrp");entries.addProperty("song:0","false");Files.writeString(file,root.toString());assertThrows(IOException.class,()->PlaylistPresets.read(file));
        entries.addProperty("song:0",false);entries.addProperty("foreign-id",true);Files.writeString(file,root.toString());assertThrows(IOException.class,()->PlaylistPresets.read(file));
    }
    @Test void malformedDuplicateOrUnassignedSongAndFolderReferencesAreRejected(){
        var data=example();data.songs.get(1).id=data.songs.get(0).id;assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(data));
        var invalidFolder=example();invalidFolder.folders.getFirst().name="../Private";assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(invalidFolder));
        var extra=example();extra.playlists.getFirst().tracks.clear();assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(extra));
        var duplicate=example();duplicate.songs.get(1).path=duplicate.songs.get(0).path;assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(duplicate));
    }
    @Test void progressIsBoundedAndFileChecksDoNotLoseMissingEntries()throws Exception{
        var data=example();var progress=new ArrayList<MusicFiles.ScanProgress>();
        var plan=PresetSongs.plan(data,List.of(),Map.of(),directory.resolve("unresolved"),progress::add,List.of(),Map.of(),()->false,false);
        assertEquals(2,progress.size());assertEquals(2,progress.getLast().processed());assertEquals(1,progress.getLast().fraction());assertEquals(2,plan.songs().size());
    }
    @Test void songExportReadsOnlyAssignedTracksThroughTheExistingIndex(){
        var lib=source();boolean[] indexed={false};
        lib.tracks=new ArrayList<>(){
            @Override public Iterator<Track> iterator(){throw new AssertionError("Full catalog export traversal");}
            @Override public Track get(int i){if(indexed[0])throw new AssertionError("Unindexed catalog access during export");return super.get(i);}
        };
        for(int i=0;i<70_000;i++)lib.tracks.add(new Track("unrelated:"+i,"unused/"+i+".wav","Unused",false));
        lib.tracks.add(new Track("a",directory.resolve("Music/a.wav").toString(),"A",false));lib.rebuildTrackIndex();indexed[0]=true;
        lib.playlists.getFirst().tracks.put("a",true);
        assertEquals(1,songs(lib).songs.size());
    }
    @Test void removedAssignmentsGiveAnActionableExportErrorWithoutAffectingSettingsOnlySharing(){
        var lib=source();lib.playlists.getFirst().tracks.put("deleted-song",true);
        var failure=assertThrows(IllegalArgumentException.class,()->songs(lib));assertTrue(failure.getMessage().contains("removed song"));
        assertTrue(PlaylistPresets.capture(lib,Set.of(lib.playlists.getFirst().id),false).songs.isEmpty());
    }
}

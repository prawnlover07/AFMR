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
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ContentMatchingTest {
    @TempDir Path directory;
    private Path file(String name,int... bytes)throws IOException{
        Path path=directory.resolve(name);Files.createDirectories(path.getParent());byte[] contents=new byte[bytes.length];
        for(int i=0;i<bytes.length;i++)contents[i]=(byte)bytes[i];return Files.write(path,contents);
    }
    private SongIdentity identity(Path path)throws Exception{return new ContentFiles.Reader(()->false).identify(path);}
    private Library library(){var lib=Library.defaults();lib.playlists.clear();lib.createPlaylist("Travel");return lib;}
    private Track track(Library lib,Path path,String id){var t=new Track(id,path.toString(),path.getFileName().toString(),false);lib.tracks.add(t);lib.rebuildTrackIndex();return t;}
    private FileData setup(Path source)throws Exception{
        var lib=library();var t=track(lib,source,"sender");lib.playlists.getFirst().tracks.put(t.id,true);
        var data=PlaylistPresets.capture(lib,Set.of(lib.playlists.getFirst().id),true);
        PresetSongs.identifyExport(data,p->{},()->false);return data;
    }
    private PresetSongs.Plan find(FileData data,Library target,Map<String,Path> roots,Collection<Path> folders,Map<String,Path> files)throws Exception{
        return PresetSongs.plan(data,target.tracks.stream().map(Library::copyTrack).toList(),roots,directory.resolve("missing"),p->{},folders,files,()->false,false);
    }
    private Playlist apply(Library target,FileData data,PresetSongs.Plan plan){
        return target.playlist(PlaylistPresets.apply(target,"default","","",Placement.TOP,data,"",plan).ids().getFirst());
    }
    @Test void byteIdentityIgnoresNameFolderAndModificationTime()throws Exception{
        var a=file("sender/one.wav",1,2,3,4);var b=file("receiver/albums/renamed.mp3",1,2,3,4);
        Files.setLastModifiedTime(b,java.nio.file.attribute.FileTime.fromMillis(1000));
        assertEquals(identity(a),identity(b));assertEquals(4,identity(a).size());
    }
    @Test void sameSizeDifferentContentsAreNotTheSameSong()throws Exception{
        var a=file("one.wav",1,2,3,4);var b=file("two.wav",1,2,3,5);
        assertFalse(new ContentFiles.Reader(()->false).matches(b,identity(a)));
    }
    @Test void invalidIdentityCannotEnterSavedConfiguration(){
        for(String value:Arrays.asList(null,"","abc","g".repeat(64)))assertThrows(IllegalArgumentException.class,()->new SongIdentity(1,value));
        assertThrows(IllegalArgumentException.class,()->new SongIdentity(-1,"0".repeat(64)));
    }
    @Test void cancellationDuringHashingClosesTheInputFile()throws Exception{
        Path path=directory.resolve("large.wav");Files.write(path,new byte[256*1024]);var polls=new AtomicInteger();
        assertThrows(CancellationException.class,()->new ContentFiles.Reader(()->polls.incrementAndGet()>2).identify(path));
        Files.delete(path);assertFalse(Files.exists(path));
    }
    @Test void recursiveSearchFindsRenamedCopiesAndIgnoresFilenameCollisions()throws Exception{
        var expected=identity(file("sender/song.wav",1,2,3));file("recipient/song.wav",9,8,7);
        var correct=file("recipient/deep/renamed.wav",1,2,3);
        var result=ContentFiles.find(List.of(directory.resolve("recipient")),List.of(),Set.of(expected),new ContentFiles.Reader(()->false),p->{});
        assertEquals(correct,result.files().get(expected));
    }
    @Test void knownCatalogCopiesArePreferredAndOverlappingRootsAreSafe()throws Exception{
        var known=file("local/already-linked.wav",4,3,2,1);file("local/sub/duplicate.wav",4,3,2,1);var expected=identity(known);
        var result=ContentFiles.find(List.of(directory.resolve("local"),directory.resolve("local/sub")),List.of(known),Set.of(expected),new ContentFiles.Reader(()->false),p->{});
        assertEquals(known,result.files().get(expected));
    }
    @Test void unreadableRootsAreReportedWhileOtherRootsCanMatch()throws Exception{
        var correct=file("z-local/song.wav",1,2);var expected=identity(correct);
        var result=ContentFiles.find(List.of(directory.resolve("a-missing"),directory.resolve("z-local")),List.of(),Set.of(expected),new ContentFiles.Reader(()->false),p->{});
        assertEquals(correct,result.files().get(expected));assertFalse(result.errors().isEmpty());
    }
    @Test void aCancelledRecursiveSearchCannotPublishPartialMatches()throws Exception{
        var expected=identity(file("song.wav",1,2));
        assertThrows(CancellationException.class,()->ContentFiles.find(List.of(directory),List.of(),Set.of(expected),new ContentFiles.Reader(()->true),p->{}));
    }
    @Test void exportCarriesFingerprintWithoutPrivatePathsOrSourceIds()throws Exception{
        var source=file("PrivateSender/Music/song.wav",1,2,3);var data=setup(source);
        Path shared=directory.resolve("share.afmrp");PlaylistPresets.write(shared,data);String json=Files.readString(shared);
        assertFalse(json.contains("PrivateSender"));assertFalse(json.contains("sourcePath"));assertFalse(json.contains("sourceId"));assertFalse(json.contains("sender"));
        var loaded=PlaylistPresets.read(shared);assertEquals(identity(source).sha256(),loaded.songs.getFirst().sha256);assertEquals(3,loaded.songs.getFirst().bytes);
    }
    @Test void exportRechecksChangedContentsRatherThanTrustingAnOldFingerprint()throws Exception{
        var path=file("sender/song.wav",1,2,3);var data=setup(path);String old=data.songs.getFirst().sha256;
        Files.write(path,new byte[]{3,2,1});PresetSongs.identifyExport(data,p->{},()->false);
        assertNotEquals(old,data.songs.getFirst().sha256);
    }
    @Test void absentUnidentifiedSourcesExplainWhySharingCannotVerifyThem()throws Exception{
        var lib=library();var t=track(lib,directory.resolve("absent.wav"),"absent");lib.playlists.getFirst().tracks.put(t.id,true);
        var data=PlaylistPresets.capture(lib,Set.of(lib.playlists.getFirst().id),true);
        assertThrows(IOException.class,()->PresetSongs.identifyExport(data,p->{},()->false));
    }
    @Test void aPreviouslyRecordedMissingSongCanStillBeShared()throws Exception{
        var path=file("sender/song.wav",7,8,9);var data=setup(path);String expected=data.songs.getFirst().sha256;Files.delete(path);
        PresetSongs.identifyExport(data,p->{},()->false);assertEquals(expected,data.songs.getFirst().sha256);
    }
    @Test void linkedLibrarySearchWorksWithoutMatchingFolderNamesOrRelativePaths()throws Exception{
        var data=setup(file("Sender/Music/Album/a.wav",1,2,3));var local=file("Receiver/Soundtracks/other/renamed.wav",1,2,3);
        var target=library();target.playlists.getFirst().name="Recipient local playlist";var existing=track(target,local,"recipient");target.playlists.getFirst().tracks.put(existing.id,false);
        target.folders.add(new Folder(directory.resolve("Receiver/Soundtracks").toString(),"library"));
        var plan=find(data,target,Map.of(),List.of(directory.resolve("Receiver/Soundtracks")),Map.of());
        assertEquals(1,plan.found());assertEquals(1,plan.verified());var imported=apply(target,data,plan);
        assertTrue(imported.tracks.get("recipient"));assertFalse(target.playlists.get(1).tracks.get("recipient"));assertEquals(1,target.tracks.size());
    }
    @Test void supplyingAFolderFindsSongsInAReorganizedLayout()throws Exception{
        var data=setup(file("Sender/Original/Album/a.wav",1,2,3,4));var local=file("New/Flat/renamed.wav",1,2,3,4);
        var target=library();var plan=find(data,target,Map.of("folder:0",directory.resolve("New")),List.of(),Map.of());
        assertEquals(1,plan.found());assertEquals(local.toRealPath().toString(),plan.songs().getFirst().track().path);
    }
    @Test void aWrongSongAtTheExpectedPathIsNotImportedAsPlayable()throws Exception{
        var data=setup(file("Sender/Music/a.wav",1,2,3));var wrong=file("Receiver/a.wav",9,8,7);
        var target=library();track(target,wrong,"wrong");
        var plan=find(data,target,Map.of("folder:0",directory.resolve("Receiver")),List.of(),Map.of());
        assertEquals(1,plan.missing());assertFalse(plan.songs().getFirst().found());
        var p=apply(target,data,plan);assertFalse(p.tracks.containsKey("wrong"));
        var missing=target.track(p.tracks.keySet().iterator().next());assertFalse(Files.exists(Path.of(missing.path)));assertNotNull(missing.content);
    }
    @Test void supplyingOneFileAcceptsOnlyIdenticalContents()throws Exception{
        var data=setup(file("Sender/a.wav",1,2,3));var right=file("Receiver/renamed.wav",1,2,3);var wrong=file("Receiver/wrong.wav",3,2,1);
        var target=library();var bad=find(data,target,Map.of(),List.of(),Map.of("song:0",wrong));
        assertEquals(1,bad.missing());assertFalse(bad.errors().isEmpty());
        var good=find(data,target,Map.of(),List.of(),Map.of("song:0",right));assertEquals(1,good.found());assertEquals(1,good.verified());
    }
    @Test void identicalSharedDuplicatesMergeWithoutReenablingADisabledAssignment()throws Exception{
        var lib=library();var a=track(lib,file("Sender/A/a.wav",4,5,6),"a");var b=track(lib,file("Sender/B/b.wav",4,5,6),"b");
        lib.playlists.getFirst().tracks.put(a.id,true);lib.playlists.getFirst().tracks.put(b.id,false);
        var data=PlaylistPresets.capture(lib,Set.of(lib.playlists.getFirst().id),true);PresetSongs.identifyExport(data,p->{},()->false);
        file("Receiver/only-copy.wav",4,5,6);var target=library();
        var p=apply(target,data,find(data,target,Map.of(),List.of(directory.resolve("Receiver")),Map.of()));
        assertEquals(1,p.tracks.size());assertFalse(p.tracks.values().iterator().next());assertEquals(1,target.tracks.size());
    }
    @Test void unresolvedFingerprintsSurviveSavingAndLaterIndividualRepair()throws Exception{
        var original=file("Sender/a.wav",1,3,5);var data=setup(original);var target=library();var plan=find(data,target,Map.of(),List.of(),Map.of());
        var p=apply(target,data,plan);String id=p.tracks.keySet().iterator().next();Path settings=directory.resolve("library.json");
        try(var store=new LibraryStore(settings)){
            store.save(target.snapshot());target=store.load();
        }
        assertEquals(identity(original),target.track(id).content);Path local=file("Receiver/different-name.wav",1,3,5);
        var repair=LibraryRepair.plan(new LibraryRepair.Request(false,id,local),LibraryRepair.capture(target),s->{},()->false);
        LibraryRepair.apply(target,repair);assertEquals(local.toRealPath().toString(),target.track(id).path);
    }
    @Test void folderRepairFindsContentAndKeepsExistingDestinationSettings()throws Exception{
        var data=setup(file("Sender/a.wav",1,2,3));var lib=library();var p=apply(lib,data,find(data,lib,Map.of(),List.of(),Map.of()));
        String id=p.tracks.keySet().iterator().next();String oldRoot=lib.folders.getFirst().path;
        var local=file("Receiver/album/renamed.wav",1,2,3);String newRoot=directory.resolve("Receiver").toString();
        lib.folders.add(new Folder(newRoot,"combat"));String profile=lib.profiles.create(lib,"Copy","default");
        var repair=LibraryRepair.plan(new LibraryRepair.Request(true,oldRoot,Path.of(newRoot)),LibraryRepair.capture(lib),s->{},()->false);
        LibraryRepair.apply(lib,repair);assertEquals(local.toRealPath().toString(),lib.track(id).path);
        assertEquals(1,lib.folders.size());assertEquals("combat",lib.folders.getFirst().playlistId);
        lib.profiles.activate(lib,profile);assertEquals("combat",lib.folders.getFirst().playlistId);
    }
    @Test void mismatchedIndividualRepairCannotChangeTheSavedLibrary()throws Exception{
        var data=setup(file("Sender/a.wav",1,2,3));var lib=library();var p=apply(lib,data,find(data,lib,Map.of(),List.of(),Map.of()));
        String before=new Gson().toJson(lib);String id=p.tracks.keySet().iterator().next();var wrong=file("Receiver/wrong.wav",3,2,1);
        assertThrows(IOException.class,()->LibraryRepair.plan(new LibraryRepair.Request(false,id,wrong),LibraryRepair.capture(lib),s->{},()->false));
        assertEquals(before,new Gson().toJson(lib));
    }
    @Test void persistedCatalogSignaturesCanResolveAnImportBeforeAFolderSearch()throws Exception{
        var data=setup(file("Sender/a.wav",9,7,5));var local=file("Local/renamed.wav",9,7,5);var lib=library();
        var existing=track(lib,local,"local");existing.content=identity(local);
        assertEquals(1,find(data,lib,Map.of(),List.of(),Map.of()).found());
        Files.write(local,new byte[]{1,2,3});assertEquals(1,find(data,lib,Map.of(),List.of(),Map.of()).missing());
    }
    @Test void portableImportRejectsMalformedOrIncompleteFingerprints()throws Exception{
        var data=setup(file("Sender/a.wav",1,2,3));var tree=new Gson().toJsonTree(data).getAsJsonObject();
        var song=tree.getAsJsonArray("songs").get(0).getAsJsonObject();song.addProperty("sha256","not-a-hash");
        var path=directory.resolve("invalid.afmrp");Files.writeString(path,tree.toString());assertThrows(IOException.class,()->PlaylistPresets.read(path));
        song.addProperty("sha256",data.songs.getFirst().sha256);song.addProperty("bytes",-1);Files.writeString(path,tree.toString());
        assertThrows(IOException.class,()->PlaylistPresets.read(path));
    }
    @Test void cancellationDiscardsImportMatchingWithoutChangingTheTarget()throws Exception{
        var data=setup(file("Sender/a.wav",1,2,3));var lib=library();String before=new Gson().toJson(lib);
        assertThrows(CancellationException.class,()->PresetSongs.plan(data,List.of(),Map.of(),directory.resolve("missing"),p->{},List.of(directory),Map.of(),()->true,false));
        assertEquals(before,new Gson().toJson(lib));
    }
    @Test void explicitLibrarySearchAlsoFindsIndividuallyImportedSongs()throws Exception{
        var data=setup(file("Sender/a.wav",1,4,9));var local=file("Local/renamed.wav",1,4,9);var lib=library();
        track(lib,local,"existing");assertTrue(lib.folders.isEmpty());
        var plan=PresetSongs.plan(data,lib.tracks.stream().map(Library::copyTrack).toList(),Map.of(),directory.resolve("missing"),p->{},List.of(),Map.of(),()->false,true);
        assertEquals(1,plan.found());assertEquals("existing",apply(lib,data,plan).tracks.keySet().iterator().next());
    }
    @Test void partialContentRepairKeepsTheUnresolvedFolderAvailableForRetry()throws Exception{
        var sender=library();var a=track(sender,file("Sender/one.wav",1,2,3),"a");var b=track(sender,file("Sender/two.wav",4,5,6),"b");
        sender.playlists.getFirst().tracks.put(a.id,true);sender.playlists.getFirst().tracks.put(b.id,true);
        var data=PlaylistPresets.capture(sender,Set.of(sender.playlists.getFirst().id),true);PresetSongs.identifyExport(data,p->{},()->false);
        var lib=library();apply(lib,data,find(data,lib,Map.of(),List.of(),Map.of()));String old=lib.folders.getFirst().path;
        file("Local/first-renamed.wav",1,2,3);
        var partial=LibraryRepair.plan(new LibraryRepair.Request(true,old,directory.resolve("Local")),LibraryRepair.capture(lib),p->{},()->false);
        assertEquals(1,partial.found());LibraryRepair.apply(lib,partial);assertEquals(old,lib.folders.getFirst().path);
        file("Local/elsewhere/second-renamed.wav",4,5,6);
        var rest=LibraryRepair.plan(new LibraryRepair.Request(true,old,directory.resolve("Local")),LibraryRepair.capture(lib),p->{},()->false);
        LibraryRepair.apply(lib,rest);assertTrue(LibraryHealth.check(LibraryRepair.capture(lib),List.of(),p->{},()->false).issues().isEmpty());
    }
    @Test void recordedFingerprintsTravelThroughFullBackupsAndOwnedSnapshots()throws Exception{
        var lib=library();var song=track(lib,file("Local/a.wav",6,7,8),"a");song.content=identity(Path.of(song.path));
        var keys=new LinkedHashMap<String,ConfigBackup.Key>();
        for(String name:List.of("open","skip","next","pause","inspect","overrides","automatic"))keys.put("key.afmr."+name,new ConfigBackup.Key("key.keyboard.unknown","NONE"));
        for(int i=1;i<=8;i++)keys.put("key.afmr.override_"+i,new ConfigBackup.Key("key.keyboard.unknown","NONE"));
        var snapshot=lib.snapshot();var expected=song.content;song.content=null;
        var data=ConfigBackup.capture(snapshot,List.of(),keys,directory);var path=directory.resolve("backup.afmrb");
        ConfigBackup.write(path,data);assertEquals(expected,ConfigBackup.read(path).library().track("a").content);
    }
    @Test void reorganizedNestedLinksKeepTheSelectedRootsRescanSettings()throws Exception{
        var lib=library();Path old=directory.resolve("old"),nested=old.resolve("Album");
        var a=track(lib,file("source-a.wav",1,2,3),"a");a.content=identity(Path.of(a.path));a.path=nested.resolve("a.wav").toString();
        var b=track(lib,file("source-b.wav",4,5,6),"b");b.content=identity(Path.of(b.path));b.path=old.resolve("b.wav").toString();
        lib.folders.add(new Folder(nested.toString(),"combat"));lib.folders.add(new Folder(old.toString(),"home"));
        String profile=lib.profiles.create(lib,"Copy","default");file("local/renamed-a.wav",1,2,3);file("local/renamed-b.wav",4,5,6);
        var plan=LibraryRepair.plan(new LibraryRepair.Request(true,old.toString(),directory.resolve("local")),LibraryRepair.capture(lib),p->{},()->false);
        LibraryRepair.apply(lib,plan);assertEquals(1,lib.folders.size());assertEquals("home",lib.folders.getFirst().playlistId);
        assertTrue(LibraryHealth.check(LibraryRepair.capture(lib),List.of(),p->{},()->false).issues().isEmpty());
        lib.profiles.activate(lib,profile);assertEquals("home",lib.folders.getFirst().playlistId);
    }
}

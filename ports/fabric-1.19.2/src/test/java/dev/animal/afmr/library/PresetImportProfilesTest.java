package dev.animal.afmr.library;

import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.PlaylistPresets.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PresetImportProfilesTest {
    @TempDir Path temp;
    private Library source(){var l=new Library();l.playlists.add(new Playlist("source","Travel"));return l;}
    private FileData capture(Library l){return PlaylistPresets.capture(l,new HashSet<>(l.playlists.stream().map(p->p.id).toList()),false);}
    private ImportResult apply(Library target,String profile,String name,FileData data){return PlaylistPresets.apply(target,profile,name,"",Placement.TOP,data,"",null);}
    @Test void newProfileContainsOnlyImportedPlaylistsAndDoesNotActivateOrBind(){
        var target=Library.defaults();var playlists=target.playlists;var settings=target.settings;target.settings.silenceMinSeconds=73;
        var result=apply(target,null,"Friend soundtrack",capture(source()));
        assertEquals("default",target.profiles.activeId);assertSame(playlists,target.playlists);assertSame(settings,target.settings);assertEquals(73,settings.silenceMinSeconds);
        assertTrue(target.profiles.bindings.isEmpty());assertEquals(1,target.profiles.playlists(target,result.profileId()).size());assertNotEquals("default",result.profileId());
        target.profiles.activate(target,result.profileId());assertEquals("Friend soundtrack",target.profiles.activeName);assertEquals(1,target.playlists.size());assertEquals("Travel",target.playlists.get(0).name);
    }
    @Test void repeatedNewProfileImportsKeepIndependentIds(){var target=Library.defaults();var data=capture(source());var a=apply(target,null,"A",data);var b=apply(target,null,"B",data);assertNotEquals(a.profileId(),b.profileId());assertNotEquals(a.ids(),b.ids());}
    @Test void invalidNameDoesNotCreateAProfile(){var target=Library.defaults();assertThrows(IllegalArgumentException.class,()->apply(target,null,"  ",capture(source())));assertTrue(target.profiles.inactive.isEmpty());}
    @Test void profileLimitRejectsNewButStillAllowsExistingDestination(){var target=Library.defaults();for(int i=1;i<PlaylistProfiles.LIMIT;i++)target.profiles.create(target,"P"+i,null);assertThrows(IllegalArgumentException.class,()->apply(target,null,"One more",capture(source())));assertEquals(PlaylistProfiles.LIMIT-1,target.profiles.inactive.size());assertEquals("default",apply(target,"default","",capture(source())).profileId());}
    @Test void unavailableLocationsDoNotLeaveAnEmptyProfile(){var source=source();var area=source.playlists.get(0).conditions;area.areaEnabled=true;area.area.configured=true;var target=Library.defaults();assertThrows(IllegalArgumentException.class,()->apply(target,null,"Location",capture(source)));assertTrue(target.profiles.inactive.isEmpty());}
    @Test void settingsOnlyReplacementRetainsIdentityPositionSongsAndReferences(){
        var target=Library.defaults();var existing=target.createPlaylist(" travel ");existing.tracks.put("disabled-song",false);existing.tracks.put("song",true);
        target.settings.overrideSlots[0].playlistId=existing.id;target.folders.add(new Folder(temp.toString(),existing.id));int index=target.playlists.indexOf(existing);
        var src=source();var p=src.playlists.get(0);p.enabled=false;p.volume=.25;p.customSongCount=true;p.songsBeforeSilence=3;p.conditions.ridingAnimal=PlaylistConditions.State.YES;
        var result=apply(target,"default","",capture(src));assertEquals(List.of(existing.id),result.ids());assertSame(existing,target.playlists.get(index));
        assertEquals(Map.of("disabled-song",false,"song",true),existing.tracks);assertFalse(existing.enabled);assertEquals(.25,existing.volume);assertEquals(3,existing.songsBeforeSilence);assertEquals(PlaylistConditions.State.YES,existing.conditions.ridingAnimal);
        assertEquals(existing.id,target.settings.overrideSlots[0].playlistId);assertEquals(existing.id,target.folders.get(0).playlistId);
    }
    @Test void songImportReplacesAssignmentsButKeepsSharedLibraryTracks()throws Exception{
        var src=source();Path song=temp.resolve("new.wav");Files.write(song,new byte[]{1,2,3});var incoming=new Track("new-song",song.toString(),"New",false);src.tracks.add(incoming);src.folders.add(new Folder(temp.toString(),"library"));src.playlists.get(0).tracks.put(incoming.id,false);
        var data=PlaylistPresets.capture(src,Set.of("source"),true);var target=Library.defaults();var existing=target.createPlaylist("TRAVEL");var old=new Track("old-song",temp.resolve("old.wav").toString(),"Old",false);target.tracks.add(old);existing.tracks.put(old.id,true);
        var plan=PresetSongs.plan(data,target.tracks.stream().map(Library::copyTrack).toList(),Map.of(data.folders.get(0).id,temp),temp.resolve("missing"),p->{},List.of(),Map.of(),()->false,false);
        var result=PlaylistPresets.apply(target,"default","","",Placement.TOP,data,"",plan);assertEquals(existing.id,result.ids().get(0));assertEquals(1,existing.tracks.size());assertFalse(existing.tracks.containsKey(old.id));assertTrue(existing.tracks.containsValue(false));assertNotNull(target.track(old.id));
    }
    @Test void ambiguousExistingNamesStopTheWholeImport(){var target=Library.defaults();var a=target.createPlaylist("Travel");target.createPlaylist(" travel ");var data=capture(source());int count=target.playlists.size();assertThrows(IllegalArgumentException.class,()->apply(target,"default","",data));assertEquals(count,target.playlists.size());assertEquals("Travel",a.name);}
    @Test void duplicateIncomingNamesAreRejectedForExistingProfilesButAllowedInNewOnes(){var src=source();src.playlists.add(new Playlist("second","TRAVEL"));var data=capture(src);var target=Library.defaults();assertThrows(IllegalArgumentException.class,()->apply(target,"default","",data));var result=apply(target,null,"Duplicates",data);assertEquals(2,result.ids().size());}
    @Test void unmatchedInsertionDoesNotMoveReplacedPlaylists(){var target=Library.defaults();var existing=target.createPlaylist("Travel");int before=target.playlists.indexOf(existing);var src=source();src.playlists.add(new Playlist("new","New playlist"));var result=apply(target,"default","",capture(src));assertSame(existing,target.playlists.get(before+1));assertEquals(result.ids().get(1),target.playlists.get(0).id);}
    @Test void replacedAndAddedTransitionTargetsReconnectToTheirFinalIds(){var target=Library.defaults();var existing=target.createPlaylist("Travel");var src=source();src.playlists.add(new Playlist("second","Away"));var rule=new TransitionRule();rule.finish=TransitionRule.Finish.FINISH;src.playlists.get(0).transitions.put("second",rule);var result=apply(target,"default","",capture(src));assertEquals(existing.id,result.ids().get(0));assertEquals(TransitionRule.Finish.FINISH,existing.transitions.get(result.ids().get(1)).finish);}
    @Test void inactiveProfileReplacementLeavesActiveSetupUntouched(){var target=Library.defaults();var profile=target.profiles.create(target,"Other",null);var other=target.profiles.playlists(target,profile).get(0);other.name="Travel";var active=target.playlists;apply(target,profile,"",capture(source()));assertSame(active,target.playlists);assertSame(other,target.profiles.playlists(target,profile).get(0));}
    @Test void newProfileSurvivesOwnedSaveAndReload()throws Exception{var target=Library.defaults();var result=apply(target,null,"Saved import",capture(source()));var file=temp.resolve("library.json");try(var store=new LibraryStore(file)){store.save(target.snapshot());}try(var store=new LibraryStore(file)){var loaded=store.load();assertEquals(1,loaded.profiles.playlists(loaded,result.profileId()).size());assertEquals(result.ids().get(0),loaded.profiles.playlists(loaded,result.profileId()).get(0).id);}}
}

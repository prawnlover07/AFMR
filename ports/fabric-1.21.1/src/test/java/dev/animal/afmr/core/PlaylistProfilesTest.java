package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistProfilesTest {
    @Test void existingSetupIsDefaultAndUnlinkedWorldsUseIt(){
        var l=Library.defaults();assertEquals("default",l.profiles.activeId);assertEquals("Default",l.profiles.activeName);
        assertEquals("default",l.profiles.forWorld("server:unknown"));assertEquals(6,l.playlists.size());
    }
    @Test void freshProfileStartsWithTemplatesAndMinecraftTiming(){
        var l=Library.defaults();l.playlist("home").volume=0.2;l.settings.silenceMinSeconds=1;
        String id=l.profiles.create(l,"Fresh",null);l.profiles.activate(l,id);
        assertEquals(6,l.playlists.size());assertEquals(1,l.playlist("home").volume);assertEquals(600,l.settings.silenceMinSeconds);
    }
    @Test void switchingMovesOwnedPlaylistsAndPreservesSettingsObjectIdentity(){
        var l=Library.defaults();var settings=l.settings;var original=l.playlists;var day=l.playlist("day_outside");
        day.volume=0.3;day.tracks.put("song",false);l.settings.silenceMinSeconds=4;
        String id=l.profiles.create(l,"Other","default");assertTrue(l.profiles.activate(l,id));
        assertSame(settings,l.settings);assertNotSame(original,l.playlists);assertEquals(0.3,l.playlist(day.id).volume);
        l.playlist(day.id).volume=0.9;l.settings.silenceMinSeconds=50;l.profiles.activate(l,"default");
        assertSame(original,l.playlists);assertSame(day,l.playlist(day.id));assertEquals(0.3,day.volume);assertEquals(4,l.settings.silenceMinSeconds);
        l.profiles.activate(l,id);assertEquals(0.9,l.playlist(day.id).volume);assertEquals(50,l.settings.silenceMinSeconds);
    }
    @Test void copiesOwnAssignmentsConditionsGroupsAndPriority(){
        var l=Library.defaults();var p=l.playlist("dark_exploration");p.tracks.put("a",false);
        String copy=l.profiles.create(l,"Copy","default");l.profiles.activate(l,copy);var next=l.playlist(p.id);
        next.tracks.put("a",true);next.conditions.groups.clear();l.movePlaylist(next.id,0);
        l.profiles.activate(l,"default");assertFalse(p.tracks.get("a"));assertFalse(p.conditions.groups.isEmpty());assertEquals("combat",l.playlists.getFirst().id);
    }
    @Test void copyingInactiveSetupDoesNotCopyTheCurrentSetupInstead(){
        var l=Library.defaults();l.settings.silenceMinSeconds=17;String a=l.profiles.create(l,"A",null);l.profiles.activate(l,a);
        String b=l.profiles.create(l,"B","default");l.profiles.activate(l,b);assertEquals(17,l.settings.silenceMinSeconds);
    }
    @Test void displayBindingsImportPreferencesAndLibraryObjectsRemainShared(){
        var l=Library.defaults();var tracks=l.tracks;var folders=l.folders;
        String id=l.profiles.create(l,"Other",null);l.settings.widgetScale=1.7;l.settings.rescanOnLaunch=true;
        l.settings.overrideSlots[0].playlistId="custom:test";l.settings.songColumns.add(SongColumn.GENRE);l.profiles.activate(l,id);
        assertSame(tracks,l.tracks);assertSame(folders,l.folders);assertEquals(1.7,l.settings.widgetScale);assertTrue(l.settings.rescanOnLaunch);
        assertEquals("custom:test",l.settings.overrideSlots[0].playlistId);assertTrue(l.settings.songColumns.contains(SongColumn.GENRE));
    }
    @Test void folderDestinationsArePerProfileIncludingNewFolders(){
        var l=Library.defaults();l.folders.add(new Folder("one","combat"));String id=l.profiles.create(l,"Other",null);
        l.profiles.activate(l,id);assertEquals("library",l.folders.getFirst().playlistId);l.folders.getFirst().playlistId="home";l.folders.add(new Folder("two","home"));
        l.profiles.activate(l,"default");assertEquals("combat",l.folders.getFirst().playlistId);assertEquals("library",l.folders.get(1).playlistId);
        l.profiles.activate(l,id);assertEquals("home",l.folders.getFirst().playlistId);assertEquals("home",l.folders.get(1).playlistId);
    }
    @Test void importsFinishInOriginalSetupAndPreserveDisabledSongs(){
        var l=Library.defaults();l.playlist("home").tracks.put("off",false);String id=l.profiles.create(l,"Other",null);l.profiles.activate(l,id);
        l.assignImported("default","home","new");l.assignImported("default","home","off");assertTrue(l.playlist("home").tracks.isEmpty());
        l.profiles.activate(l,"default");assertTrue(l.playlist("home").tracks.get("new"));assertFalse(l.playlist("home").tracks.get("off"));
    }
    @Test void deletedImportSourceDoesNotRedirectAssignmentsToTheActiveProfile(){
        var l=Library.defaults();String id=l.profiles.create(l,"Other",null);assertTrue(l.profiles.delete(id));l.assignImported(id,"home","song");
        assertTrue(l.playlist("home").tracks.isEmpty());
    }
    @Test void exactWorldKeysAvoidDisplayNameCollisionsAndSupportSharedProfile(){
        var l=Library.defaults();String id=l.profiles.create(l,"Servers",null);
        l.profiles.bind("singleplayer:C:/saves/one/Same name",id);l.profiles.bind("server:example:25565",id);
        assertEquals(id,l.profiles.forWorld("singleplayer:C:/saves/one/Same name"));assertEquals("default",l.profiles.forWorld("singleplayer:C:/saves/two/Same name"));
        assertEquals("default",l.profiles.forWorld("server:example:25566"));assertEquals(id,l.profiles.forWorld("server:example:25565"));
    }
    @Test void deletingInactiveProfileClearsLinksAndProtectsDefaultAndActive(){
        var l=Library.defaults();String id=l.profiles.create(l,"Other",null);l.profiles.bind("server:a",id);
        assertFalse(l.profiles.delete("default"));l.profiles.activate(l,id);assertFalse(l.profiles.delete(id));
        l.profiles.activate(l,"default");assertTrue(l.profiles.delete(id));assertEquals("default",l.profiles.forWorld("server:a"));
    }
    @Test void namesAreValidatedAndIdentityDoesNotDependOnName(){
        var l=Library.defaults();String a=l.profiles.create(l,"Same",null),b=l.profiles.create(l,"Same",null);assertNotEquals(a,b);
        l.profiles.bind("server:a",a);l.profiles.rename(a," Renamed\n");assertEquals(a,l.profiles.forWorld("server:a"));
        assertThrows(IllegalArgumentException.class,()->l.profiles.rename(a," "));assertThrows(IllegalArgumentException.class,()->l.profiles.create(l,"x".repeat(81),null));
        assertThrows(IllegalArgumentException.class,()->l.profiles.activate(l,"missing"));assertThrows(IllegalArgumentException.class,()->l.profiles.bind("",a));
    }
    @Test void capacityBoundsStoredSetups(){
        var l=Library.defaults();for(int i=1;i<PlaylistProfiles.LIMIT;i++)l.profiles.create(l,"Profile "+i,null);
        assertThrows(IllegalArgumentException.class,()->l.profiles.create(l,"Too many",null));l.normalize();
    }
    @Test void snapshotsOwnBothInactiveAndActiveSetups(){
        var l=Library.defaults();String id=l.profiles.create(l,"Other","default");l.profiles.bind("server:a",id);var copy=l.snapshot();
        l.profiles.activate(l,id);l.playlist("home").volume=0.1;l.profiles.rename("default","Changed");l.profiles.bindings.clear();
        copy.profiles.activate(copy,id);assertEquals(1,copy.playlist("home").volume);assertEquals(id,copy.profiles.forWorld("server:a"));
        copy.profiles.activate(copy,"default");assertEquals("Default",copy.profiles.activeName);
    }
    @Test void invalidStoredLinksAndDuplicateIdsAreRejected(){
        var l=Library.defaults();l.profiles.bindings.put("server:a","missing");assertThrows(IllegalArgumentException.class,l::normalize);
        l.profiles.bindings.clear();String id=l.profiles.create(l,"Other",null);l.profiles.inactive.getFirst().id="default";
        assertThrows(IllegalArgumentException.class,l::normalize);
    }
    @Test void inactivePlaybackAndVolumeUseTheSameBounds(){
        var l=Library.defaults();String id=l.profiles.create(l,"Other",null);var p=l.profiles.inactive.getFirst();
        p.playback.fadeInSeconds=-1;p.playback.fadeOutSeconds=99;p.playback.combatDeathMusic=null;p.playlists.getFirst().volume=9;
        l.normalize();l.profiles.activate(l,id);assertEquals(0,l.settings.fadeInSeconds);assertEquals(60,l.settings.fadeOutSeconds);
        assertEquals(DeathMusic.FADE,l.settings.combatDeathMusic);assertEquals(1,l.playlists.getFirst().volume);
    }
    @Test void profileSwitchAndSteadyVolumeNeverWalkSeventyThousandLibraryTracks(){
        var l=Library.defaults();var tracks=new LibraryIndexTest.GuardedTracks();l.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("file:"+i,"file","Song",false));
        l.playlist("day_outside").tracks.put("file:69999",true);l.playlist("day_outside").volume=0.25;
        String id=l.profiles.create(l,"Copy","default");l.rebuildTrackIndex();tracks.locked=true;l.profiles.activate(l,id);
        l.settings.silenceMinSeconds=l.settings.silenceMaxSeconds=0;var audio=new PlaybackEngineTest.FakeAudio();var engine=new PlaybackEngine(audio,new Random(1));
        var c=new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"",0,70,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
        for(int i=0;i<2000;i++)engine.tick(i/20.0,l,c,Set.of());assertEquals(0.25,audio.gain,0.001);
    }
    @Test void everyProfilePlaybackFieldSurvivesCaptureAndApply() throws Exception{
        var p=new ProfilePlayback();p.silenceMinSeconds=3;p.silenceMaxSeconds=9;p.fadeInSeconds=2;p.fadeOutSeconds=4;p.bufferSeconds=1;
        p.jukeboxMode=JukeboxMode.IMMEDIATE;p.continuePlayingOnMatch=true;p.playImmediatelyOnJoin=true;p.combatEnabled=false;p.combatThreatsOnly=false;
        p.combatOnAttack=false;p.combatOnDamage=false;p.preventMusicWhileDead=false;p.replayGainEnabled=false;
        p.combatQuietSeconds=45;p.combatDeathFadeSeconds=4;p.combatDeathMusic=DeathMusic.KEEP_PLAYING;
        var s=new Settings();p.apply(s);var copy=ProfilePlayback.capture(s);
        for(var field:ProfilePlayback.class.getFields())assertEquals(field.get(p),field.get(copy),field.getName());
    }
}

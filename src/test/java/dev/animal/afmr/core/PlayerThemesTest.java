package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import dev.animal.afmr.library.PlaylistPresets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerThemesTest {
    private static final String UUID="12345678-1234-1234-1234-123456789abc";
    @TempDir Path folder;
    private Context context(NearbyPlayers players){return new Context("minecraft:overworld","minecraft:plains",false,false,6000,false,15,"test",0,70,0,0,.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,players);}
    @Test void radiusUsesSquaredThreeDimensionalDistanceAndInclusiveBoundary(){var p=new NearbyPlayers(Map.of("Friend",25.0));assertTrue(p.matches("","friend",5));assertFalse(p.matches("","friend",4.99));}
    @Test void uuidSurvivesNameChangeAndNeverFallsBackToADifferentPlayersName(){
        var p=new NearbyPlayers(Map.of(UUID,4.0,"NewName",4.0));assertTrue(p.matches(UUID,"OldName",3));
        assertFalse(new NearbyPlayers(Map.of("OldName",0.0)).matches(UUID,"OldName",3));
    }
    @Test void absentInvalidAndUnconfiguredTargetsCannotMatch(){
        assertFalse(NearbyPlayers.EMPTY.matches(UUID,"Friend",32));assertFalse(new NearbyPlayers(Map.of("friend",Double.NaN)).matches("","Friend",32));
        assertFalse(new NearbyPlayers(Map.of("",0.0)).matches("","",32));assertFalse(new NearbyPlayers(Map.of("friend",0.0)).matches("","Friend",Double.POSITIVE_INFINITY));
    }
    @Test void unknownUuidIsNotANameAlias(){assertFalse(new NearbyPlayers(Map.of("friend",0.0)).matches("not-a-uuid","friend",32));assertFalse(new NearbyPlayers(Map.of("friend",0.0)).matches("friend","another",32));}
    @Test void proximityComposesWithAllAnyNotAndNestedGroups(){
        var c=new PlaylistConditions();c.nearbyPlayerEnabled=true;c.nearbyPlayerName="Friend";c.nearbyPlayerRadius=5;c.timeEnabled=true;c.startTime=12000;c.endTime=23999;
        var near=context(new NearbyPlayers(Map.of("friend",1.0)));var far=context(NearbyPlayers.EMPTY);var settings=new Settings();
        assertFalse(c.matches(near,settings));c.matchAny=true;assertTrue(c.matches(near,settings));assertFalse(c.matches(far,settings));
        var parent=new PlaylistConditions();parent.groups.add(c);assertTrue(parent.matches(near,settings));parent.negated=true;assertFalse(parent.matches(near,settings));assertTrue(parent.matches(far,settings));
    }
    @Test void appearanceCountsAndNearbyIdentitySurviveOwnedSnapshotsAndDisk()throws Exception{
        var l=Library.defaults();var p=l.playlists.get(0);l.settings.songsBeforeSilence=3;l.settings.popupDelaySeconds=3;
        p.customSongCount=true;p.songsBeforeSilence=5;p.conditions.nearbyPlayerEnabled=true;p.conditions.nearbyPlayerName="Friend";p.conditions.nearbyPlayerUuid=UUID;
        var owned=l.snapshot();l.settings.songsBeforeSilence=9;p.conditions.nearbyPlayerName="Changed";
        assertEquals(3,owned.settings.songsBeforeSilence);assertEquals("Friend",owned.playlists.get(0).conditions.nearbyPlayerName);
        var file=folder.resolve("library.json");try(var store=new LibraryStore(file)){store.save(owned);}
        Library read;try(var store=new LibraryStore(file)){read=store.load();}
        assertEquals(3,read.settings.popupDelaySeconds);assertEquals(5,read.playlists.get(0).songsBeforeSilence);assertEquals(UUID,read.playlists.get(0).conditions.nearbyPlayerUuid);
    }
    @Test void profilePlaybackOwnsCountWhileDelayStaysShared(){var s=new Settings();s.songsBeforeSilence=3;s.popupDelaySeconds=4;var p=ProfilePlayback.capture(s);s.songsBeforeSilence=7;s.popupDelaySeconds=6;p.apply(s);assertEquals(3,s.songsBeforeSilence);assertEquals(6,s.popupDelaySeconds);}
    @Test void minecraftDefaultsRestoreOneSongWithoutChangingPlaylistOverride(){var l=Library.defaults();l.settings.songsBeforeSilence=8;var p=l.playlists.get(0);p.customSongCount=true;p.songsBeforeSilence=4;l.settings.restoreMinecraftPlaybackDefaults();assertEquals(1,l.settings.songsBeforeSilence);assertEquals(4,PlaybackEngine.songsBeforeSilence(p,l.settings));}
    @Test void playlistSharingRetainsTheCountAndPlayerIdentity()throws Exception{
        var l=Library.defaults();var p=l.playlists.get(0);p.customSongCount=true;p.songsBeforeSilence=4;p.conditions.nearbyPlayerEnabled=true;p.conditions.nearbyPlayerName="Friend";p.conditions.nearbyPlayerUuid=UUID;
        var data=PlaylistPresets.capture(l,Set.of(p.id),false);var file=folder.resolve("theme.afmrp");PlaylistPresets.write(file,data);var read=PlaylistPresets.read(file);
        assertEquals(4,read.playlists.get(0).songsBeforeSilence);assertEquals(UUID,read.playlists.get(0).conditions.nearbyPlayerUuid);
        read.playlists.get(0).songsBeforeSilence=0;assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(read));
    }
    @Test void sharingRejectsNonFiniteRadiusAndInvalidIdentity(){
        var l=Library.defaults();var data=PlaylistPresets.capture(l,Set.of(l.playlists.get(0).id),false);var c=data.playlists.get(0).conditions;
        c.nearbyPlayerRadius=Double.NaN;assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(data));
        c.nearbyPlayerRadius=32;c.nearbyPlayerUuid="wrong";assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(data));
    }
}

package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DiagnosticSummaryTest {
    private final Library lib=Library.defaults();
    private final PlaybackEngineTest.FakeAudio audio=new PlaybackEngineTest.FakeAudio();
    private final PlaybackEngine engine=new PlaybackEngine(audio,new Random(1));
    private final Context context=new Context("minecraft:overworld","minecraft:forest",false,false,6000,false,15,
        "server:private-server.example:25565",123456,80,-654321,0,.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,
        "minecraft:fishing_rod","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY);
    private DiagnosticSummary.SystemInfo system(){return new DiagnosticSummary.SystemInfo("0.6","26.3","NeoForge","26.3.0.90","25.0.4","Windows 11","amd64",1,.5,128L<<20,256L<<20,1024L<<20);}
    private DiagnosticSummary.Session session(){return new DiagnosticSummary.Session(true,false,true,false,"READING_TAGS",12,50,-1);}
    private String report(String id){return DiagnosticSummary.create(system(),session(),lib,engine,context,Set.of(),2,id);}
    private Playlist day(){return lib.playlist("day_outside");}
    private void song(Playlist p){var t=new Track("private-track-id-"+p.id,"C:\\Users\\PrivateUser\\Music\\Private Song.mp3","Private Song",false);t.artist="Private Artist";lib.tracks.add(t);p.tracks.put(t.id,true);}

    @Test void capturesInstallationPlaybackEnvironmentAndUsefulSettings(){
        song(day());lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.bufferSeconds=0;engine.tick(0,lib,context,Set.of());
        String text=report(day().id);
        assertTrue(text.contains("AFMR: 0.6; Minecraft: 26.3"));assertTrue(text.contains("Loader: NeoForge 26.3.0.90"));
        assertTrue(text.contains("128 / 256 / 1024 (single snapshot)"));assertTrue(text.contains("master / music: 100.00% / 50.00%"));
        assertTrue(text.contains("reason: SILENT_GAP"));assertTrue(text.contains("8.00s / 0.00s"));
        assertTrue(text.contains("biome minecraft:forest"));assertTrue(text.contains("block Y: 80.00"));
        assertTrue(text.contains("main minecraft:fishing_rod"));assertTrue(text.contains("Combat: false; PvP: false"));
        assertTrue(text.contains("missing/unavailable sources: not checked"));assertTrue(text.contains("Inspected playlist: #6"));
    }
    @Test void omitsPrivateNamesPathsAddressesMetadataAndSavedLocationsInNestedGroups(){
        song(day());day().name="My private-server.example:25565 playlist\nInjected report line";
        var group=new PlaylistConditions();group.label="Private group name";group.areaEnabled=true;group.area.configured=true;
        group.area.world="singleplayer:C:\\Users\\PrivateUser\\saves\\PrivateWorld";group.area.x=777888;group.area.y=999111;group.area.z=222333;group.area.radius=444555;
        day().conditions.groups.add(group);lib.profiles.activeId="private-profile-id";lib.profiles.activeName="Private profile name";
        lib.profiles.bindings.put("private-server.example:25565","private-profile-id");
        engine.preview(lib.tracks.getFirst(),0);
        String text=report(day().id);
        for(String secret:List.of("PrivateUser","PrivateWorld","private-server.example","Private Song","Private Artist","private-track-id",
            "Private group name","Private profile name","private-profile-id","Injected report line","123456","654321","777888","999111","222333","444555"))assertFalse(text.contains(secret),secret);
        assertTrue(text.contains("Location: configured=true; matches=false (location details omitted)"));
        assertTrue(text.contains("preview: true"));assertTrue(text.contains("Current track: local file"));assertTrue(text.contains("custom (name omitted)"));
    }
    @Test void errorsNeverCopyRawMessagesOrExceptionPaths(){
        song(day());var track=lib.tracks.getFirst();engine.preview(track,0);
        engine.failed(track.id,"Private Song: AccessDeniedException: C:\\Users\\PrivateUser\\secret.mp3 on private-server.example",1,lib);
        String text=report(day().id);
        assertTrue(text.contains("Playback error: access denied (details omitted)"));assertTrue(text.contains("failed songs this session: 1"));
        assertTrue(text.contains("reason: PLAYBACK_ERROR"));assertFalse(text.contains("PrivateUser"));assertFalse(text.contains("Private Song"));assertFalse(text.contains("private-server.example"));
    }
    @Test void unknownErrorsAreStillUsefulWithoutCopyingTheirContent(){
        assertEquals("audio decoding/playback failure (details omitted)",DiagnosticSummary.errorCategory("unusual error containing /home/secret/name"));
        assertEquals("file unavailable (details omitted)",DiagnosticSummary.errorCategory("NoSuchFileException: \\\\host\\share\\song"));
        assertEquals("memory allocation failure (details omitted)",DiagnosticSummary.errorCategory("OutOfMemoryError: Out of stack space."));
        assertEquals("unsupported audio/input (details omitted)",DiagnosticSummary.errorCategory("UnsupportedAudioFileException: private-song"));
        assertEquals("none",DiagnosticSummary.errorCategory(""));
    }
    @Test void titleScreenDoesNotReportOrEvaluateStaleWorldContext(){
        var s=new DiagnosticSummary.Session(false,false,true,false,"READING_TAGS",0,0,-1);
        String text=DiagnosticSummary.create(system(),s,lib,engine,null,Set.of(),0,day().id);
        assertTrue(text.contains("Session: title screen"));assertTrue(text.contains("reason: NO_WORLD"));assertTrue(text.contains("conditions=not evaluated"));
        assertFalse(text.contains("biome minecraft:forest"));assertFalse(text.contains("private-server"));
    }
    @Test void reportsScanCountsAndRuntimeDisabledWithoutCopyingScanPaths(){
        var s=new DiagnosticSummary.Session(true,true,false,true,"C:\\Users\\PrivateUser\\file.mp3",13,50,7);
        String text=DiagnosticSummary.create(system(),s,lib,engine,context,Set.of(),0,"");
        assertTrue(text.contains("Session: singleplayer; runtime enabled: false"));
        assertTrue(text.contains("Scan: [omitted]; processed 13 / discovered 50; missing/unavailable sources: 7"));assertFalse(text.contains("PrivateUser"));
    }
    @Test void aliasedSelectionsRetainManualOverridesWithAutomaticOff(){
        song(day());lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;
        var combat=lib.playlist("combat");song(combat);combat.enabled=false;
        engine.overridePlaylist(combat.id,OverrideMode.UNTIL_RELEASED,0,lib,Set.of());engine.tick(0,lib,context,Set.of());
        String text=report(combat.id);
        assertTrue(text.contains("source: #1"));assertTrue(text.contains("automatic choice: #6"));assertTrue(text.contains("Override: #1; mode: UNTIL_RELEASED"));
        assertTrue(text.contains("#1: automatic=false"));assertTrue(text.contains("reason: HELD_OVERRIDE"));assertFalse(combat.enabled);
    }
    @Test void nestedConditionsExplainAllAnyNotAndUnavailableStructures(){
        var p=day();p.conditions.groups.clear();var nested=new PlaylistConditions();nested.matchAny=true;nested.negated=true;
        nested.structureEnabled=true;nested.structures.add("minecraft:village");p.conditions.groups.add(nested);
        String text=report(p.id);
        assertTrue(text.contains("root.1: enabled=true; match=ANY; negated=true; result=STRUCTURE_DATA"));
        assertTrue(text.contains("Structure: 1 selected; radius=0.00; data available=false"));
    }
    @Test void newlinesControlCharactersAndPathsCannotEnterThroughVersionOrResourceFields(){
        var sys=new DiagnosticSummary.SystemInfo("0.6\nINJECTED","26.3","C:\\Users\\PrivateUser","https://private-server.example","25\tPRIVATE","Windows 11","amd64",0,0,0,0,0);
        day().conditions.dimensionEnabled=true;day().conditions.dimension="file:///home/PrivateUser";
        String text=DiagnosticSummary.create(sys,session(),lib,engine,context,Set.of(),0,day().id);
        assertFalse(text.contains("INJECTED"));assertFalse(text.contains("PrivateUser"));assertFalse(text.contains("private-server.example"));assertFalse(text.contains("PRIVATE"));
        assertTrue(text.contains("AFMR: [omitted]"));assertTrue(text.contains("Dimension: [omitted]"));
        for(String address:List.of("private-server.example:25565","192.168.1.12:25565","server:private-server.example","file:users/privateuser")){
            day().conditions.dimension=address;assertFalse(report(day().id).contains(address));
        }
    }
    @Test void noLibraryTraversalAndNoPlaybackMutationWithSeventyThousandTracks(){
        var tracks=new LibraryIndexTest.GuardedTracks();lib.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("id"+i,"private path","Private title",false));day().tracks.put("id69999",true);
        lib.rebuildTrackIndex();tracks.locked=true;lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;engine.tick(0,lib,context,Set.of());
        var current=engine.current();long sequence=engine.playbackSequence();var phase=engine.phase();
        String text=report(day().id);
        assertTrue(text.contains("Library: 70000 tracks"));assertTrue(text.contains("playable=true"));
        assertSame(current,engine.current());assertEquals(sequence,engine.playbackSequence());assertEquals(phase,engine.phase());assertTrue(engine.failedTracks().isEmpty());
    }
    @Test void reportCapsPlaylistRowsAndStillIncludesInspectedPlaylistBeyondTheCap(){
        for(int i=0;i<80;i++)lib.createPlaylist("Private name "+i);
        var p=lib.playlists.getLast();String text=report(p.id);
        assertTrue(text.contains("Remaining playlists omitted: 22"));assertTrue(text.contains("Inspected playlist: #86"));assertTrue(text.contains("#86: automatic="));
        assertFalse(text.contains("Private name"));assertTrue(text.length()<=DiagnosticSummary.MAX_CHARACTERS);
    }
    @Test void manyDetailedGroupsKeepClipboardSizeBounded(){
        var p=day();p.conditions.groups.clear();
        for(int i=0;i<7;i++){
            var a=allFilters();p.conditions.groups.add(a);
            for(int j=0;j<8;j++)a.groups.add(allFilters());
        }
        for(int i=0;i<64;i++)lib.createPlaylist("Private name");
        String text=report(p.id);assertTrue(text.length()<=DiagnosticSummary.MAX_CHARACTERS);
        assertTrue(text.contains("Report truncated"));
    }
    private PlaylistConditions allFilters(){
        var p=new PlaylistConditions();p.areaEnabled=p.dimensionEnabled=p.biomeEnabled=p.structureEnabled=p.timeEnabled=p.elevationEnabled=p.temperatureEnabled=p.lightEnabled=true;
        p.cover=p.combat=p.water=PlaylistConditions.State.YES;p.healthEnabled=p.weatherEnabled=p.heldItemEnabled=p.gameModeEnabled=true;return p;
    }
    @Test void nearbyThemeReportsOmitPlayerNamesAndUuids(){
        var c=day().conditions;c.nearbyPlayerEnabled=true;c.nearbyPlayerName="SecretFriend";c.nearbyPlayerUuid="aabbccdd-1234-1234-1234-123456789abc";
        String text=report(day().id);assertTrue(text.contains("Nearby player: radius="));assertFalse(text.contains("SecretFriend"));assertFalse(text.contains("aabbccdd"));
    }
}

package dev.animal.afmr.library;

import com.google.gson.*;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.*;
import dev.animal.afmr.library.PlaylistPresets.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistPresetsTest {
    @TempDir Path directory;
    private static final Gson JSON=new Gson();
    private Library source(){var l=Library.defaults();l.playlists.clear();l.createPlaylist("Shared music");return l;}
    private FileData capture(Library l){return PlaylistPresets.capture(l,new HashSet<>(l.playlists.stream().map(p->p.id).toList()),false);}
    private Path file(){return directory.resolve("setup.afmrp");}
    private FileData roundTrip(FileData data)throws Exception{PlaylistPresets.write(file(),data);return PlaylistPresets.read(file());}
    private static void area(PlaylistConditions c,String world){
        c.areaEnabled=true;c.area.configured=true;c.area.world=world;c.area.dimension="minecraft:the_nether";
        c.area.x=123.5;c.area.y=-24.25;c.area.z=-999;c.area.radius=73;
    }
    @Test void allPlaylistFlagsAndVolumeSurviveRoundTripAndImport()throws Exception{
        var l=source();var p=l.playlists.getFirst();p.name="夜の音楽 — Łódź";p.enabled=false;p.manualOnly=true;p.finishCurrentSong=true;
        p.finishCurrentSongOnExit=true;p.playOnTrigger=true;p.includeVanilla=false;p.volume=0.37;
        var defaults=new Playlist();
        for(var field:Playlist.class.getFields())if(field.getType()==boolean.class)field.setBoolean(p,!field.getBoolean(defaults));
        var data=roundTrip(capture(l));var target=Library.defaults();var ids=PlaylistPresets.apply(target,"default","day_outside",Placement.ABOVE_SELECTED,data,"",null);
        var imported=target.playlist(ids.getFirst());var expected=JSON.toJsonTree(p).getAsJsonObject();var actual=JSON.toJsonTree(imported).getAsJsonObject();
        expected.remove("id");actual.remove("id");assertEquals(expected,actual);assertNotEquals(p.id,imported.id);
    }
    @Test void everyConditionFieldAndNestedAllAnyNotGroupIsPreserved()throws Exception{
        var l=source();var c=l.playlists.getFirst().conditions;
        c.enabled=false;c.negated=true;c.label="Grouped filters";c.matchAny=true;c.weatherEnabled=true;c.weather=Weather.THUNDERSTORM;
        c.dimensionEnabled=true;c.dimension="custom:moon";c.timeEnabled=true;c.fitSongsToTimeWindow=true;c.startTime=23000;c.endTime=2000;
        c.elevationEnabled=true;c.minY=-30;c.maxY=90;c.temperatureEnabled=true;c.minTemperature=-0.4;c.maxTemperature=0.5;
        c.lightEnabled=true;c.minLight=2;c.maxLight=8;c.cover=State.YES;c.minCoverDepth=8;c.water=State.NO;
        c.biomeEnabled=true;c.biomes.add("custom:snow");c.structureEnabled=true;c.structures.add("custom:castle");c.structureRadius=37;
        c.heldItemEnabled=true;c.heldItems.add("custom:wand");c.heldItemHand=Hand.OFF_HAND;
        c.gameModeEnabled=true;c.gameModes=EnumSet.of(GameMode.ADVENTURE,GameMode.CREATIVE);
        c.healthEnabled=true;c.healthComparison=HealthComparison.ABOVE;c.healthThreshold=7.5;c.combat=State.YES;c.combatPlayersOnly=true;
        var group=new PlaylistConditions();group.negated=true;group.label="Nested Not";group.matchAny=true;group.lightEnabled=true;
        group.groups.add(new PlaylistConditions());group.groups.getFirst().water=State.YES;c.groups.add(group);
        var loaded=roundTrip(capture(l));assertEquals(JSON.toJsonTree(c),JSON.toJsonTree(loaded.playlists.getFirst().conditions));
    }
    @Test void noSongsMusicPathsHistoryProfileBindingsOrGlobalSettingsAreExported()throws Exception{
        var l=source();var p=l.playlists.getFirst();
        l.tracks.add(new Track("file:private-song","C:/Users/PrivateUser/Music/secret.mp3","Private song",false));p.tracks.put("file:private-song",false);
        l.settings.silenceMinSeconds=1234;l.folders.add(new Folder("C:/PrivateMusic","home"));l.profiles.bindings.put("server:private.example","default");
        PlaylistPresets.write(file(),capture(l));String text=Files.readString(file());
        for(String privateValue:List.of("PrivateUser","secret.mp3","Private song","private-song","PrivateMusic","private.example","1234","overrideSlots","history"))assertFalse(text.contains(privateValue),privateValue);
        assertTrue(PlaylistPresets.read(file()).playlists.getFirst().tracks.isEmpty());assertFalse(p.tracks.get("file:private-song"));
    }
    @Test void savedLocationsKeepCoordinatesRadiusDimensionAndRebindEveryNestedScope()throws Exception{
        var l=source();var c=l.playlists.getFirst().conditions;area(c,"singleplayer:C:/Users/PrivateUser/saves/Base");
        var nested=new PlaylistConditions();area(nested,"server:hidden.example:25565");nested.enabled=false;c.groups.add(nested);
        var data=roundTrip(capture(l));String text=Files.readString(file());assertFalse(text.contains("PrivateUser"));assertFalse(text.contains("hidden.example"));
        assertEquals(2,PlaylistPresets.locations(data));assertEquals("",data.playlists.getFirst().conditions.area.world);
        var target=Library.defaults();var id=PlaylistPresets.apply(target,"default","",Placement.TOP,data,"server:recipient",null).getFirst();var imported=target.playlist(id).conditions;
        assertEquals("server:recipient",imported.area.world);assertEquals("server:recipient",imported.groups.getFirst().area.world);
        assertEquals(123.5,imported.area.x);assertEquals(-24.25,imported.area.y);assertEquals(-999,imported.area.z);assertEquals(73,imported.area.radius);assertEquals("minecraft:the_nether",imported.area.dimension);
        assertEquals("server:hidden.example:25565",c.groups.getFirst().area.world);assertFalse(imported.groups.getFirst().enabled);
    }
    @Test void configuredDisabledLocationParametersAreRetainedForLaterUse()throws Exception{
        var l=source();var c=l.playlists.getFirst().conditions;area(c,"private");c.areaEnabled=false;
        var target=Library.defaults();var id=PlaylistPresets.apply(target,"default","",Placement.TOP,roundTrip(capture(l)),"recipient",null).getFirst();
        assertFalse(target.playlist(id).conditions.areaEnabled);assertTrue(target.playlist(id).conditions.area.configured);assertEquals("recipient",target.playlist(id).conditions.area.world);
    }
    @Test void locationImportWithoutAWorldFailsBeforeChangingTheDestination()throws Exception{
        var l=source();area(l.playlists.getFirst().conditions,"world");var target=Library.defaults();int before=target.playlists.size();
        assertThrows(IllegalArgumentException.class,()->PlaylistPresets.apply(target,"default","",Placement.TOP,capture(l),"",null));assertEquals(before,target.playlists.size());
    }
    @Test void unconfiguredLocationsDoNotRequireBeingInAWorld()throws Exception{
        var l=source();l.playlists.getFirst().conditions.areaEnabled=true;
        var target=Library.defaults();assertEquals(1,PlaylistPresets.apply(target,"default","",Placement.TOP,capture(l),"",null).size());
    }
    @Test void multiSelectionUsesPriorityOrderRatherThanClickOrder()throws Exception{
        var l=source();var a=l.playlists.getFirst();var b=l.createPlaylist("Second");var c=l.createPlaylist("Third");l.movePlaylist(c.id,0);
        var data=PlaylistPresets.capture(l,new LinkedHashSet<>(List.of(a.id,c.id)),false);
        assertEquals(List.of("Third","Shared music"),data.playlists.stream().map(p->p.name).toList());assertEquals(2,roundTrip(data).playlists.size());
    }
    @Test void allInsertionModesPreserveGroupOrderAndExistingPlaylistIdentity(){
        var l=source();l.createPlaylist("Second");var data=capture(l);
        for(var mode:Placement.values()){
            var target=Library.defaults();var previous=target.playlist("day_outside");int anchor=target.playlists.indexOf(previous),count=target.playlists.size();
            var ids=PlaylistPresets.apply(target,"default",previous.id,mode,data,"",null);int index=mode==Placement.TOP?0:mode==Placement.BOTTOM?count:anchor;
            assertEquals(ids.getFirst(),target.playlists.get(index).id);assertEquals(ids.get(1),target.playlists.get(index+1).id);assertSame(previous,target.playlist(previous.id));
        }
    }
    @Test void deletedAnchorFallsBackToBottom(){
        var l=source();var target=Library.defaults();var ids=PlaylistPresets.apply(target,"default","removed",Placement.ABOVE_SELECTED,capture(l),"",null);assertEquals(ids.getFirst(),target.playlists.getLast().id);
    }
    @Test void importingIntoAnInactiveProfileLeavesTheActiveSetupAndGlobalsUntouched(){
        var target=Library.defaults();String profile=target.profiles.create(target,"Other",null);var active=target.playlists;var settings=target.settings;
        target.settings.silenceMinSeconds=17;target.settings.overrideSlots[0].playlistId="kept";target.settings.widgetScale=1.5;
        PlaylistPresets.apply(target,profile,"day_outside",Placement.TOP,capture(source()),"",null);
        assertSame(active,target.playlists);assertSame(settings,target.settings);assertEquals(6,active.size());assertEquals(17,settings.silenceMinSeconds);assertEquals("kept",settings.overrideSlots[0].playlistId);assertEquals(1.5,settings.widgetScale);
        target.profiles.activate(target,profile);assertEquals("Shared music",target.playlists.getFirst().name);assertEquals(7,target.playlists.size());
    }
    @Test void deletedDestinationProfileFailsWithoutAddingAnything(){
        var target=Library.defaults();assertThrows(IllegalStateException.class,()->PlaylistPresets.apply(target,"deleted","",Placement.TOP,capture(source()),"",null));assertEquals(6,target.playlists.size());
    }
    @Test void repeatedImportsCreateIndependentIdentitiesAndConditions(){
        var data=capture(source());var target=Library.defaults();String a=PlaylistPresets.apply(target,"default","",Placement.TOP,data,"",null).getFirst();String b=PlaylistPresets.apply(target,"default","",Placement.TOP,data,"",null).getFirst();
        assertNotEquals(a,b);target.playlist(a).conditions.label="Edited";assertNotEquals("Edited",target.playlist(b).conditions.label);assertNotEquals("Edited",data.playlists.getFirst().conditions.label);
    }
    @Test void exportSnapshotsStayStableAcrossLaterEditsAndProfileSwitches()throws Exception{
        var l=source();var p=l.playlists.getFirst();p.volume=0.4;var data=capture(l);p.volume=0.9;p.name="Changed";
        String profile=l.profiles.create(l,"Different",null);l.profiles.activate(l,profile);var loaded=roundTrip(data);
        assertEquals("Shared music",loaded.playlists.getFirst().name);assertEquals(0.4,loaded.playlists.getFirst().volume);
    }
    @Test void importedSettingsSurviveOrdinaryLibraryPersistence()throws Exception{
        var target=Library.defaults();String id=PlaylistPresets.apply(target,"default","",Placement.TOP,capture(source()),"",null).getFirst();
        try(var store=new LibraryStore(directory.resolve("library.json"))){store.save(target.snapshot());assertEquals("Shared music",store.load().playlist(id).name);}
    }
    @Test void unicodeUtf8AndUppercaseExtensionAreSupported()throws Exception{
        var l=source();l.playlists.getFirst().name="音楽 🎵";Path p=directory.resolve("日本語.AFMRP");PlaylistPresets.write(p,capture(l));assertEquals("音楽 🎵",PlaylistPresets.read(p).playlists.getFirst().name);
    }
    @Test void successfulOverwriteIsCompleteAndLeavesNoTemporaryFiles()throws Exception{
        var data=capture(source());PlaylistPresets.write(file(),data);data.playlists.getFirst().name="Updated";PlaylistPresets.write(file(),data);
        assertEquals("Updated",PlaylistPresets.read(file()).playlists.getFirst().name);try(var paths=Files.list(directory)){assertEquals(1,paths.count());}
    }
    @Test void invalidExportLeavesAnExistingFileUntouched()throws Exception{
        var data=capture(source());PlaylistPresets.write(file(),data);byte[] before=Files.readAllBytes(file());data.playlists.getFirst().volume=Double.NaN;
        assertThrows(IllegalArgumentException.class,()->PlaylistPresets.write(file(),data));assertArrayEquals(before,Files.readAllBytes(file()));
    }
    @Test void settingsFilesRejectSongAssignmentsAndForeignWorldPaths()throws Exception{
        var root=JSON.toJsonTree(capture(source())).getAsJsonObject();var p=root.getAsJsonArray("playlists").get(0).getAsJsonObject();p.getAsJsonObject("tracks").addProperty("file:foreign",true);
        Files.writeString(file(),root.toString());assertThrows(IOException.class,()->PlaylistPresets.read(file()));p.getAsJsonObject("tracks").remove("file:foreign");
        p.getAsJsonObject("conditions").getAsJsonObject("area").addProperty("world","singleplayer:C:/PrivateUser/world");Files.writeString(file(),root.toString());assertThrows(IOException.class,()->PlaylistPresets.read(file()));
    }
    @Test void unsupportedFieldsEnumsNullAndCoercedBooleansAreRejected()throws Exception{
        String valid=JSON.toJson(capture(source()));
        for(String bad:List.of(valid.replace("\"manualOnly\":false","\"manualOnly\":\"false\""),valid.replace("\"cover\":\"ANY\"","\"cover\":\"UNKNOWN\""),valid.replace("\"conditions\":{","\"conditions\":{\"futureFilter\":true,"),valid.replace("\"conditions\":{","\"conditions\":null,\"discard\":{\"anything\":1,"))){
            Files.writeString(file(),bad);assertThrows(IOException.class,()->PlaylistPresets.read(file()),bad.substring(0,Math.min(100,bad.length())));
        }
    }
    @Test void duplicateFieldsMissingFormatAndMalformedUtf8AreRejected()throws Exception{
        String valid=JSON.toJson(capture(source()));Files.writeString(file(),valid.replace("\"format\":","\"format\":\"afmr-playlists\",\"format\":"));assertThrows(IOException.class,()->PlaylistPresets.read(file()));
        Files.writeString(file(),valid.replace("\"format\":\"afmr-playlists\",",""));assertThrows(IOException.class,()->PlaylistPresets.read(file()));
        Files.write(file(),new byte[]{(byte)0xc3,0x28});assertThrows(IOException.class,()->PlaylistPresets.read(file()));
    }
    @Test void fileSizePlaylistCountAndNestedGroupLimitsAreEnforced()throws Exception{
        try(var sparse=new java.io.RandomAccessFile(file().toFile(),"rw")){sparse.setLength(PlaylistPresets.MAX_BYTES+1L);}assertThrows(IOException.class,()->PlaylistPresets.read(file()));
        var data=capture(source());for(int i=0;i<PlaylistPresets.MAX_PLAYLISTS;i++)data.playlists.add(data.playlists.getFirst());assertThrows(IllegalArgumentException.class,()->PlaylistPresets.validate(data));
        Files.writeString(file(),"{".repeat(50));assertThrows(IOException.class,()->PlaylistPresets.read(file()));
        var l=source();var c=l.playlists.getFirst().conditions;for(int i=0;i<6;i++){var child=new PlaylistConditions();c.groups.add(child);c=child;}assertThrows(IllegalArgumentException.class,()->capture(l));
    }
    @Test void exportAndImportDoNotTraverseTheTrackCatalog(){
        var l=source();l.tracks=new ArrayList<>(){
            @Override public Iterator<Track> iterator(){throw new AssertionError("Visited music catalog");}
            @Override public Track get(int i){throw new AssertionError("Read music catalog");}
            @Override public Spliterator<Track> spliterator(){throw new AssertionError("Streamed music catalog");}
        };
        for(int i=0;i<70000;i++)l.tracks.add(new Track("file:"+i,"private/path/"+i,"Song "+i,false));
        l.playlists.getFirst().tracks=new LinkedHashMap<>(){
            @Override public Set<Map.Entry<String,Boolean>> entrySet(){throw new AssertionError("Read song assignments during settings-only sharing");}
        };
        for(int i=0;i<70000;i++)l.playlists.getFirst().tracks.put("file:"+i,true);
        var data=capture(l);PlaylistPresets.apply(l,"default","",Placement.TOP,data,"",null);assertEquals(2,l.playlists.size());
    }
}

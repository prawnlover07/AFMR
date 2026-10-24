package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions.State;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistTemplatesTest {
    private Context context(String dimension,int time,double y,boolean cover,double depth,int light,boolean combat){
        return new Context(dimension,"test:biome",combat,false,time,cover,light,"world",0,y,0,depth,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false);
    }
    @Test void initialTemplatesReproduceThePreviousRulesAcrossEnvironmentBoundaries(){
        var lib=Library.defaults();
        for(String dim:List.of("minecraft:overworld","minecraft:the_nether","minecraft:the_end","test:dimension"))
        for(int time:new int[]{0,11999,12000,23999})for(int y:new int[]{-100,64,65,200})
        for(boolean cover:List.of(false,true))for(double depth:new double[]{0,4.9,5,100})
        for(int light:new int[]{0,4,5,15})for(boolean combat:List.of(false,true)){
            var c=context(dim,time,y,cover,depth,light,combat);
            boolean below=cover&&depth>=5&&y<=64;
            boolean dark=!dim.equals("minecraft:the_nether")&&!dim.equals("minecraft:the_end")&&cover&&light<=4;
            assertEquals(combat,lib.playlist("combat").matches(c,lib.settings));
            assertFalse(lib.playlist("home").matches(c,lib.settings));
            assertEquals(dark,lib.playlist("dark_exploration").matches(c,lib.settings));
            assertEquals(below,lib.playlist("underground").matches(c,lib.settings));
            assertEquals(time>=12000&&!below,lib.playlist("night_outside").matches(c,lib.settings));
            assertEquals(time<12000&&!below,lib.playlist("day_outside").matches(c,lib.settings));
        }
    }
    @Test void everyTemplateCanBecomeAnyOtherKindOfPlaylist(){
        var lib=Library.defaults();var c=context("test:dimension",6000,200,false,0,15,false);
        for(var p:lib.playlists){
            p.name="Edited "+p.name;p.conditions=new PlaylistConditions();p.conditions.water=State.NO;
            assertTrue(p.matches(c,lib.settings));p.conditions.water=State.YES;assertFalse(p.matches(c,lib.settings));
        }
        assertTrue(lib.removePlaylist("combat"));assertNull(lib.playlist("combat"));
    }
    @Test void cloningOwnsAllMutableConditionsAndAssignmentsButSharesTrackIdentity(){
        var lib=Library.defaults();var source=lib.playlist("dark_exploration");
        source.enabled=false;source.manualOnly=true;source.finishCurrentSong=source.finishCurrentSongOnExit=true;
        source.tracks.put("disabled",false);source.tracks.put("on",true);
        source.conditions.biomeEnabled=true;source.conditions.biomes.add("test:a");
        source.conditions.groups.getFirst().area.configured=true;source.conditions.groups.getFirst().area.world="world";
        var copy=lib.clonePlaylist(source.id,"Branch");
        assertNotEquals(source.id,copy.id);assertEquals(lib.playlists.indexOf(source)+1,lib.playlists.indexOf(copy));
        assertFalse(copy.enabled);assertTrue(copy.manualOnly);assertTrue(copy.finishCurrentSong);assertTrue(copy.finishCurrentSongOnExit);assertFalse(copy.includeVanilla);
        assertEquals(source.tracks,copy.tracks);assertNotSame(source.tracks,copy.tracks);
        copy.tracks.clear();copy.conditions.biomes.clear();copy.conditions.groups.getFirst().area.world="other";
        copy.conditions.groups.getFirst().groups.clear();copy.conditions.groups.get(1).enabled=true;
        assertEquals(2,source.tracks.size());assertEquals(Set.of("test:a"),source.conditions.biomes);
        assertEquals("world",source.conditions.groups.getFirst().area.world);assertEquals(2,source.conditions.groups.getFirst().groups.size());
        assertFalse(source.conditions.groups.get(1).enabled);
        assertThrows(IllegalArgumentException.class,()->lib.clonePlaylist("missing","Copy"));
        assertThrows(IllegalArgumentException.class,()->lib.clonePlaylist(source.id," "));
        assertThrows(IllegalArgumentException.class,()->lib.clonePlaylist(source.id,"x".repeat(81)));
    }
    @Test void autoplayHereReplacesOnlyTheChosenPlaylistsConditions(){
        var lib=Library.defaults();var source=lib.playlist("combat");source.enabled=false;source.manualOnly=true;
        source.includeVanilla=false;source.finishCurrentSong=true;source.tracks.put("song",false);
        var untouched=lib.playlist("home").conditions;var c=context("test:dimension",6000,200,false,0,15,false);
        source.autoplayHere(c);
        assertTrue(source.enabled);assertFalse(source.manualOnly);assertTrue(source.conditions.areaEnabled);
        assertFalse(source.conditions.matchAny);assertFalse(source.conditions.negated);assertEquals(State.ANY,source.conditions.combat);assertTrue(source.conditions.groups.isEmpty());
        assertEquals(64,source.conditions.area.radius);assertTrue(source.matches(c,lib.settings));
        var boundary=new Context(c.dimension(),c.biome(),false,false,6000,false,15,c.world(),0,264,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false);
        assertTrue(source.matches(boundary,lib.settings));source.conditions.area.radius=63.9;assertFalse(source.matches(boundary,lib.settings));
        assertFalse(source.conditions.area.contains(context("other:dimension",6000,200,false,0,15,false)));
        assertSame(untouched,lib.playlist("home").conditions);assertFalse(untouched.area.configured);
        assertFalse(source.includeVanilla);assertTrue(source.finishCurrentSong);assertEquals(Boolean.FALSE,source.tracks.get("song"));
    }
    @Test void autoplayWithoutAWorldDoesNotDiscardConditions(){
        var p=Library.defaults().playlist("combat");var before=p.conditions;
        var c=new Context("","",false,false,0,false,15,"",0,0,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false);
        assertThrows(IllegalArgumentException.class,()->p.autoplayHere(c));assertSame(before,p.conditions);
    }
    @Test void groupsComposeNotAndDisabledBranchesWithoutChangingUnrelatedTemplates(){
        var lib=Library.defaults();var dark=lib.playlist("dark_exploration").conditions;
        var nether=context("minecraft:the_nether",6000,200,false,0,15,false);
        assertFalse(dark.matches(nether,lib.settings));dark.groups.get(1).enabled=true;assertTrue(dark.matches(nether,lib.settings));
        assertFalse(dark.matches(context("minecraft:the_end",6000,200,false,0,15,false),lib.settings));
        var day=lib.playlist("day_outside").conditions;
        assertFalse(day.matches(context("minecraft:overworld",6000,64,true,5,15,false),lib.settings));
        day.groups.getFirst().negated=false;assertTrue(day.matches(context("minecraft:overworld",6000,64,true,5,15,false),lib.settings));
        assertEquals(64,lib.playlist("underground").conditions.maxY);
    }
    @Test void unavailableStructureDataCannotBecomeAMatchThroughNegation(){
        var c=new PlaylistConditions();c.structureEnabled=true;c.structures.add("test:village");c.negated=true;
        var context=context("minecraft:overworld",6000,100,false,0,15,false);var s=new Settings();
        assertEquals(PlaylistConditions.Failure.STRUCTURE_DATA,c.failure(context,s));
        var parent=new PlaylistConditions();parent.negated=true;parent.groups.add(c);
        assertEquals(PlaylistConditions.Failure.STRUCTURE_DATA,parent.failure(context,s));
        parent.matchAny=true;parent.water=State.NO;assertFalse(parent.matches(context,s));
        parent.negated=false;assertTrue(parent.matches(context,s));
        c.enabled=false;parent.water=State.ANY;assertTrue(parent.matches(context,s));
    }
    @Test void groupSnapshotsOwnAllBranchesAndStructureObservationFindsNestedFilters(){
        var lib=Library.defaults();var branch=lib.playlist("day_outside").conditions.groups.getFirst();
        branch.structureEnabled=true;branch.structureRadius=31.2;branch.structures.add("test:village");
        assertEquals(32,lib.playlist("day_outside").conditions.structureSearchRadius());
        var copy=lib.snapshot();branch.structures.clear();branch.enabled=false;
        assertEquals(-1,lib.playlist("day_outside").conditions.structureSearchRadius());
        assertEquals(Set.of("test:village"),copy.playlist("day_outside").conditions.groups.getFirst().structures);
    }
    @Test void invalidGroupTreesAreRejectedRatherThanSilentlyDroppingConditions(){
        var root=new PlaylistConditions();var cursor=root;
        for(int i=0;i<=PlaylistConditions.MAX_DEPTH;i++){var next=new PlaylistConditions();cursor.groups.add(next);cursor=next;}
        assertThrows(IllegalArgumentException.class,root::normalize);
        var wide=new PlaylistConditions();for(int i=0;i<=PlaylistConditions.MAX_GROUPS;i++)wide.groups.add(new PlaylistConditions());
        assertThrows(IllegalArgumentException.class,wide::normalize);
    }
    @Test void deletingATemplateDoesNotBreakExistingAmbientFolderLinks(){
        var lib=Library.defaults();lib.removePlaylist("underground");lib.assignImported(lib.profiles.activeId,"ambient","new");
        assertTrue(lib.playlist("day_outside").tracks.get("new"));assertTrue(lib.playlist("night_outside").tracks.get("new"));
    }
    @Test void inspectorReportsNestedConditionsUsingTheSharedPredicate(){
        var lib=Library.defaults();var c=context("minecraft:the_nether",6000,200,false,0,15,false);
        var engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));
        var row=PlaybackInspector.inspect(lib,c,Set.of(),engine).stream().filter(r->r.id().equals("dark_exploration")).findFirst().orElseThrow();
        assertTrue(row.details().stream().anyMatch(line->line.startsWith("Group 1.1 / Nether:")&&line.endsWith("negated")));
        assertTrue(row.details().stream().anyMatch(line->line.startsWith("Group 2 / Nether:")&&line.endsWith("disabled")));
    }
}

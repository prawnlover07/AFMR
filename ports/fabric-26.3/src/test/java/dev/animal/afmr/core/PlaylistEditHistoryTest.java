package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistEditHistoryTest {
    private final Library lib=Library.defaults();
    private final Playlist p=lib.playlists.get(0);
    private final PlaylistEditHistory history=new PlaylistEditHistory();
    private void edit(){history.watch(lib,p);p.conditions.timeEnabled=true;p.conditions.startTime=123;history.checkpoint(lib);}
    @Test void settingsUndoRetainsIdentityAndDoesNotTouchGlobalSettingsOrSongs(){
        p.tracks.put("disabled",false);double global=lib.settings.silenceMinSeconds;edit();
        assertTrue(history.undo(lib));assertSame(p,lib.playlist(p.id));assertFalse(p.conditions.timeEnabled);assertEquals(Boolean.FALSE,p.tracks.get("disabled"));assertEquals(global,lib.settings.silenceMinSeconds);
        assertTrue(history.redo(lib));assertTrue(p.conditions.timeEnabled);assertEquals(123,p.conditions.startTime);
    }
    @Test void nestedConditionsAndProximityCurvesAreOwnedSnapshots(){
        var group=new PlaylistConditions();group.label="Branch";group.nearbyPlayerEnabled=true;group.proximityVolume.enabled=true;group.proximityVolume.nearVolume=.2;p.conditions.groups.add(group);
        history.watch(lib,p);p.conditions.groups.clear();history.checkpoint(lib);assertTrue(history.undo(lib));assertEquals("Branch",p.conditions.groups.get(0).label);assertEquals(.2,p.conditions.groups.get(0).proximityVolume.nearVolume);
        p.conditions.groups.get(0).proximityVolume.nearVolume=.9;assertTrue(history.redo(lib)==false); // Unrecorded external edits must not be overwritten.
    }
    @Test void newAcceptedEditClearsRedo(){edit();history.undo(lib);p.volume=.3;history.checkpoint(lib);assertFalse(history.canRedo(lib));assertTrue(history.undo(lib));assertEquals(1,p.volume);}
    @Test void numbersCanCoalesceButSeparateActionsRemainSeparate(){
        history.watch(lib,p);history.coalesce("volume");p.volume=.5;history.checkpoint(lib);p.volume=.25;history.checkpoint(lib);
        history.separate();p.playOnTrigger=true;history.checkpoint(lib);history.undo(lib);assertFalse(p.playOnTrigger);assertEquals(.25,p.volume);history.undo(lib);assertEquals(1,p.volume);assertFalse(history.canUndo(lib));
    }
    @Test void noOpSaveDoesNotCreateHistory(){history.watch(lib,p);for(int i=0;i<10;i++)history.checkpoint(lib);assertFalse(history.canUndo(lib));}
    @Test void settingsCheckpointsNeverTraverseAssignmentsOrTheCatalog(){
        p.tracks=new AbstractMap<>(){@Override public Set<Entry<String,Boolean>> entrySet(){throw new AssertionError("assignments traversed");}};
        lib.tracks=new AbstractList<>(){@Override public Track get(int index){throw new AssertionError("catalog traversed");}@Override public int size(){throw new AssertionError("catalog traversed");}};
        edit();assertTrue(history.undo(lib));assertTrue(history.redo(lib));
    }
    @Test void assignmentUndoDistinguishesAbsentFromExplicitOff(){
        p.tracks.put("off",false);history.assign(lib,p,List.of("auto","off"),true);assertTrue(history.undo(lib));assertFalse(p.tracks.containsKey("auto"));assertEquals(Boolean.FALSE,p.tracks.get("off"));assertTrue(history.redo(lib));assertEquals(Boolean.TRUE,p.tracks.get("off"));
        history.assign(lib,p,List.of("off"),null);history.undo(lib);assertEquals(Boolean.TRUE,p.tracks.get("off"));
    }
    @Test void assignmentUndoTouchesOnlyEditedIds(){p.tracks.put("unrelated",false);history.assign(lib,p,List.of("a"),true);p.tracks.put("later",true);history.undo(lib);assertEquals(Boolean.FALSE,p.tracks.get("unrelated"));assertEquals(Boolean.TRUE,p.tracks.get("later"));assertFalse(p.tracks.containsKey("a"));}
    @Test void priorityMovesAreUndoableAndNoOpMovesAreNotRecorded(){assertFalse(history.move(lib,p.id,0));assertFalse(history.canUndo(lib));history.move(lib,p.id,3);history.undo(lib);assertSame(p,lib.playlists.get(0));history.redo(lib);assertSame(p,lib.playlists.get(3));}
    @Test void createdPlaylistCanBeRemovedAndRestoredWithItsTracks(){var clone=lib.clonePlaylist(p.id,"Branch");clone.tracks.put("x",false);history.created(lib,clone);history.undo(lib);assertNull(lib.playlist(clone.id));history.redo(lib);assertEquals(Boolean.FALSE,lib.playlist(clone.id).tracks.get("x"));}
    @Test void deletionRestoresIncomingTransitionsFoldersAndDisabledAssignments(){
        var source=lib.playlists.get(1);var rule=new TransitionRule();rule.customBuffer=true;rule.bufferSeconds=5;source.transitions.put(p.id,rule);lib.folders.add(new Folder("generated",p.id));p.tracks.put("off",false);
        history.delete(lib,p.id);assertFalse(source.transitions.containsKey(p.id));assertEquals("library",lib.folders.get(0).playlistId);history.undo(lib);assertEquals(Boolean.FALSE,lib.playlist(p.id).tracks.get("off"));assertEquals(5,source.transitions.get(p.id).bufferSeconds);assertEquals(p.id,lib.folders.get(0).playlistId);history.redo(lib);assertNull(lib.playlist(p.id));
    }
    @Test void deletingLastPlaylistHasOneUndoStepIncludingThePlaceholder(){lib.playlists=new ArrayList<>(List.of(p));history.delete(lib,p.id);String replacement=lib.playlists.get(0).id;assertNotEquals(p.id,replacement);history.undo(lib);assertEquals(1,lib.playlists.size());assertEquals(p.id,lib.playlists.get(0).id);history.redo(lib);assertEquals(replacement,lib.playlists.get(0).id);}
    @Test void profilesKeepSeparateHistories(){edit();String other=lib.profiles.create(lib,"Other",null);lib.profiles.activate(lib,other);assertFalse(history.canUndo(lib));lib.profiles.activate(lib,PlaylistProfiles.DEFAULT);assertTrue(history.undo(lib));assertFalse(lib.playlist(p.id).conditions.timeEnabled);}
    @Test void boundedHistoryKeepsTheNewestActions(){history.watch(lib,p);for(int i=0;i<150;i++){p.name="Name "+i;history.checkpoint(lib);}int count=0;while(history.undo(lib))count++;assertEquals(128,count);assertEquals("Name 21",p.name);}
    @Test void overlappingUnrecordedAssignmentChangesDoNotGetOverwritten(){history.assign(lib,p,List.of("a"),true);p.tracks.put("a",false);assertFalse(history.undo(lib));assertEquals(Boolean.FALSE,p.tracks.get("a"));assertFalse(history.canUndo(lib));}
}

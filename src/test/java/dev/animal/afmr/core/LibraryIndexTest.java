package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LibraryIndexTest {
    static final class GuardedTracks extends ArrayList<Track> {
        boolean locked;
        @Override public Track get(int index) { if(locked)throw new AssertionError("Steady playback walked the full library");return super.get(index); }
        @Override public Iterator<Track> iterator() { if(locked)throw new AssertionError("Steady playback iterated the full library");return super.iterator(); }
        @Override public Spliterator<Track> spliterator() { if(locked)throw new AssertionError("Steady playback streamed the full library");return super.spliterator(); }
    }
    private final Context outside=new Context("minecraft:overworld","minecraft:forest",false,false,6000,false,15,"",0,64,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,10,false,"","",GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY);
    @Test void steadyPlaybackAndPoolUseAssignmentsWithoutWalkingSeventyThousandTracks() {
        Library library=Library.defaults();GuardedTracks tracks=new GuardedTracks();library.tracks=tracks;
        for(int i=0;i<70000;i++)tracks.add(new Track("file:"+i,"path"+i,"Song "+i,false));
        library.playlist("day_outside").tracks.put("file:69999",true);
        library.rebuildTrackIndex();tracks.locked=true;
        PlaybackEngine engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));
        for(int i=0;i<2000;i++)engine.tick(i/20.0,library,outside,Set.of());
        assertEquals("day_outside",engine.selected().id);
        assertEquals("file:69999",library.pool(engine.selected(),Set.of(),false).getFirst().id);
        assertEquals("",library.playbackUnavailable(outside,Set.of()));
    }
    @Test void poolPreservesDisabledRowsWithoutDuplicatingVanillaOrMissingTracks() {
        Library library=Library.defaults();Playlist p=library.playlist("day_outside");
        library.tracks.add(new Track("custom","file","Custom",false));
        library.tracks.add(new Track("vanilla:a","a","A",true));
        library.tracks.add(new Track("vanilla:b","b","B",true));
        p.tracks.put("custom",false);p.tracks.put("vanilla:a",false);p.tracks.put("missing",true);
        Set<String> vanilla=Set.of("vanilla:a","vanilla:b","unknown");
        assertEquals(List.of("vanilla:b"),library.pool(p,vanilla,false).stream().map(t->t.id).toList());
        assertEquals(Set.of("custom","vanilla:a","vanilla:b"),new HashSet<>(library.pool(p,vanilla,true).stream().map(t->t.id).toList()));
        p.tracks.put("vanilla:b",true);assertEquals(3,library.pool(p,vanilla,true).size());
        assertNull(library.selectPlayable(outside,vanilla,Set.of("vanilla:b")));
    }
    @Test void newImportsAndReplacedMetadataRefreshLookup() {
        Library library=Library.defaults();assertNull(library.track("new"));
        Track track=new Track("new","path","New",false);library.tracks.add(track);
        assertSame(track,library.track("new"));
        Track replacement=new Track("new","path","New metadata",false);library.tracks.set(0,replacement);
        library.rebuildTrackIndex();assertSame(replacement,library.track("new"));
        library.tracks=new ArrayList<>();assertNull(library.track("new"));
    }
    @Test void disabledPlaylistsRemainSilentAtTheEndOfTheGap() {
        Library library=Library.defaults();library.playlists.forEach(p->p.enabled=false);
        library.settings.silenceMinSeconds=library.settings.silenceMaxSeconds=0;
        PlaybackEngine engine=new PlaybackEngine(new PlaybackEngineTest.FakeAudio(),new Random(1));
        engine.tick(0,library,outside,Set.of());engine.tick(10,library,outside,Set.of());
        assertNull(engine.current());assertNull(engine.selected());
    }
}

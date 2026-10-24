package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.ListeningHistory.Origin;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ListeningHistoryTest {
    @Test void historyIsBoundedNewestFirstAndRetainsImmutableMetadata(){
        var history=new ListeningHistory();var track=new Track("song","private-file-path","Initial",false);track.artist="Artist";track.album="Album";
        var playlist=new Playlist("source","Source");
        for(int i=0;i<150;i++){track.title="Song "+i;history.record(track,playlist,Origin.AUTOMATIC,"Reason "+i);}
        var rows=history.snapshot();assertEquals(100,rows.size());assertEquals("Song 149",rows.getFirst().title());assertEquals("Song 50",rows.getLast().title());
        track.title="Changed";playlist.name="Changed";assertEquals("Song 149",rows.getFirst().title());assertEquals("Source",rows.getFirst().playlistName());
        assertEquals("Artist",rows.getFirst().artist());assertEquals("Album",rows.getFirst().album());assertTrue(rows.getFirst().startedAt()>0);assertThrows(UnsupportedOperationException.class,()->rows.clear());
    }
    @Test void restoredHistoryKeepsOrderingAndAllocatesNewUniqueIds(){
        var first=new ListeningHistory();var track=new Track("a","file","A",false);
        first.record(track,null,Origin.PREVIEW,"Manual preview");first.record(track,null,Origin.QUEUE,"Queued");
        var restored=new ListeningHistory();restored.restore(first.snapshot());assertEquals(first.snapshot(),restored.snapshot());
        restored.record(track,null,Origin.PREVIEW,"Again");assertEquals(3,restored.snapshot().getFirst().id());assertEquals(3,restored.size());
    }
    @Test void clearingRetainsNoRowsAndDoesNotResurrectAPlayingSong(){
        var history=new ListeningHistory();var track=new Track("a","file","A",false);history.record(track,null,Origin.PREVIEW,"Preview");
        long revision=history.revision();history.clear();assertTrue(history.snapshot().isEmpty());assertTrue(history.revision()>revision);
        history.record(track,null,Origin.QUEUE,"Queue");assertEquals(1,history.size());assertEquals(2,history.snapshot().getFirst().id());
    }
    @Test void unusuallyLongMetadataIsBoundedWithoutModifyingTheLibrary(){
        var history=new ListeningHistory();var track=new Track("a","file","x".repeat(4000),false);track.artist="a".repeat(4000);
        history.record(track,null,Origin.QUEUE,"r".repeat(10000));var entry=history.snapshot().getFirst();
        assertEquals(512,entry.title().length());assertEquals(512,entry.artist().length());assertEquals(1024,entry.reason().length());assertEquals(4000,track.title.length());assertTrue(ListeningHistory.valid(entry));
    }
}

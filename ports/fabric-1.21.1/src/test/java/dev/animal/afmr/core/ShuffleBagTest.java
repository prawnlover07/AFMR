package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Track;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ShuffleBagTest {
    private List<Track> tracks(int count){var list=new ArrayList<Track>();for(int i=0;i<count;i++)list.add(new Track("t"+i,"file","Song "+i,false));return list;}
    @Test void everySongPlaysOncePerCycleAndBoundariesDoNotRepeat(){
        var pool=tracks(17);var bag=new ShuffleBag();var random=new Random(19);String previous=null;
        for(int cycle=0;cycle<100;cycle++){var seen=new HashSet<String>();for(int i=0;i<pool.size();i++){
            var next=bag.next(pool,previous,random);assertNotEquals(previous,next.id);assertTrue(seen.add(next.id));previous=next.id;
        }assertEquals(17,seen.size());}
    }
    @Test void fisherYatesVisitsDecreasingPrefixBounds(){
        var bounds=new ArrayList<Integer>();var random=new Random(){@Override public int nextInt(int bound){bounds.add(bound);return 0;}};
        new ShuffleBag().next(tracks(7),null,random);assertEquals(List.of(7,6,5,4,3,2),bounds);
    }
    @Test void seededShuffleIsReproducible(){
        var pool=tracks(10);var a=new ShuffleBag();var b=new ShuffleBag();var ra=new Random(9);var rb=new Random(9);String previous=null;
        for(int i=0;i<40;i++){String next=a.next(pool,previous,ra).id;assertEquals(next,b.next(pool,previous,rb).id);previous=next;}
    }
    @Test void newSongsJoinTheUnplayedPartOfTheCycle(){
        var pool=new ArrayList<>(tracks(4));var bag=new ShuffleBag();var random=new Random(2);String first=bag.next(pool,null,random).id;
        pool.add(new Track("new","file","New",false));var rest=new HashSet<String>();
        for(int i=0;i<4;i++){var t=bag.next(pool,first,random);assertNotEquals(first,t.id);assertTrue(rest.add(t.id));}
        assertTrue(rest.contains("new"));
    }
    @Test void removedOrDisabledSongsAreNotDrawnAndPlayedHistoryIsKept(){
        var pool=new ArrayList<>(tracks(5));var bag=new ShuffleBag();var random=new Random(3);String first=bag.next(pool,null,random).id;
        String removed=pool.stream().map(t->t.id).filter(id->!id.equals(first)).findFirst().orElseThrow();pool.removeIf(t->t.id.equals(removed));
        var rest=new HashSet<String>();for(int i=0;i<3;i++){var t=bag.next(pool,first,random);assertNotEquals(first,t.id);assertNotEquals(removed,t.id);assertTrue(rest.add(t.id));}
    }
    @Test void metadataRefreshUsesTheCurrentTrackObject(){
        var pool=tracks(2);var bag=new ShuffleBag();var random=new Random(1);var first=bag.next(pool,null,random);
        String other=pool.stream().filter(t->t!=first).findFirst().orElseThrow().id;var updated=new Track(other,"new-path","New metadata",false);
        assertSame(updated,bag.next(List.of(first,updated),first.id,random));
    }
    @Test void emptyEligibilityDoesNotEraseUnplayedHistory(){
        var pool=tracks(4);var bag=new ShuffleBag();var random=new Random(1);var first=bag.next(pool,null,random);
        assertNull(bag.next(List.of(),first.id,random));assertNotEquals(first.id,bag.next(pool,first.id,random).id);
    }
    @Test void oneSongCanRepeatAndDuplicateInputIdsAreDeduplicated(){
        var t=tracks(1).getFirst();var bag=new ShuffleBag();var random=new Random(1);
        for(int i=0;i<5;i++)assertSame(t,bag.next(List.of(t,t),t.id,random));
    }
    @Test void reshuffleKeepsIndependentPlaylistHistory(){
        var pool=tracks(8);var a=new ShuffleBag();var b=new ShuffleBag();var random=new Random(1);var first=a.next(pool,null,random);
        for(int i=0;i<20;i++)b.next(pool,null,random);
        var rest=new HashSet<String>();for(int i=0;i<7;i++){var next=a.next(pool,null,random);assertNotEquals(first.id,next.id);assertTrue(rest.add(next.id));}
    }
}

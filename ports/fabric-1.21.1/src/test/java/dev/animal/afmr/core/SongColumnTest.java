package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Track;
import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SongColumnTest {
    @TempDir Path directory;
    private Track track(String number){var t=new Track(number,"file:"+number,number,false);t.trackNumber=number;return t;}
    @Test void trackAndDiscNumbersUseNumericOrderAndRetainTagText(){
        var tracks=new ArrayList<>(List.of(track("10"),track("02/12"),track("1"),track("")));
        tracks.sort(SongColumn.TRACK_NUMBER.comparator(false));assertEquals(List.of("1","02/12","10",""),tracks.stream().map(t->t.trackNumber).toList());
        assertEquals("02/12",SongColumn.TRACK_NUMBER.text(tracks.get(1)));
        tracks.get(0).discNumber="10";tracks.get(1).discNumber="2";
        assertTrue(SongColumn.DISC_NUMBER.comparator(false).compare(tracks.get(0),tracks.get(1))>0);
    }
    @Test void missingValuesSortLastInEitherDirection(){
        var missing=track("");var present=track("2");missing.artist="";present.artist="Artist";
        for(boolean descending:List.of(false,true)){
            assertTrue(SongColumn.TRACK_NUMBER.comparator(descending).compare(missing,present)>0);
            assertTrue(SongColumn.ARTIST.comparator(descending).compare(missing,present)>0);
            assertTrue(SongColumn.RG_TRACK_GAIN.comparator(descending).compare(missing,present)==0);
        }
    }
    @Test void gainAndPeakDisplayTagsSeparatelyFromMeasuredGain(){
        var t=track("1");t.replayGain=new ReplayGainInfo(-8.25,0.9,-7.0,1.1);t.loudness=new TrackLoudness(10,"stamp",-3,0.8);
        assertEquals("-8.25 dB",SongColumn.RG_TRACK_GAIN.text(t));assertEquals("0.900000",SongColumn.RG_TRACK_PEAK.text(t));
        assertEquals("-7.00 dB",SongColumn.RG_ALBUM_GAIN.text(t));assertEquals("1.100000",SongColumn.RG_ALBUM_PEAK.text(t));
        assertEquals("-3.00 dB",SongColumn.AFMR_GAIN.text(t));t.replayGain=new ReplayGainInfo(0d,null,null,null);
        assertEquals("+0.00 dB",SongColumn.RG_TRACK_GAIN.text(t));assertEquals("",SongColumn.RG_ALBUM_GAIN.text(t));
    }
    @Test void gainSortingUsesValuesRatherThanFormattedStrings(){
        var a=track("a");var b=track("b");a.replayGain=new ReplayGainInfo(-10d,null,null,null);b.replayGain=new ReplayGainInfo(-2d,null,null,null);
        assertTrue(SongColumn.RG_TRACK_GAIN.comparator(false).compare(a,b)<0);
        assertTrue(SongColumn.RG_TRACK_GAIN.comparator(true).compare(a,b)>0);
    }
    @Test void durationMissingMetadataAndVanillaRemainReadable(){
        var t=new Track("vanilla","minecraft:test","Vanilla",true);t.durationSeconds=125.9;
        assertEquals("2:05",SongColumn.DURATION.text(t));t.durationSeconds=Double.NaN;assertEquals("--:--",SongColumn.DURATION.text(t));
        assertEquals("",SongColumn.ALBUM_ARTIST.text(t));assertEquals("",SongColumn.RG_TRACK_GAIN.text(t));assertEquals("minecraft:test",SongColumn.PATH.text(t));
    }
    @Test void visibilityKeepsTitleAndDefaultsWithoutMutatingTheInput(){
        assertEquals(EnumSet.of(SongColumn.TITLE,SongColumn.ARTIST,SongColumn.ALBUM,SongColumn.DURATION),SongColumn.normalize(null));
        var source=new HashSet<SongColumn>();source.add(null);source.add(SongColumn.GENRE);
        assertEquals(EnumSet.of(SongColumn.TITLE,SongColumn.GENRE),SongColumn.normalize(source));assertFalse(source.contains(SongColumn.TITLE));
        assertEquals(EnumSet.of(SongColumn.TITLE),SongColumn.normalize(Set.of()));
    }
    @Test void columnPreferencesAndImportedMetadataHaveOwnedPersistentSnapshots() throws Exception {
        var lib=Library.defaults();var t=track("03/12");t.albumArtist="Album artist";t.discNumber="2/2";t.genre="Ambient";t.date="2026-10-22";
        t.replayGain=new ReplayGainInfo(-6d,0.8,-8d,0.9);lib.tracks.add(t);lib.settings.songColumns=EnumSet.allOf(SongColumn.class);
        var snapshot=lib.snapshot();lib.settings.songColumns.clear();t.albumArtist="Changed";t.replayGain=null;
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(snapshot);var loaded=store.load();var copy=loaded.track(t.id);
            assertEquals(EnumSet.allOf(SongColumn.class),loaded.settings.songColumns);assertEquals("Album artist",copy.albumArtist);
            assertEquals("03/12",copy.trackNumber);assertEquals("2/2",copy.discNumber);assertEquals("Ambient",copy.genre);assertEquals("2026-10-22",copy.date);
            assertEquals(-6,copy.replayGain.trackDb());assertEquals(0.9,copy.replayGain.albumPeak());
        }
    }
    @Test void missingFieldsNormalizeForRendering(){
        var lib=new Library();var t=track("2");t.albumArtist=t.discNumber=t.trackNumber=t.genre=t.date=null;lib.tracks.add(t);lib.settings.songColumns=null;
        lib.normalize();for(var column:SongColumn.values())assertNotNull(column.text(t));assertEquals(SongColumn.defaults(),lib.settings.songColumns);
    }
}

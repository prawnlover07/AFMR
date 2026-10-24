package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.LibraryStore;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class NowPlayingWidgetTest {
    @TempDir Path directory;

    private Track tagged(){
        var track=new Track("test","fixture","Long / Unicode ♫ title",false);
        track.artist="Artist";track.album="Album";track.durationSeconds=245.9;return track;
    }
    private Settings allInfo(){
        var s=new Settings();s.widgetAlbum=s.widgetDuration=s.widgetPlaylist=true;return s;
    }

    @Test void enabledInformationUsesLiteralMetadataAndOriginalSource(){
        var info=new NowPlayingInfo(tagged(),"Long / Unicode ♫ title","Original source",allInfo());
        assertArrayEquals(new String[]{"Long / Unicode ♫ title","Artist","Album","Length: 4:05","Playlist: Original source"},info.lines);
    }

    @Test void missingMetadataAndUnknownDurationDoNotLeaveEmptyRows(){
        var track=new Track("vanilla","minecraft:music/game/sweden","Unused raw title",true);
        var info=new NowPlayingInfo(track,"Sweden","",allInfo());
        assertArrayEquals(new String[]{"Sweden"},info.lines);
        track.durationSeconds=Double.NaN;
        assertArrayEquals(info.lines,new NowPlayingInfo(track,"Sweden","",allInfo()).lines);
    }

    @Test void eachInformationSwitchIsIndependent(){
        var s=allInfo();s.widgetTitle=s.widgetArtist=false;
        assertArrayEquals(new String[]{"Album","Length: 4:05","Playlist: Preview"},new NowPlayingInfo(tagged(),"title","Preview",s).lines);
        s.widgetAlbum=s.widgetDuration=s.widgetPlaylist=false;
        assertEquals(0,new NowPlayingInfo(tagged(),"title","Preview",s).lines.length);
    }

    @Test void savedSnapshotOwnsEveryWidgetPreference() throws Exception {
        var lib=Library.defaults();var s=lib.settings;
        assertEquals(0x000000,s.widgetBackgroundColor);assertEquals(0.92,s.widgetBackgroundOpacity);
        s.nowPlayingStyle=NowPlayingStyle.HYBRID;s.widgetScale=1.75;s.widgetWidth=410;s.popupSeconds=19;
        s.widgetBackgroundColor=0x2060A0;s.widgetBackgroundOpacity=0.35;
        s.widgetArtwork=s.widgetTitle=s.widgetArtist=false;
        s.widgetAlbum=s.widgetDuration=s.widgetPlaylist=s.widgetScroll=s.widgetWrap=true;
        var copy=lib.snapshot();
        s.nowPlayingStyle=NowPlayingStyle.MINECRAFT;s.widgetScale=1;s.widgetWidth=260;s.popupSeconds=7;
        s.widgetBackgroundColor=0;s.widgetBackgroundOpacity=0.92;
        s.widgetArtwork=s.widgetTitle=s.widgetArtist=true;
        s.widgetAlbum=s.widgetDuration=s.widgetPlaylist=s.widgetScroll=s.widgetWrap=false;
        copy.settings.restoreMinecraftPlaybackDefaults();
        try(var store=new LibraryStore(directory.resolve("widget.json"))){
            store.save(copy);var restored=store.load().settings;
            assertEquals(NowPlayingStyle.HYBRID,restored.nowPlayingStyle);
            assertEquals(1.75,restored.widgetScale);assertEquals(410,restored.widgetWidth);assertEquals(19,restored.popupSeconds);
            assertEquals(0x2060A0,restored.widgetBackgroundColor);assertEquals(0.35,restored.widgetBackgroundOpacity);
            assertFalse(restored.widgetArtwork);assertFalse(restored.widgetTitle);assertFalse(restored.widgetArtist);
            assertTrue(restored.widgetAlbum);assertTrue(restored.widgetDuration);assertTrue(restored.widgetPlaylist);assertTrue(restored.widgetScroll);assertTrue(restored.widgetWrap);
        }
    }

    @Test void normalizationBoundsSizeAndRejectsNonFiniteScale(){
        var lib=Library.defaults();var s=lib.settings;
        s.widgetScale=Double.NaN;s.widgetWidth=-10;lib.normalize();
        assertEquals(1,s.widgetScale);assertEquals(140,s.widgetWidth);
        s.widgetScale=Double.POSITIVE_INFINITY;lib.normalize();assertEquals(1,s.widgetScale);
        s.widgetScale=3;s.widgetWidth=999;lib.normalize();assertEquals(2,s.widgetScale);assertEquals(600,s.widgetWidth);
        s.widgetScale=0.1;lib.normalize();assertEquals(0.5,s.widgetScale);
    }

    @Test void backgroundOpacityAndRgbStayWithinTheirSupportedRanges(){
        var lib=Library.defaults();var s=lib.settings;
        s.widgetBackgroundColor=-1;s.widgetBackgroundOpacity=-0.2;lib.normalize();
        assertEquals(0,s.widgetBackgroundColor);assertEquals(0,s.widgetBackgroundOpacity);
        s.widgetBackgroundColor=0x1000000;s.widgetBackgroundOpacity=2;lib.normalize();
        assertEquals(0xFFFFFF,s.widgetBackgroundColor);assertEquals(1,s.widgetBackgroundOpacity);
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}){
            s.widgetBackgroundOpacity=invalid;lib.normalize();assertEquals(0.92,s.widgetBackgroundOpacity);
        }
        s.widgetBackgroundColor=0x123456;s.widgetBackgroundOpacity=0.375;lib.normalize();
        assertEquals(0x123456,s.widgetBackgroundColor);assertEquals(0.375,s.widgetBackgroundOpacity);
    }

    @Test void fullSizedWidgetsRespectRequestedScaleAndWidth(){
        var layout=NowPlayingLayout.fit(260,1.5,3,true,true,1000,500);
        assertEquals(260,layout.width());assertEquals(1.5,layout.scale());assertEquals(24,layout.iconSize());
        assertTrue(layout.height()>=3*14+9);
    }

    @Test void largeWidgetsFitNarrowAndShortViewportsWithEveryFieldCombination(){
        for(boolean hybrid:new boolean[]{true,false})for(boolean artwork:new boolean[]{true,false})for(int rows=0;rows<=6;rows++){
            var layout=NowPlayingLayout.fit(600,2,rows,artwork,hybrid,180,64);
            assertTrue(layout.width()*layout.scale()<=180.000001);
            assertTrue(layout.height()*layout.scale()<=64.000001);
            assertTrue(layout.width()-layout.textX()-7>0);
        }
    }

    @Test void hidingArtworkReclaimsTextSpace(){
        var art=NowPlayingLayout.fit(140,1,1,true,false,400,200);
        var text=NowPlayingLayout.fit(140,1,1,false,false,400,200);
        assertEquals(0,text.iconSize());assertTrue(text.textX()<art.textX());assertTrue(text.height()<art.height());
    }

    @Test void scrollingPausesAtBothEndsAndReturnsToStart(){
        // 120 pixels overflow takes five seconds in each direction, plus the pauses.
        assertEquals(0,NowPlayingLayout.scrollOffset(220,100,1.5));
        assertEquals(24,NowPlayingLayout.scrollOffset(220,100,2.5));
        assertEquals(120,NowPlayingLayout.scrollOffset(220,100,6.5));
        assertEquals(120,NowPlayingLayout.scrollOffset(220,100,7.5));
        assertEquals(96,NowPlayingLayout.scrollOffset(220,100,9));
        assertEquals(0,NowPlayingLayout.scrollOffset(220,100,13));
    }

    @Test void scrollingNeverExposesBlankSpaceBeyondTheText(){
        for(int overflow:new int[]{1,10,120,10000})for(double age=0;age<100;age+=0.13){
            double offset=NowPlayingLayout.scrollOffset(100+overflow,100,age);
            assertTrue(offset>=0&&offset<=overflow);
        }
        assertEquals(0,NowPlayingLayout.scrollOffset(80,100,10));
        assertEquals(0,NowPlayingLayout.scrollOffset(200,100,Double.NaN));
    }

    private NowPlayingInfo titleOnly(String title){
        var s=new Settings();s.widgetArtist=false;
        return new NowPlayingInfo(tagged(),title,"",s);
    }

    @Test void wrappingBreaksOnlyBetweenWordsAndUsesTheAvailableWidth(){
        var info=titleOnly("One two three four");
        assertArrayEquals(new String[]{"One two","three","four"},info.wrap(7,String::length));
        assertArrayEquals(new String[]{"One two three four"},info.wrap(100,String::length));
        assertArrayEquals(new String[]{"One two","three","four"},titleOnly(" One  two\tthree\u2003four ").wrap(7,String::length));
    }

    @Test void oversizedWordsAndUnicodeCodePointsStayIntact(){
        assertArrayEquals(new String[]{"extraordinary","melody"},titleOnly("extraordinary melody").wrap(6,String::length));
        assertArrayEquals(new String[]{"夜の歌","🎵","remix"},titleOnly("夜の歌 🎵 remix").wrap(3,s->s.codePointCount(0,s.length())));
    }

    @Test void everyWrapWidthPreservesAllWordsInOrder(){
        String title="A song with several longer words and an extraordinary ending";
        for(int width=1;width<=80;width++){
            String[] lines=titleOnly(title).wrap(width,String::length);
            assertEquals(title,String.join(" ",lines));
            for(String line:lines)assertTrue(line.length()<=width||!line.contains(" "));
        }
    }

    @Test void wrappedContentAddsHeightWithoutExpandingTheWidget(){
        var info=titleOnly("A very long song title that needs several rows inside a narrow widget");
        var single=NowPlayingLayout.fit(140,1,info.lines.length,true,false,1000,1000);
        String[] wrapped=info.wrap(single.width()-single.textX()-7,s->s.length()*6);
        var multi=NowPlayingLayout.fit(140,1,wrapped.length,true,false,1000,1000);
        assertTrue(wrapped.length>1);assertEquals(single.width(),multi.width());assertTrue(multi.height()>single.height());
    }
}

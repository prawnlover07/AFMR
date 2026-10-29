package dev.animal.afmr.ui;

import dev.animal.afmr.core.SongColumn;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SongTableLayoutTest {
    @Test void defaultsFillTheUsualTableWithoutHorizontalScrolling(){
        for(int width:new int[]{478,600,1200}){
            var table=new SongTableLayout(SongColumn.defaults(),width,100);
            assertFalse(table.overflow());assertEquals(0,table.scroll());assertEquals(table.viewport,table.total);
            assertEquals(-1,table.columnAt(0));assertEquals(0,table.columnAt(SongTableLayout.PINNED));assertEquals(3,table.columnAt(width-1));assertEquals(-1,table.columnAt(width));
        }
    }
    @Test void extraColumnsKeepTheirWidthsAndHitTestingFollowsTheScroll(){
        var table=new SongTableLayout(EnumSet.allOf(SongColumn.class),478,0);assertTrue(table.overflow());
        table.scroll(Integer.MAX_VALUE);assertEquals(table.maxScroll(),table.scroll());
        assertEquals(table.columns.length-1,table.columnAt(477));assertEquals(SongColumn.PATH,table.columns[table.columnAt(477)]);
        for(int i=1;i<table.columns.length;i++)assertEquals(table.columns[i].width,table.widths[i]);
    }
    @Test void scrollbarEndpointsAndNarrowWindowsStayBounded(){
        for(int width:new int[]{29,36,100,478,2000}){
            var table=new SongTableLayout(EnumSet.allOf(SongColumn.class),width,0);
            assertTrue(table.thumbWidth()<=table.viewport);table.dragThumb(-1000);assertEquals(0,table.scroll());
            table.dragThumb(10000);assertEquals(table.viewport==table.thumbWidth()?0:table.maxScroll(),table.scroll());
            assertTrue(table.thumbX()+table.thumbWidth()<=table.viewport);
        }
    }
    @Test void resizedOrReducedColumnsClampTheRetainedOffset(){
        var wide=new SongTableLayout(EnumSet.allOf(SongColumn.class),478,1000);
        assertEquals(1000,wide.scroll());var reduced=new SongTableLayout(Set.of(SongColumn.TITLE),478,wide.scroll());
        assertEquals(0,reduced.scroll());assertFalse(reduced.overflow());
    }
}

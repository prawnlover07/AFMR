package dev.animal.afmr.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceLayoutTest {
    @Test void fitsGameWindowsAndGuiScales(){
        for(int[] size:new int[][]{{160,120},{320,240},{427,240},{854,480},{1280,720},{1920,1080},{3840,2160},{500,1000}}){
            var l=WorkspaceLayout.fit(size[0],size[1]);
            assertTrue(l.scale()>0&&l.scale()<=1);
            assertEquals(size[0],l.width()*l.scale(),1);
            assertEquals(size[1],l.height()*l.scale(),1);
            assertTrue(l.tableWidth()>=420,"Song actions and metadata must fit at "+size[0]+"x"+size[1]);
            assertTrue(l.libraryListHeight()>=84,"At least six library rows must be visible");
            assertTrue(l.songListHeight()>=140,"At least ten playlist rows must be visible");
        }
    }
    @Test void libraryAndPriorityStaySeparateFromTheSongEditor(){
        var l=WorkspaceLayout.fit(854,480);
        assertTrue(l.tableX()>l.sidebarWidth());
        assertTrue(l.tableX()+l.tableWidth()<l.rulesX());
        assertTrue(l.rulesX()+l.rulesWidth()<l.width());
        assertTrue(94+l.priorityHeight()+52<l.libraryY());
        assertTrue(l.libraryListY()+l.libraryListHeight()<l.footerY()-22);
        assertTrue(l.songListY()+l.songListHeight()<l.footerY()-32);
    }
}

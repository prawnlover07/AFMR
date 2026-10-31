package dev.animal.afmr.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceLayoutTest {
    @Test void pixelFactorsRespectTheWindowAndGuiScale(){
        for(int gui=1;gui<=4;gui++){
            assertPixels(1920,1080,gui,Math.min(2,gui));
            assertPixels(3840,2160,gui,gui);
        }
    }
    @Test void smallScreensKeepLargerFractionalText(){
        assertPixels(1366,768,1,1);
        for(int gui=2;gui<=4;gui++)assertPixels(1366,768,gui,768.0/516);
        assertPixels(2560,1440,2,2);assertPixels(2560,1440,3,1440.0/516);
    }
    private void assertPixels(int width,int height,int gui,double pixels){
        var l=WorkspaceLayout.fit(width,height,gui);
        assertEquals(pixels,l.scale()*gui,1e-6);assertTrue(l.scale()<=1);
        assertEquals((int)(width/pixels),l.width(),1);assertEquals((int)(height/pixels),l.height(),1);
    }
    @Test void tinyWindowsKeepTheFractionalFallback(){
        var l=WorkspaceLayout.fit(320,240,4);
        assertEquals(320.0/900,l.scale()*4,1e-6);assertEquals(900,l.width());assertTrue(l.height()>=516);
        assertEquals(320,l.width()*l.scale()*4,1);assertEquals(240,l.height()*l.scale()*4,1);
        assertTrue(WorkspaceLayout.fit(0,0,1).scale()>0);
    }
    @Test void minimumCanvasKeepsControlsAndScrollableListsSeparate(){
        for(int[] size:new int[][]{{160,120},{320,240},{854,480},{900,516},{1280,720},{1366,768},{1920,1080},{2560,1440},{3840,2160},{500,1000}}){
            var l=WorkspaceLayout.fit(size[0],size[1],4);
            assertTrue(l.tableWidth()>=350);assertTrue(l.tableWidth()-184>=166,"Song search width");
            assertTrue(l.tableWidth()>=288,"Playlist actions, Undo and Redo");
            assertTrue(l.libraryListHeight()>=84,"Six library rows");assertTrue(l.songListHeight()>=140);
            assertTrue(l.tableX()>l.sidebarWidth());assertTrue(l.tableX()+l.tableWidth()<l.rulesX());
            assertTrue(l.rulesX()+l.rulesWidth()<l.width());assertTrue(94+l.priorityHeight()+52<l.libraryY());
            assertTrue(l.libraryListY()+l.libraryListHeight()<l.footerY()-22);
            assertTrue(l.songListY()+l.songListHeight()<=l.footerY()-8);
            int tabRight=12;for(var page:WorkspacePage.values())tabRight+=page.width+4;
            assertTrue(tabRight<=l.width()-12);assertTrue(l.width()/2+121<l.width()-232,"Transport/source gap");
        }
    }
    @Test void snappingOnlyWhenItCostsAtMostFifteenPercent(){
        assertPixels(1800,1032,4,2);assertPixels(2115,1212,4,2);
        assertPixels(1799,1032,4,1799.0/900);assertPixels(2200,1300,4,2200.0/900);
        assertThrows(IllegalArgumentException.class,()->WorkspaceLayout.fit(1920,1080,0));
    }
}

package dev.animal.afmr.core;

import org.junit.jupiter.api.Test;
import static dev.animal.afmr.core.MenuImageLayout.Fit.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuImageLayoutTest {
    @Test void fillCropsTheCenterWithoutOversizedDraws(){
        assertEquals(new MenuImageLayout(0,0,400,300,200,0,800,600),MenuImageLayout.calculate(1200,600,400,300,FILL));
        assertEquals(new MenuImageLayout(0,0,400,300,0,375,600,450),MenuImageLayout.calculate(600,1200,400,300,FILL));
    }
    @Test void fitShowsTheEntireImageAndCentersTheGaps(){
        assertEquals(new MenuImageLayout(0,50,400,200,0,0,1200,600),MenuImageLayout.calculate(1200,600,400,300,FIT));
        assertEquals(new MenuImageLayout(125,0,150,300,0,0,600,1200),MenuImageLayout.calculate(600,1200,400,300,FIT));
    }
    @Test void stretchUsesTheWholeImageAndViewport(){
        assertEquals(new MenuImageLayout(0,0,400,300,0,0,1200,600),MenuImageLayout.calculate(1200,600,400,300,STRETCH));
    }
    @Test void extremeAspectRatiosAndSmallWindowsRemainInsideBounds(){
        for(var fit:values())for(int iw:new int[]{1,2,127,2048})for(int ih:new int[]{1,2,127,2048})
            for(int w:new int[]{1,17,853})for(int h:new int[]{1,33,480}){
                var r=MenuImageLayout.calculate(iw,ih,w,h,fit);
                assertTrue(r.x()>=0&&r.y()>=0&&r.x()+r.width()<=w&&r.y()+r.height()<=h);
                assertTrue(r.width()>0&&r.height()>0&&r.sourceWidth()>0&&r.sourceHeight()>0);
                assertTrue(r.u()>=0&&r.v()>=0&&r.u()+r.sourceWidth()<=iw&&r.v()+r.sourceHeight()<=ih);
            }
    }
    private static MenuImageLayout.Fit[] values(){return MenuImageLayout.Fit.values();}
    @Test void emptyDimensionsAreRejected(){assertThrows(IllegalArgumentException.class,()->MenuImageLayout.calculate(0,10,400,300,FILL));}
}

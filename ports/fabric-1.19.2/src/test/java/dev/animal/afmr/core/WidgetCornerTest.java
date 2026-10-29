package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WidgetCornerTest {
    @Test void defaultsPreserveExistingPositions(){
        assertEquals(8,NowPlayingLayout.anchorX(WidgetCorner.DEFAULT,260,1280));
        assertEquals(654,NowPlayingLayout.anchorY(WidgetCorner.DEFAULT,260,54,1280,720));
    }
    @Test void allFourCornersAnchorTheScaledWidgetInsideTheViewport(){
        for(var corner:WidgetCorner.values()){
            float x=NowPlayingLayout.anchorX(corner,390,1280),y=NowPlayingLayout.anchorY(corner,390,81,1280,720);
            assertTrue(x>=0&&x+390<=1280);assertTrue(y>=0&&y+81<=720);
            if(corner.right())assertEquals(882,x);
            if(corner!=WidgetCorner.DEFAULT&&!corner.right())assertEquals(8,x);
        }
    }
    @Test void bottomCornersAvoidTheHotbarOnNarrowWindows(){
        for(var corner:new WidgetCorner[]{WidgetCorner.BOTTOM_LEFT,WidgetCorner.BOTTOM_RIGHT}){
            assertEquals(128,NowPlayingLayout.anchorY(corner,260,54,427,240));
            assertTrue(NowPlayingLayout.anchorX(corner,260,427)>=0);
        }
    }
}

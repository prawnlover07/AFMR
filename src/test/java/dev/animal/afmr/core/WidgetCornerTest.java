package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WidgetCornerTest {
    @Test void defaultsPreserveExistingPositions(){
        assertEquals(0,NowPlayingLayout.anchorX(NowPlayingStyle.HYBRID,WidgetCorner.DEFAULT,260,1280));
        assertEquals(0,NowPlayingLayout.anchorY(NowPlayingStyle.HYBRID,WidgetCorner.DEFAULT,260,54,1280,720));
        assertEquals(8,NowPlayingLayout.anchorX(NowPlayingStyle.AFMR,WidgetCorner.DEFAULT,260,1280));
        assertEquals(654,NowPlayingLayout.anchorY(NowPlayingStyle.AFMR,WidgetCorner.DEFAULT,260,54,1280,720));
    }
    @Test void allFourCornersAnchorTheScaledWidgetInsideTheViewport(){
        for(var style:NowPlayingStyle.values())for(var corner:WidgetCorner.values()){
            float x=NowPlayingLayout.anchorX(style,corner,390,1280),y=NowPlayingLayout.anchorY(style,corner,390,81,1280,720);
            assertTrue(x>=0&&x+390<=1280);assertTrue(y>=0&&y+81<=720);
            if(corner.right())assertEquals(882,x);
            if(corner!=WidgetCorner.DEFAULT&&!corner.right())assertEquals(8,x);
        }
    }
    @Test void bottomCornersAvoidTheHotbarOnNarrowWindows(){
        for(var corner:new WidgetCorner[]{WidgetCorner.BOTTOM_LEFT,WidgetCorner.BOTTOM_RIGHT}){
            assertEquals(128,NowPlayingLayout.anchorY(NowPlayingStyle.AFMR,corner,260,54,427,240));
            assertTrue(NowPlayingLayout.anchorX(NowPlayingStyle.AFMR,corner,260,427)>=0);
        }
    }
}

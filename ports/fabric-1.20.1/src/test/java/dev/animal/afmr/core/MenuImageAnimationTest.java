package dev.animal.afmr.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuImageAnimationTest {
    @Test void respectsVariableDelaysAndLoopsAtExactBoundary(){
        var clock=new MenuImageAnimation(new int[]{100,200,300});
        assertEquals(0,clock.frame(0));assertEquals(0,clock.frame(99));assertEquals(1,clock.frame(100));
        assertEquals(1,clock.frame(299));assertEquals(2,clock.frame(300));assertEquals(2,clock.frame(599));
        assertEquals(0,clock.frame(600));assertEquals(1,clock.frame(700));assertEquals(2,clock.frame(12_000_500));
    }
    @Test void stationaryImageAndNegativeTimeAreStable(){
        assertEquals(0,new MenuImageAnimation(new int[]{10}).frame(Long.MAX_VALUE));
        assertEquals(0,new MenuImageAnimation(new int[]{10,20}).frame(-1));
    }
    @Test void emptyOrNonpositiveDurationsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new MenuImageAnimation(new int[0]));
        assertThrows(IllegalArgumentException.class,()->new MenuImageAnimation(new int[]{10,0}));
    }
}

package dev.animal.afmr.ui;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class MenuSurfaceTest {
    @Test void cutsPreserveCoverageWithoutPaintingUnderListsOrStackingAlpha(){
        var surfaces=new ArrayList<MenuSurface>();
        surfaces.add(new MenuSurface(0,0,1000,540,dev.animal.afmr.core.MenuColor.BACKGROUND,0x141B1B));
        MenuSurface.cutOut(surfaces,12,94,218,192);
        MenuSurface.cutOut(surfaces,12,306,218,432);
        MenuSurface.cutOut(surfaces,242,182,728,424);
        MenuSurface.cutOut(surfaces,-20,0,1020,64);
        int area=0;
        for(int i=0;i<surfaces.size();i++){
            var a=surfaces.get(i);assertTrue(a.x1()>a.x0()&&a.y1()>a.y0());
            area+=(a.x1()-a.x0())*(a.y1()-a.y0());
            for(int j=0;j<i;j++){
                var b=surfaces.get(j);
                assertFalse(a.x0()<b.x1()&&b.x0()<a.x1()&&a.y0()<b.y1()&&b.y0()<a.y1());
            }
        }
        assertEquals(1000*540-206*98-206*126-486*242-1000*64,area);
    }
}

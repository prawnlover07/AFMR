package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MenuAppearanceStoreTest {
    @TempDir Path directory;

    @Test void menuAndWidgetAppearanceRemainIndependentAcrossSnapshotsAndDisk() throws Exception {
        var library=Library.defaults();
        library.settings.menuBackgroundOpacity=0.35;library.settings.menuBlur=false;
        library.settings.widgetBackgroundOpacity=0.92;
        var snapshot=library.snapshot();
        library.settings.menuBackgroundOpacity=1;library.settings.menuBlur=true;
        var store=new LibraryStore(directory.resolve("library.json"));store.save(snapshot);
        var loaded=store.load().settings;
        assertEquals(0.35,loaded.menuBackgroundOpacity);assertFalse(loaded.menuBlur);
        assertEquals(0.92,loaded.widgetBackgroundOpacity);
    }

    @Test void opacityIsBoundedAndNonFiniteValuesUseTheDefault(){
        var library=Library.defaults();
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}){
            library.settings.menuBackgroundOpacity=invalid;library.normalize();
            assertEquals(0.80,library.settings.menuBackgroundOpacity);
        }
        library.settings.menuBackgroundOpacity=-1;library.normalize();assertEquals(0,library.settings.menuBackgroundOpacity);
        library.settings.menuBackgroundOpacity=2;library.normalize();assertEquals(1,library.settings.menuBackgroundOpacity);
    }
}

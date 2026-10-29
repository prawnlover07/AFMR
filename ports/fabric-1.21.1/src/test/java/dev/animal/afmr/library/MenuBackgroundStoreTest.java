package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.MenuImageLayout.Fit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MenuBackgroundStoreTest {
    @TempDir Path directory;
    @Test void imageSettingsSurviveOwnedSnapshotsAndDisk()throws Exception{
        var library=Library.defaults();var s=library.settings;
        s.menuImageEnabled=true;s.menuImageName="Wallpaper.png";s.menuImageOpacity=.45;s.menuImageFit=Fit.FIT;
        var snapshot=library.snapshot();s.menuImageEnabled=false;s.menuImageName="Other.jpg";s.menuImageOpacity=1;s.menuImageFit=Fit.STRETCH;
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(snapshot);var loaded=store.load().settings;
            assertTrue(loaded.menuImageEnabled);assertEquals("Wallpaper.png",loaded.menuImageName);
            assertEquals(.45,loaded.menuImageOpacity);assertEquals(Fit.FIT,loaded.menuImageFit);
        }
    }
    @Test void imagesStaySharedWhenWorldProfilesChange(){
        var library=Library.defaults();var s=library.settings;
        s.menuImageEnabled=true;s.menuImageName="shared.png";s.menuImageOpacity=.6;s.menuImageFit=Fit.STRETCH;
        String other=library.profiles.create(library,"Other",null);library.profiles.activate(library,other);
        library.profiles.activate(library,"default");
        assertTrue(s.menuImageEnabled);assertEquals("shared.png",s.menuImageName);assertEquals(.6,s.menuImageOpacity);assertEquals(Fit.STRETCH,s.menuImageFit);
    }
    @Test void malformedAppearanceUsesSafeDefaultsAndBounds(){
        var l=Library.defaults();assertFalse(l.settings.menuImageEnabled);assertEquals(Fit.FILL,l.settings.menuImageFit);
        l.settings.menuImageName=null;l.settings.menuImageFit=null;l.settings.menuImageOpacity=Double.NaN;l.normalize();
        assertEquals("",l.settings.menuImageName);assertEquals(Fit.FILL,l.settings.menuImageFit);assertEquals(1,l.settings.menuImageOpacity);
        l.settings.menuImageOpacity=-2;l.normalize();assertEquals(0,l.settings.menuImageOpacity);
        l.settings.menuImageOpacity=4;l.normalize();assertEquals(1,l.settings.menuImageOpacity);
    }
}

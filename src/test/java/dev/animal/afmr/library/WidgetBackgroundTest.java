package dev.animal.afmr.library;

import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.MenuImageLayout.Fit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class WidgetBackgroundTest {
    @TempDir Path directory;

    @Test void widgetAndMenuCopiesNeverReplaceOrDeleteEachOther()throws Exception{
        Path menu=directory.resolve("menu-background.png"),widget=directory.resolve("widget-background.png");
        Path source=directory.resolve("source.gif");byte[] gif=BackgroundFormatsTest.fixture("disposal.gif");Files.write(source,gif);
        MenuBackgroundImage.install(source,menu);MenuBackgroundImage.install(source,widget);
        assertArrayEquals(gif,Files.readAllBytes(directory.resolve("menu-background.gif")));
        assertArrayEquals(gif,Files.readAllBytes(directory.resolve("widget-background.gif")));
        Path still=directory.resolve("still.png");Files.write(still,BackgroundFormatsTest.fixture("disposal-0.png"));
        MenuBackgroundImage.install(still,widget);
        assertTrue(Files.exists(widget));assertFalse(Files.exists(directory.resolve("widget-background.gif")));
        assertArrayEquals(gif,Files.readAllBytes(MenuBackgroundImage.installed(menu)));
        MenuBackgroundImage.install(still,menu);
        assertFalse(Files.exists(directory.resolve("menu-background.gif")));assertTrue(Files.exists(widget));
        assertArrayEquals(gif,Files.readAllBytes(source));
    }

    @Test void failedWidgetReplacementPreservesBothInstalledImages()throws Exception{
        Path menu=directory.resolve("menu-background.png"),widget=directory.resolve("widget-background.png");
        byte[] png=BackgroundFormatsTest.fixture("disposal-0.png");Files.write(menu,png);Files.write(widget,png);
        Path bad=directory.resolve("bad.png");Files.writeString(bad,"broken");
        assertThrows(java.io.IOException.class,()->MenuBackgroundImage.install(bad,widget));
        assertArrayEquals(png,Files.readAllBytes(menu));assertArrayEquals(png,Files.readAllBytes(widget));
    }

    @Test void widgetAppearanceSurvivesSnapshotsSaveLoadAndProfileChanges()throws Exception{
        var lib=Library.defaults();var s=lib.settings;
        s.widgetImageEnabled=true;s.widgetImageName="my-image.gif";s.widgetImageFit=Fit.STRETCH;s.widgetImageOpacity=0.37;
        s.menuImageName="menu.png";s.menuImageFit=Fit.FIT;s.menuImageOpacity=0.81;
        var snapshot=lib.snapshot();s.widgetImageName="changed.png";
        assertEquals("my-image.gif",snapshot.settings.widgetImageName);
        var profile=snapshot.profiles.create(snapshot,"Other",snapshot.profiles.activeId);snapshot.profiles.activate(snapshot,profile);
        var store=new LibraryStore(directory.resolve("library.json"));store.save(snapshot);var loaded=store.load().settings;
        assertTrue(loaded.widgetImageEnabled);assertEquals("my-image.gif",loaded.widgetImageName);
        assertEquals(Fit.STRETCH,loaded.widgetImageFit);assertEquals(0.37,loaded.widgetImageOpacity);
        assertEquals("menu.png",loaded.menuImageName);assertEquals(Fit.FIT,loaded.menuImageFit);assertEquals(0.81,loaded.menuImageOpacity);
    }

    @Test void invalidWidgetValuesNormalizeIndependently() {
        var lib=Library.defaults();var s=lib.settings;
        assertFalse(s.widgetImageEnabled);assertEquals(Fit.FILL,s.widgetImageFit);assertEquals(1,s.widgetImageOpacity);
        s.widgetImageOpacity=Double.NaN;s.widgetImageFit=null;s.widgetImageName=null;s.menuImageOpacity=0.4;
        lib.normalize();assertEquals(1,s.widgetImageOpacity);assertEquals(Fit.FILL,s.widgetImageFit);assertEquals("",s.widgetImageName);
        assertEquals(0.4,s.menuImageOpacity);
        s.widgetImageOpacity=-1;lib.normalize();assertEquals(0,s.widgetImageOpacity);
        s.widgetImageOpacity=2;lib.normalize();assertEquals(1,s.widgetImageOpacity);
    }
}

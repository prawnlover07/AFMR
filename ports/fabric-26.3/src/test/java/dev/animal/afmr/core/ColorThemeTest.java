package dev.animal.afmr.core;

import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ColorThemeTest {
    @TempDir Path temp;
    @Test void captureAndApplyOwnTheirColorsAndChangeOnlyThemeSettings(){
        var lib=Library.defaults();lib.settings.menuColors.put(MenuColor.ACCENT,0xABCDEF);lib.settings.menuBackgroundOpacity=.3;lib.settings.widgetBackgroundColor=0x123456;lib.settings.widgetBackgroundOpacity=.4;
        var theme=ColorTheme.capture("Warm",lib.settings);lib.settings.menuColors.put(MenuColor.ACCENT,0);
        assertEquals(0xABCDEF,theme.menuColors.get(MenuColor.ACCENT));lib.settings.menuFontId="test:font";lib.settings.menuImageEnabled=true;lib.settings.widgetWidth=350;lib.settings.silenceMinSeconds=77;lib.settings.menuBlur=false;
        theme.apply(lib.settings);assertTrue(theme.matches(lib.settings));assertEquals(.3,lib.settings.menuBackgroundOpacity);assertEquals(.4,lib.settings.widgetBackgroundOpacity);assertEquals(0x123456,lib.settings.widgetBackgroundColor);
        assertEquals("test:font",lib.settings.menuFontId);assertTrue(lib.settings.menuImageEnabled);assertEquals(350,lib.settings.widgetWidth);assertEquals(77,lib.settings.silenceMinSeconds);assertFalse(lib.settings.menuBlur);
        lib.settings.menuColors.put(MenuColor.ACCENT,0);assertEquals(0xABCDEF,theme.menuColors.get(MenuColor.ACCENT));
    }
    @Test void graphiteIsAnIndependentBuiltInSnapshot(){var a=ColorTheme.graphite();a.menuColors.put(MenuColor.ACCENT,0);var b=ColorTheme.graphite();assertEquals(0x90C6FF,b.menuColors.get(MenuColor.ACCENT));assertEquals(.8,b.menuOpacity);assertEquals(.92,b.widgetOpacity);assertEquals(0,b.widgetBackground);}
    @Test void snapshotAndDiskRoundTripRetainIndependentSavedThemes()throws Exception{
        var lib=Library.defaults();var theme=ColorTheme.capture("Custom",lib.settings);lib.settings.savedThemes.add(theme);var snapshot=lib.snapshot();theme.menuColors.put(MenuColor.TEXT,0);theme.name="Changed";
        try(var store=new LibraryStore(temp.resolve("library.json"))){store.save(snapshot);var loaded=store.load();assertEquals("Custom",loaded.settings.savedThemes.get(0).name);assertEquals(0xEBEEF3,loaded.settings.savedThemes.get(0).menuColors.get(MenuColor.TEXT));assertEquals(theme.id,loaded.settings.savedThemes.get(0).id);}
    }
    @Test void profilesShareSavedThemesAndAppearance(){var lib=Library.defaults();var theme=ColorTheme.capture("Shared",lib.settings);lib.settings.savedThemes.add(theme);String profile=lib.profiles.create(lib,"Other",lib.profiles.activeId);lib.profiles.activate(lib,profile);assertEquals(theme.id,lib.settings.savedThemes.get(0).id);assertEquals(0x90C6FF,lib.settings.menuColors.get(MenuColor.ACCENT));}
    @Test void namesAreBoundedAndCollisionsNeverReplaceExistingThemes(){
        var settings=new Library.Settings();var theme=ColorTheme.capture("Night",settings);settings.savedThemes.add(theme);
        assertEquals("night (2)",ColorTheme.uniqueName(settings," night ",""));assertEquals("Night",ColorTheme.uniqueName(settings,"Night",theme.id));assertEquals("Graphite (2)",ColorTheme.uniqueName(settings,"Graphite",""));
        for(String name:List.of(""," ","bad\nname","x".repeat(81)))assertThrows(IllegalArgumentException.class,()->ColorTheme.validName(name));
        assertEquals("夜",ColorTheme.validName(" 夜 "));
    }
    @Test void normalizationBoundsSavedThemesAndDropsInvalidOrDuplicateEntries(){
        var saved=new ArrayList<ColorTheme>();saved.add(null);saved.add(ColorTheme.graphite());var invalid=ColorTheme.graphite();invalid.id="invalid";invalid.menuOpacity=Double.NaN;saved.add(invalid);
        var first=ColorTheme.capture("First",new Library.Settings());saved.add(first);saved.add(first.copy());
        for(int i=0;i<200;i++)saved.add(ColorTheme.capture("Theme "+i,new Library.Settings()));
        var copy=ColorTheme.copySaved(saved);assertEquals(128,copy.size());assertEquals("First",copy.get(0).name);assertNotSame(first,copy.get(0));assertEquals(128,copy.stream().map(t->t.id).distinct().count());
    }
}

package dev.animal.afmr.core;

import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuColorTest {
    @TempDir Path directory;
    @Test void defaultsPreserveEveryOriginalShadeExactly(){
        var colors=MenuColor.defaults();
        int[] shades={0x141B1B,0x1D2727,0x253232,0x1B2023,0x202629,0x191D20,0x111619,0x35594E,0x3B4949,0x283236,0x6D7976,0x8DE0BE,0xE1ECE6,0xFFFFFF};
        for(var role:MenuColor.values())for(int shade:shades)assertEquals(0xFF000000|shade,role.argb(colors,shade,1));
    }
    @Test void changesOnlyAffectTheirColorFamilyAndKeepShadeOffsets(){
        var colors=MenuColor.defaults();colors.put(MenuColor.PANELS,0x402030);
        assertEquals(0xFF402030,MenuColor.PANELS.argb(colors,0x1D2727,1));
        assertEquals(0xFF482B3B,MenuColor.PANELS.argb(colors,0x253232,1));
        assertEquals(0xFF141B1B,MenuColor.BACKGROUND.argb(colors,0x141B1B,1));
        assertEquals(0xFFE1ECE6,MenuColor.TEXT.argb(colors,0xFFE1ECE6,1));
    }
    @Test void extremeCustomBasesClampChannelsWithoutWrapping(){
        var colors=MenuColor.defaults();colors.put(MenuColor.TEXT,0xFFFFFF);
        assertEquals(0xFFFFFFFF,MenuColor.TEXT.argb(colors,0xFFFFFFFF,1));colors.put(MenuColor.TEXT,0);
        assertEquals(0xFF000000,MenuColor.TEXT.argb(colors,0,1));
    }
    @Test void opacityIsIndependentFromRgbAndTextCanRemainFullyOpaque(){
        var colors=MenuColor.defaults();colors.put(MenuColor.BACKGROUND,0x203040);
        assertEquals(0x00203040,MenuColor.BACKGROUND.argb(colors,0x141B1B,0));
        assertEquals(0x80203040,MenuColor.BACKGROUND.argb(colors,0x141B1B,0.5));
        assertEquals(0xFF203040,MenuColor.BACKGROUND.argb(colors,0x141B1B,1));
        assertEquals(0xFFE1ECE6,MenuColor.TEXT.argb(colors,0xFFE1ECE6,1));
    }
    @Test void rgbInputAcceptsSixAsciiHexDigitsAndRejectsIncompleteOrAlphaValues(){
        assertEquals(0xABCDEF,MenuColor.parse(" #aBcDeF "));assertEquals(0,MenuColor.parse("000000"));assertEquals(0xFFFFFF,MenuColor.parse("FFFFFF"));
        for(String text:List.of("","#123","#12345678","GGGGGG","0x123456","12345z","１２３４５６"))assertThrows(IllegalArgumentException.class,()->MenuColor.parse(text));
    }
    @Test void missingInvalidOrNullColorValuesNormalizeToIndependentDefaults(){
        var input=new HashMap<MenuColor,Integer>();input.put(MenuColor.TEXT,0x203040);input.put(MenuColor.HOVER,-1);input.put(MenuColor.ACCENT,0x1000000);input.put(MenuColor.BACKGROUND,null);input.put(null,10);
        var colors=MenuColor.normalize(input);assertEquals(10,colors.size());assertEquals(0x203040,colors.get(MenuColor.TEXT));
        assertEquals(MenuColor.HOVER.defaultRgb,colors.get(MenuColor.HOVER));assertEquals(MenuColor.ACCENT.defaultRgb,colors.get(MenuColor.ACCENT));
        assertNull(input.get(MenuColor.BACKGROUND));assertFalse(colors.containsKey(null));assertEquals(MenuColor.defaults(),MenuColor.normalize(null));
    }
    @Test void paletteHasOwnedSnapshotsAndSurvivesDiskWithoutChangingOtherAppearance() throws Exception {
        var lib=Library.defaults();lib.settings.menuColors.put(MenuColor.ACCENT,0xD090E0);lib.settings.menuColors.put(MenuColor.BACKGROUND,0x201028);
        lib.settings.menuBackgroundOpacity=0.35;lib.settings.menuImageOpacity=0.4;lib.settings.widgetBackgroundColor=0x123456;
        var copy=lib.snapshot();lib.settings.menuColors.put(MenuColor.ACCENT,0);lib.settings.menuColors.put(MenuColor.BACKGROUND,0);
        try(var store=new LibraryStore(directory.resolve("library.json"))){
            store.save(copy);var settings=store.load().settings;assertEquals(0xD090E0,settings.menuColors.get(MenuColor.ACCENT));assertEquals(0x201028,settings.menuColors.get(MenuColor.BACKGROUND));
            assertEquals(0.35,settings.menuBackgroundOpacity);assertEquals(0.4,settings.menuImageOpacity);assertEquals(0x123456,settings.widgetBackgroundColor);
        }
    }
    @Test void resetPaletteDoesNotResetOpacityImageOrWidgetColors(){
        var lib=Library.defaults();lib.settings.menuColors.clear();lib.settings.menuBackgroundOpacity=0.25;lib.settings.menuImageEnabled=true;lib.settings.widgetBackgroundColor=0x123456;
        lib.settings.menuColors=MenuColor.defaults();assertEquals(10,lib.settings.menuColors.size());assertEquals(0.25,lib.settings.menuBackgroundOpacity);
        assertTrue(lib.settings.menuImageEnabled);assertEquals(0x123456,lib.settings.widgetBackgroundColor);
    }
}

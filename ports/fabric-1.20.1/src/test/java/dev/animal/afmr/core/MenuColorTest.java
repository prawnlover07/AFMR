package dev.animal.afmr.core;

import dev.animal.afmr.library.LibraryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuColorTest {
    @TempDir Path directory;
    @Test void defaultsUseTheChosenGraphitePaletteInActualRendering(){
        int[] expected={0x17191D,0x202329,0x2C3038,0x3B424D,0x304963,0x90C6FF,0xEBEEF3,0xABB5C2,0x3B424C,0x66778B};
        var settings=Library.defaults().settings;int index=0;
        for(var role:MenuColor.values()){
            int rgb=expected[index++];assertEquals(rgb,settings.menuColors.get(role));
            assertEquals(0xFF000000|rgb,role.argb(settings.menuColors,rgb,1));
            assertEquals(0xCC000000|rgb,role.argb(settings.menuColors,rgb,0.8));
        }
    }
    @Test void changesOnlyAffectTheirColorFamilyAndKeepShadeOffsets(){
        var colors=MenuColor.defaults();colors.put(MenuColor.PANELS,0x402030);
        assertEquals(0xFF402030,MenuColor.PANELS.argb(colors,0x202329,1));
        assertEquals(0xFF482B3B,MenuColor.PANELS.argb(colors,0x282E34,1));
        assertEquals(0xFF17191D,MenuColor.BACKGROUND.argb(colors,0x17191D,1));
        assertEquals(0xFFEBEEF3,MenuColor.TEXT.argb(colors,0xFFEBEEF3,1));
    }
    @Test void extremeCustomBasesClampChannelsWithoutWrapping(){
        var colors=MenuColor.defaults();colors.put(MenuColor.TEXT,0xFFFFFF);
        assertEquals(0xFFFFFFFF,MenuColor.TEXT.argb(colors,0xFFFFFFFF,1));colors.put(MenuColor.TEXT,0);
        assertEquals(0xFF000000,MenuColor.TEXT.argb(colors,0,1));
    }
    @Test void opacityIsIndependentFromRgbAndTextCanRemainFullyOpaque(){
        var colors=MenuColor.defaults();colors.put(MenuColor.BACKGROUND,0x203040);
        assertEquals(0x00203040,MenuColor.BACKGROUND.argb(colors,0x17191D,0));
        assertEquals(0x80203040,MenuColor.BACKGROUND.argb(colors,0x17191D,0.5));
        assertEquals(0xFF203040,MenuColor.BACKGROUND.argb(colors,0x17191D,1));
        assertEquals(0xFFEBEEF3,MenuColor.TEXT.argb(colors,0xFFEBEEF3,1));
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

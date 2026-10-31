package dev.animal.afmr.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContrastTest {
    @Test void wcagReferenceValues(){
        assertEquals(0,Contrast.luminance(0),1e-12);assertEquals(1,Contrast.luminance(0xFFFFFF),1e-12);
        assertEquals(.2126,Contrast.luminance(0xFF0000),1e-12);assertEquals(.7152,Contrast.luminance(0x00FF00),1e-12);
        assertEquals(.0722,Contrast.luminance(0x0000FF),1e-12);assertEquals(.215861,Contrast.luminance(0x808080),1e-6);
        assertEquals(21,Contrast.ratio(0,0xFFFFFF),1e-12);assertEquals(1,Contrast.ratio(0x123456,0x123456),1e-12);
        assertEquals(Contrast.ratio(0x123456,0xABCDEF),Contrast.ratio(0xABCDEF,0x123456),1e-12);
        assertEquals(Contrast.luminance(0x808080),Contrast.luminance(0xFF808080),1e-12);
    }
    @Test void translucentColorsUseSrgbChannelsAndAnExplicitReference(){
        assertEquals(0x123456,Contrast.composite(0xFFFFFF,0,0x123456));assertEquals(0x123456,Contrast.composite(0x123456,1,0));
        assertEquals(0x808080,Contrast.composite(0xFFFFFF,.5,0));assertEquals(0x804020,Contrast.composite(0xFF8040,.5,0));
        assertEquals(0x123456,Contrast.composite(0,-1,0x123456));assertEquals(0xFFFFFF,Contrast.composite(0xFFFFFF,2,0));
        assertEquals(0x123456,Contrast.composite(0,Double.NaN,0x123456));
    }
    @Test void widgetSelectsTheMoreReadableTextForDarkAndLightFills(){
        assertEquals(Contrast.WIDGET_LIGHT,Contrast.widgetText(0));assertEquals(Contrast.WIDGET_DARK,Contrast.widgetText(0xFFFFFF));
        for(int channel=0;channel<256;channel++){
            int background=channel*0x010101,chosen=Contrast.widgetText(background),other=chosen==Contrast.WIDGET_LIGHT?Contrast.WIDGET_DARK:Contrast.WIDGET_LIGHT;
            assertTrue(Contrast.ratio(chosen,background)>=Contrast.ratio(other,background));
        }
    }
    @Test void themeWarningsCoverEveryTextAndSurfaceRole(){
        assertFalse(ColorTheme.graphite().lowContrast());
        for(var text:new MenuColor[]{MenuColor.TEXT,MenuColor.MUTED_TEXT})for(var surface:new MenuColor[]{MenuColor.BACKGROUND,MenuColor.PANELS,MenuColor.CONTROLS}){
            var theme=ColorTheme.graphite();theme.menuColors.put(text,surface.rgb(theme.menuColors));assertTrue(theme.lowContrast());
        }
        var theme=ColorTheme.graphite();theme.menuColors.put(MenuColor.PANELS,0xFFFFFF);assertTrue(theme.lowContrast());
    }
    @Test void widgetWarningIncludesPoorMidtoneFillsAndTranslucentReference(){
        var theme=ColorTheme.graphite();theme.widgetBackground=0xFFFFFF;assertFalse(theme.lowContrast());
        theme.widgetOpacity=.25;assertTrue(theme.lowContrast());theme.widgetOpacity=.92;
        theme.widgetBackground=0x757575;assertTrue(theme.lowContrast());
        theme.widgetBackground=0;theme.widgetOpacity=0;assertTrue(theme.lowContrast());
        theme.widgetOpacity=.92;theme.menuOpacity=0;assertTrue(theme.lowContrast());
    }
    @Test void disabledShadePassesOnControlsAndRemainsDimmerThanSecondaryText(){
        assertEquals(4.852,Contrast.ratio(MenuColor.DISABLED_TEXT,MenuColor.CONTROLS.defaultRgb),.001);
        assertTrue(Contrast.luminance(MenuColor.DISABLED_TEXT)<Contrast.luminance(MenuColor.MUTED_TEXT.defaultRgb));
        var colors=MenuColor.defaults();assertEquals(MenuColor.DISABLED_TEXT,MenuColor.MUTED_TEXT.argb(colors,MenuColor.DISABLED_TEXT,1));
        for(int background:new int[]{0x1E1E25,0x23242B,0x2C303E,0x304963})assertTrue(Contrast.ratio(0xADB7C4,background)>=4.5);
    }
}

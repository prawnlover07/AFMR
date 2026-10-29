package dev.animal.afmr.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThemePreviewTest {
    @Test void previewNeverChangesSettingsOrSavedSnapshots(){
        var lib=Library.defaults();var opening=ColorTheme.capture("Before",lib.settings);var preview=new ThemePreview(lib.settings);
        preview.draft().menuColors.put(MenuColor.ACCENT,0x123456);preview.draft().menuOpacity=.1;preview.draft().widgetBackground=0xABCDEF;preview.draft().widgetOpacity=.2;
        assertTrue(opening.matches(lib.settings));assertTrue(opening.matches(lib.snapshot().settings));
    }
    @Test void choosingAPresetOwnsItsDraftAndDoesNotMutateThePreset(){
        var settings=new Library.Settings();var preset=ColorTheme.capture("Saved",settings);var preview=new ThemePreview(settings);preview.choose(preset);preview.draft().menuColors.put(MenuColor.TEXT,0);
        assertEquals(0xEBEEF3,preset.menuColors.get(MenuColor.TEXT));assertEquals(0xEBEEF3,settings.menuColors.get(MenuColor.TEXT));
    }
    @Test void cancelingAChildEditorRestoresOnlyItsOpeningDraft(){
        var settings=new Library.Settings();var preview=new ThemePreview(settings);preview.draft().widgetBackground=0x112233;var opening=preview.draft().copy();preview.draft().widgetBackground=0;preview.choose(opening);
        assertEquals(0x112233,preview.draft().widgetBackground);assertEquals(0,settings.widgetBackgroundColor);
    }
    @Test void applyCommitsAppearanceAndTheSelectedCustomThemeExactlyOnce(){
        var settings=new Library.Settings();var saved=ColorTheme.capture("Named",settings);settings.savedThemes.add(saved);var preview=new ThemePreview(settings);preview.draft().menuColors.put(MenuColor.ACCENT,0x123456);preview.draft().menuOpacity=.3;
        settings.silenceMinSeconds=999;settings.menuFontId="pack:font";preview.commit(settings,saved.id);
        assertTrue(preview.draft().matches(settings));assertTrue(settings.savedThemes.get(0).matches(settings));assertEquals(saved.id,settings.savedThemes.get(0).id);assertEquals("Named",settings.savedThemes.get(0).name);assertEquals(999,settings.silenceMinSeconds);assertEquals("pack:font",settings.menuFontId);
        preview.draft().menuColors.put(MenuColor.ACCENT,0);assertEquals(0x123456,settings.menuColors.get(MenuColor.ACCENT));assertEquals(0x123456,settings.savedThemes.get(0).menuColors.get(MenuColor.ACCENT));
    }
    @Test void builtinDefinitionIsUnchangedWhenItsCustomizedDraftIsApplied(){var settings=new Library.Settings();var preview=new ThemePreview(settings);preview.choose(ColorTheme.graphite());preview.draft().widgetBackground=0x123456;preview.commit(settings,ColorTheme.GRAPHITE);assertEquals(0x123456,settings.widgetBackgroundColor);assertEquals(0,ColorTheme.graphite().widgetBackground);assertTrue(settings.savedThemes.isEmpty());}
    @Test void deletedTargetRejectsApplyBeforeAnyAppearanceChanges(){var settings=new Library.Settings();var original=ColorTheme.capture("Before",settings);var preview=new ThemePreview(settings);preview.draft().menuOpacity=.01;assertThrows(IllegalArgumentException.class,()->preview.commit(settings,"deleted"));assertTrue(original.matches(settings));}
    @Test void exportedOrSavedDraftSnapshotsRemainIndependent(){var settings=new Library.Settings();var preview=new ThemePreview(settings);preview.draft().widgetOpacity=.25;var copy=preview.snapshot("Copy");preview.draft().widgetOpacity=.75;assertEquals(.25,copy.widgetOpacity);assertEquals(.92,settings.widgetBackgroundOpacity);assertNotEquals(copy.id,preview.draft().id);}
}

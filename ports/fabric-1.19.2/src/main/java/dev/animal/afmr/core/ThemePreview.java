package dev.animal.afmr.core;

import java.util.UUID;

/** A draft owns its colors and never changes persisted settings until an explicit commit. */
public final class ThemePreview {
    private ColorTheme draft;
    public ThemePreview(Library.Settings settings){draft=ColorTheme.capture("Preview",settings);}
    public ColorTheme draft(){return draft;}
    public void choose(ColorTheme theme){draft=theme.copy();}
    public ColorTheme snapshot(String name){var copy=draft.copy();copy.id=UUID.randomUUID().toString();copy.name=ColorTheme.validName(name);return copy;}
    public void commit(Library.Settings settings,String id){
        ColorTheme saved=id.isBlank()||ColorTheme.GRAPHITE.equals(id)?null:ColorTheme.find(settings,id);
        if(saved==null&&!id.isBlank()&&!ColorTheme.GRAPHITE.equals(id))throw new IllegalArgumentException("Theme no longer exists.");
        draft.apply(settings);
        if(saved!=null){var updated=snapshot(saved.name);updated.id=id;settings.savedThemes.set(settings.savedThemes.indexOf(saved),updated);}
    }
}

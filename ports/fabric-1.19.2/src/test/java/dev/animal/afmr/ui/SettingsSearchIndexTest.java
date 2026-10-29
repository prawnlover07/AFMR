package dev.animal.afmr.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingsSearchIndexTest {
    @Test void familiarTermsFindDirectDestinations(){
        assertTrue(SettingsSearchIndex.search("fade in").stream().anyMatch(e->e.destination()==SettingsSearchIndex.Destination.TIMING&&e.control().equals("Fade in (sec)")));
        assertTrue(SettingsSearchIndex.search("widget font").stream().anyMatch(e->e.destination()==SettingsSearchIndex.Destination.WIDGET_FONT));
        assertTrue(SettingsSearchIndex.search("home").stream().anyMatch(e->e.control().equals("Set playback area here")));
        assertTrue(SettingsSearchIndex.search("proximity volume").stream().anyMatch(e->e.control().equals("NEARBY_PLAYER")));
    }
    @Test void searchIsCaseInsensitiveAndRequiresAllWords(){
        assertEquals(SettingsSearchIndex.search("fade in"),SettingsSearchIndex.search("  FADE   IN  "));
        assertTrue(SettingsSearchIndex.search("unrelated nonexistent setting").isEmpty());
    }
    @Test void everyConditionHasOneSearchDestination(){
        var all=SettingsSearchIndex.search("");
        for(var option:ConditionOption.values())assertEquals(1,all.stream().filter(e->e.destination()==SettingsSearchIndex.Destination.RULE&&e.control().equals(option.name())).count());
        assertEquals(all.size(),all.stream().map(e->e.section()+"/"+e.label()).distinct().count());
    }
}

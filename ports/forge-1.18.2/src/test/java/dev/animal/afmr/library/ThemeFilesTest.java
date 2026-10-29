package dev.animal.afmr.library;

import com.google.gson.*;
import dev.animal.afmr.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ThemeFilesTest {
    @TempDir Path temp;
    private Path file(){return temp.resolve("theme.afmrt");}
    private JsonObject example()throws Exception{ThemeFiles.write(file(),ColorTheme.graphite());return JsonParser.parseString(Files.readString(file())).getAsJsonObject();}
    private void invalid(JsonObject value)throws Exception{Files.writeString(file(),value.toString());assertThrows(IOException.class,()->ThemeFiles.read(file()));}
    @Test void roundTripContainsOnlySharedAppearanceAndNeverAnInstallationIdentity()throws Exception{
        var settings=new Library.Settings();settings.menuColors.put(MenuColor.TEXT,0x123456);settings.menuBackgroundOpacity=.11;settings.widgetBackgroundColor=0xABCDEF;settings.widgetBackgroundOpacity=.22;settings.menuFontId="private:font";
        var original=ColorTheme.capture("Custom 夜",settings);ThemeFiles.write(file(),original);var loaded=ThemeFiles.read(file());assertTrue(loaded.matches(settings));assertEquals(original.name,loaded.name);assertNotEquals(original.id,loaded.id);
        var root=JsonParser.parseString(Files.readString(file())).getAsJsonObject();assertEquals(Set.of("name","menuColors","menuOpacity","widgetBackground","widgetOpacity"),root.keySet());assertEquals(10,root.getAsJsonObject("menuColors").size());
    }
    @Test void rejectsMissingAndUnknownFieldsWithoutChangingTheDestinationSettings()throws Exception{
        var root=example();root.remove("widgetOpacity");invalid(root);root=example();root.addProperty("musicFolder","private");invalid(root);root=example();root.getAsJsonObject("menuColors").remove("TEXT");invalid(root);root=example();root.getAsJsonObject("menuColors").addProperty("UNKNOWN","#000000");invalid(root);
    }
    @Test void rejectsDuplicateFieldsAndDuplicateRoles()throws Exception{
        example();String source=Files.readString(file());Files.writeString(file(),source.replace("\"name\":", "\"name\":\"Duplicate\",\"name\":"));assertThrows(IOException.class,()->ThemeFiles.read(file()));
        Files.writeString(file(),source.replace("\"TEXT\":", "\"TEXT\":\"#000000\",\"TEXT\":"));assertThrows(IOException.class,()->ThemeFiles.read(file()));
    }
    @Test void refusesInvalidNumbersTypesColorsAndNames()throws Exception{
        for(double value:new double[]{-1,1.01}){var root=example();root.addProperty("menuOpacity",value);invalid(root);}
        var root=example();root.addProperty("widgetOpacity","0.5");invalid(root);root=example();root.addProperty("widgetBackground","#12345678");invalid(root);root=example();root.addProperty("name","bad\nname");invalid(root);root=example();root.getAsJsonObject("menuColors").addProperty("TEXT",123);invalid(root);
    }
    @Test void rejectsTruncatedTrailingLenientAndInvalidUtf8Content()throws Exception{
        example();String source=Files.readString(file());
        for(String text:List.of(source.substring(0,source.length()/2),source+" {}",source.replace("\"name\"","name"),"[]")){Files.writeString(file(),text);assertThrows(IOException.class,()->ThemeFiles.read(file()));}
        Files.write(file(),new byte[]{(byte)0xC3,(byte)0x28});assertThrows(IOException.class,()->ThemeFiles.read(file()));
    }
    @Test void boundsFileReadsAndRequiresThemeExtension()throws Exception{
        try(var channel=Files.newByteChannel(file(),StandardOpenOption.CREATE,StandardOpenOption.WRITE)){channel.position(ThemeFiles.MAX_BYTES);channel.write(ByteBuffer.wrap(new byte[]{0}));}assertThrows(IOException.class,()->ThemeFiles.read(file()));
        assertThrows(IOException.class,()->ThemeFiles.write(temp.resolve("wrong.json"),ColorTheme.graphite()));
    }
    @Test void invalidExportsLeaveAnExistingFileUntouched()throws Exception{
        Files.writeString(file(),"original");var theme=ColorTheme.graphite();theme.widgetOpacity=Double.NaN;assertThrows(IOException.class,()->ThemeFiles.write(file(),theme));assertEquals("original",Files.readString(file()));
        try(var files=Files.list(temp)){assertEquals(1,files.count());}
    }
    @Test void fullBackupsIncludeTheSavedThemeCollection()throws Exception{
        var library=Library.defaults();library.settings.menuBackgroundOpacity=.37;library.settings.widgetBackgroundColor=0x123456;var theme=ColorTheme.capture("Backup theme",library.settings);library.settings.savedThemes.add(theme);
        var keys=new LinkedHashMap<String,ConfigBackup.Key>();for(String name:List.of("open","skip","next","pause","inspect","overrides","automatic"))keys.put("key.afmr."+name,new ConfigBackup.Key("key.keyboard.m","NONE"));for(int i=1;i<=8;i++)keys.put("key.afmr.override_"+i,new ConfigBackup.Key("key.keyboard.unknown","NONE"));
        Path archive=temp.resolve("backup.afmrb");ConfigBackup.write(archive,ConfigBackup.capture(library.snapshot(),List.of(),keys,temp));var restored=ConfigBackup.read(archive).library().settings.savedThemes.get(0);assertEquals(theme.id,restored.id);assertEquals(theme.name,restored.name);assertTrue(restored.matches(library.settings));
    }
}

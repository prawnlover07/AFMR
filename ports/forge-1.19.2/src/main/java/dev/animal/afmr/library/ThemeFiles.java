package dev.animal.afmr.library;

import com.google.gson.*;
import com.google.gson.stream.*;
import dev.animal.afmr.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Small, strict appearance-only JSON files shared by both maintained targets. */
public final class ThemeFiles {
    public static final int MAX_BYTES=64*1024;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private ThemeFiles(){}
    public static boolean supported(Path path){return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".afmrt");}
    public static ColorTheme read(Path path)throws IOException{
        if(!supported(path))throw new IOException("Choose an AFMR theme (.afmrt).");
        if(Files.size(path)>MAX_BYTES)throw new IOException("Theme exceeds 64 KiB.");
        byte[] bytes;try(var input=Files.newInputStream(path)){bytes=input.readNBytes(MAX_BYTES+1);}
        if(bytes.length>MAX_BYTES)throw new IOException("Theme exceeds 64 KiB.");
        try(var reader=new JsonReader(new StringReader(StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()))){
            reader.setLenient(false);var theme=new ColorTheme();theme.id=UUID.randomUUID().toString();var fields=new HashSet<String>();
            reader.beginObject();
            while(reader.hasNext()){
                String key=reader.nextName();if(!fields.add(key))throw new IOException("Duplicate theme field: "+key);
                switch(key){
                    case "name"->theme.name=ColorTheme.validName(string(reader));
                    case "menuColors"->{
                        var colors=new EnumMap<MenuColor,Integer>(MenuColor.class);reader.beginObject();
                        while(reader.hasNext()){
                            MenuColor role=MenuColor.valueOf(reader.nextName());if(colors.containsKey(role))throw new IOException("Duplicate theme color: "+role);
                            colors.put(role,MenuColor.parse(string(reader)));
                        }reader.endObject();if(colors.size()!=MenuColor.values().length)throw new IOException("Theme must include all ten menu colors.");theme.menuColors=colors;
                    }
                    case "menuOpacity"->theme.menuOpacity=opacity(reader);
                    case "widgetBackground"->theme.widgetBackground=MenuColor.parse(string(reader));
                    case "widgetOpacity"->theme.widgetOpacity=opacity(reader);
                    default->throw new IOException("Unknown theme field: "+key);
                }
            }reader.endObject();
            if(fields.size()!=5||reader.peek()!=JsonToken.END_DOCUMENT)throw new IOException("Incomplete theme or trailing content.");return theme;
        }catch(IllegalArgumentException|IllegalStateException ex){throw new IOException("Invalid theme: "+ex.getMessage(),ex);}
    }
    private static String string(JsonReader reader)throws IOException{if(reader.peek()!=JsonToken.STRING)throw new IOException("Expected text in theme.");return reader.nextString();}
    private static double opacity(JsonReader reader)throws IOException{
        if(reader.peek()!=JsonToken.NUMBER)throw new IOException("Expected numeric opacity.");double value=reader.nextDouble();
        if(!ColorTheme.validOpacity(value))throw new IOException("Opacity must be between 0 and 1.");return value;
    }
    public static void write(Path path,ColorTheme theme)throws IOException{
        if(!supported(path))throw new IOException("Choose a filename ending in .afmrt.");
        var root=new JsonObject();root.addProperty("name",ColorTheme.validName(theme.name));var colors=new JsonObject();
        for(var role:MenuColor.values()){
            Integer rgb=theme.menuColors.get(role);if(rgb==null||rgb<0||rgb>0xFFFFFF)throw new IOException("Theme must include all ten menu colors.");
            colors.addProperty(role.name(),String.format(Locale.ROOT,"#%06X",rgb));
        }
        if(!ColorTheme.validOpacity(theme.menuOpacity)||!ColorTheme.validOpacity(theme.widgetOpacity)||theme.widgetBackground<0||theme.widgetBackground>0xFFFFFF)throw new IOException("Invalid theme appearance values.");
        root.add("menuColors",colors);root.addProperty("menuOpacity",theme.menuOpacity);root.addProperty("widgetBackground",String.format(Locale.ROOT,"#%06X",theme.widgetBackground));root.addProperty("widgetOpacity",theme.widgetOpacity);
        Path target=path.toAbsolutePath().normalize(),parent=target.getParent();Files.createDirectories(parent);Path temp=Files.createTempFile(parent,"afmr-theme-",".tmp");
        try{Files.writeString(temp,JSON.toJson(root)+"\n",StandardCharsets.UTF_8);try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ex){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temp);}
    }
}

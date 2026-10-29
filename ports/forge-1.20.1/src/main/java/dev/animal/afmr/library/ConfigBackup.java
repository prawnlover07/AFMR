package dev.animal.afmr.library;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.ListeningHistory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** One bounded archive; only the named AFMR files can ever be installed. */
public final class ConfigBackup {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final int MIB=1024*1024;
    private static final Map<String,Integer> LIMITS=Map.of("library.json",96*MIB,"history.json",2*MIB,
        "keys.json",64*1024,"menu-background.png",MenuBackgroundImage.MAX_BYTES,"menu-background.gif",MenuBackgroundImage.MAX_BYTES,
        "widget-background.png",MenuBackgroundImage.MAX_BYTES,"widget-background.gif",MenuBackgroundImage.MAX_BYTES,"menu-font.ttf",FontFiles.MAX_BYTES,"widget-font.ttf",FontFiles.MAX_BYTES);
    private static final List<String> INSTALLED=List.of("library.json","history.json","restored-keys.json","menu-background.png","menu-background.gif","widget-background.png","widget-background.gif","menu-font.ttf","widget-font.ttf");
    public static final String PENDING="pending-restore.afmrb";
    public record Key(String code,String modifier){}
    public record Data(Map<String,byte[]> files,Library library,Map<String,Key> keys,int historyCount){}
    private ConfigBackup(){}
    public static boolean supported(Path path){return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".afmrb");}

    /** The caller transfers ownership of its library/history/key snapshots to this operation. */
    public static Data capture(Library library,List<ListeningHistory.Entry> history,Map<String,Key> keys,Path directory)throws IOException{
        var files=new LinkedHashMap<String,byte[]>();
        files.put("library.json",bytes(library));files.put("history.json",bytes(history));files.put("keys.json",bytes(keys));
        for(String stem:List.of("menu-background","widget-background")){
            Path installed=MenuBackgroundImage.installed(directory.resolve(stem+".png"));
            if(Files.exists(installed)){
                var image=MenuBackgroundImage.read(installed);
                files.put(stem+(image.animated()?".gif":".png"),image.stored());
            }
        }
        for(String name:List.of("menu-font.ttf","widget-font.ttf"))if(Files.isRegularFile(directory.resolve(name))){
            files.put(name,FontFiles.read(directory.resolve(name)).bytes());
        }
        return validate(files);
    }
    private static byte[] bytes(Object value){return GSON.toJson(value).getBytes(StandardCharsets.UTF_8);}
    public static void write(Path destination,Data data)throws IOException{
        if(!supported(destination))throw new IOException("Choose a filename ending in .afmrb.");
        Path absolute=destination.toAbsolutePath();Files.createDirectories(absolute.getParent());
        Path temporary=Files.createTempFile(absolute.getParent(),"afmr-backup-",".tmp");
        try{
            try(var zip=new ZipOutputStream(Files.newOutputStream(temporary))){
                for(var entry:data.files.entrySet()){
                    Integer limit=LIMITS.get(entry.getKey());
                    if(limit==null||entry.getValue().length>limit)throw new IOException("Backup entry exceeds its limit.");
                    zip.putNextEntry(new ZipEntry(entry.getKey()));zip.write(entry.getValue());zip.closeEntry();
                }
            }
            move(temporary,absolute);
        }finally{Files.deleteIfExists(temporary);}
    }
    public static Data read(Path source)throws IOException{
        if(!supported(source))throw new IOException("Choose an AFMR backup (.afmrb).");
        if(Files.size(source)>128L*MIB)throw new IOException("Backup exceeds 128 MiB.");
        var files=new LinkedHashMap<String,byte[]>();
        try(var zip=new ZipInputStream(Files.newInputStream(source))){
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){
                Integer limit=LIMITS.get(entry.getName());
                if(entry.isDirectory()||limit==null||files.containsKey(entry.getName()))throw new IOException("Unexpected or duplicate backup entry.");
                byte[] bytes=zip.readNBytes(limit+1);
                if(bytes.length>limit)throw new IOException(entry.getName()+" exceeds its size limit.");
                files.put(entry.getName(),bytes);zip.closeEntry();
            }
        }
        return validate(files);
    }
    private static JsonElement json(byte[] bytes)throws IOException{
        if(bytes==null)throw new IOException("Incomplete AFMR backup.");
        // Bound nesting before Gson builds its tree or recursively reads conditions.
        try(var reader=new JsonReader(new StringReader(new String(bytes,StandardCharsets.UTF_8)))){
            int depth=0;
            while(reader.peek()!=JsonToken.END_DOCUMENT){
                switch(reader.peek()){
                    case BEGIN_ARRAY->{reader.beginArray();depth++;}
                    case BEGIN_OBJECT->{reader.beginObject();depth++;}
                    case END_ARRAY->{reader.endArray();depth--;}
                    case END_OBJECT->{reader.endObject();depth--;}
                    case NAME->reader.nextName();
                    default->reader.skipValue();
                }
                if(depth>48)throw new IOException("Backup nesting exceeds its limit.");
            }
        }
        return JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8));
    }
    private static Data validate(Map<String,byte[]> files)throws IOException{
        try{
            var tree=json(files.get("library.json")).getAsJsonObject();
            for(String name:List.of("schemaVersion","settings","tracks","folders","playlists","profiles","legacyPlaylists"))
                if(!tree.has(name)||tree.get(name).isJsonNull())throw new IOException("Incomplete library in backup.");
            if(tree.get("schemaVersion").getAsInt()!=2)throw new IOException("Unsupported library in backup.");
            Library library=GSON.fromJson(tree,Library.class);
            var ids=new HashSet<String>();
            for(var t:library.tracks)if(t==null||t.id==null||t.id.isBlank()||!ids.add(t.id)||t.path==null||t.title==null)throw new IOException("Invalid track in backup.");
            for(var f:library.folders)if(f==null||f.path==null||f.playlistId==null)throw new IOException("Invalid music folder in backup.");
            library.normalize();
            validatePlaylists(library.playlists);validatePlaylists(library.legacyPlaylists);
            for(var profile:library.profiles.inactive)validatePlaylists(profile.playlists);
            var history=GSON.fromJson(json(files.get("history.json")),ListeningHistory.Entry[].class);
            if(history==null||history.length>ListeningHistory.LIMIT||Arrays.stream(history).anyMatch(e->!ListeningHistory.valid(e)))throw new IOException("Invalid history in backup.");
            var keys=new LinkedHashMap<String,Key>();
            for(var e:json(files.get("keys.json")).getAsJsonObject().entrySet()){
                Key key=GSON.fromJson(e.getValue(),Key.class);
                if(!e.getKey().matches("key\\.afmr\\.(open|skip|next|pause|show_now_playing|inspect|overrides|automatic|override_[1-8])")
                    ||key==null||key.code==null||key.code.length()>128||!key.code.matches("(key\\.(keyboard|mouse)\\.[a-z0-9._-]+|scancode\\.[0-9]+)")
                    ||!Set.of("NONE","SHIFT","CONTROL","CONTROL_OR_COMMAND","ALT").contains(key.modifier))throw new IOException("Invalid AFMR keybinding in backup.");
                keys.put(e.getKey(),key);
            }
            // The replay shortcut is optional in existing 1.1 backups; all original bindings remain required.
            if(keys.size()-(keys.containsKey("key.afmr.show_now_playing")?1:0)!=15)throw new IOException("Incomplete AFMR keybindings in backup.");
            for(String stem:List.of("menu-background","widget-background")){
                boolean still=files.containsKey(stem+".png"),animation=files.containsKey(stem+".gif");
                boolean enabled=stem.equals("menu-background")?library.settings.menuImageEnabled:library.settings.widgetImageEnabled;
                if(enabled&&!still&&!animation)throw new IOException("Backup is missing its enabled "+stem+" image.");
                if(still&&animation)throw new IOException("Backup contains conflicting "+stem+" images.");
                if(still||animation){
                    byte[] bytes=files.get(stem+(animation?".gif":".png"));
                    var image=MenuBackgroundImage.decode(bytes);
                    if(animation!=image.animated()||(!animation&&(bytes[0]!=(byte)0x89
                        ||java.nio.ByteBuffer.wrap(bytes).getInt(16)>MenuBackgroundImage.MAX_EDGE||java.nio.ByteBuffer.wrap(bytes).getInt(20)>MenuBackgroundImage.MAX_EDGE)))
                        throw new IOException("Invalid "+stem+" image in backup.");
                }
            }
            for(String target:List.of("menu","widget")){
                byte[] font=files.get(target+"-font.ttf");String selected=target.equals("menu")?library.settings.menuFontId:library.settings.widgetFontId;
                if(font==null&&selected.equals("afmr:custom_"+target))throw new IOException("Backup is missing its selected "+target+" font.");
                if(font!=null)FontFiles.validate(font);
            }
            return new Data(Collections.unmodifiableMap(new LinkedHashMap<>(files)),library,Map.copyOf(keys),history.length);
        }catch(RuntimeException e){throw new IOException("Invalid AFMR backup: "+e.getMessage(),e);}
    }
    private static void validatePlaylists(List<Library.Playlist> playlists)throws IOException{
        var ids=new HashSet<String>();
        for(var p:playlists){
            if(p.id.isBlank()||!ids.add(p.id)||p.tracks.entrySet().stream().anyMatch(e->e.getKey().isBlank()||e.getValue()==null))
                throw new IOException("Invalid playlist assignments in backup.");
        }
    }
    public static void stage(Path directory,Data data)throws IOException{write(directory.resolve(PENDING),data);}

    /** Called before loading the library or starting workers. Interrupted installs roll back first. */
    public static boolean installPending(Path directory)throws IOException{
        Path journal=directory.resolve("restore-in-progress"),previous=directory.resolve("restore-previous");
        if(Files.exists(journal))rollback(directory,previous,journal);
        Path pending=directory.resolve(PENDING);if(!Files.exists(pending))return false;
        Data data=read(pending);
        for(String name:INSTALLED)if(Files.exists(directory.resolve(name))&&!Files.isRegularFile(directory.resolve(name)))
            throw new IOException("Expected an AFMR file at "+name+"; original data preserved.");
        Files.createDirectories(previous);
        var present=new ArrayList<String>();
        for(String name:INSTALLED){
            Path source=directory.resolve(name),old=previous.resolve(name);
            if(Files.exists(source)){Files.copy(source,old,StandardCopyOption.REPLACE_EXISTING);present.add(name);}
            else Files.deleteIfExists(old);
        }
        atomicWrite(journal,String.join("\n",present).getBytes(StandardCharsets.UTF_8));
        try{
            for(String name:INSTALLED){
                byte[] bytes=data.files.get(name.equals("restored-keys.json")?"keys.json":name);
                if(bytes==null)Files.deleteIfExists(directory.resolve(name));else atomicWrite(directory.resolve(name),bytes);
            }
            Files.delete(pending);Files.delete(journal);return true;
        }catch(IOException failure){
            try{rollback(directory,previous,journal);}catch(IOException recovery){failure.addSuppressed(recovery);}
            throw failure;
        }
    }
    private static void rollback(Path directory,Path previous,Path journal)throws IOException{
        var present=Files.readAllLines(journal,StandardCharsets.UTF_8);
        if(!INSTALLED.containsAll(present))throw new IOException("Invalid restore journal; original files preserved.");
        for(String name:INSTALLED){
            if(present.contains(name))atomicWrite(directory.resolve(name),Files.readAllBytes(previous.resolve(name)));
            else Files.deleteIfExists(directory.resolve(name));
        }
        Files.delete(journal);
    }
    public static Map<String,Key> restoredKeys(Path directory)throws IOException{
        Path file=directory.resolve("restored-keys.json");if(!Files.exists(file))return Map.of();
        var result=new LinkedHashMap<String,Key>();
        for(var e:json(Files.readAllBytes(file)).getAsJsonObject().entrySet())result.put(e.getKey(),GSON.fromJson(e.getValue(),Key.class));
        return result;
    }
    private static void atomicWrite(Path file,byte[] bytes)throws IOException{
        Path temp=Files.createTempFile(file.getParent(),"restore-",".tmp");
        try{Files.write(temp,bytes);move(temp,file);}finally{Files.deleteIfExists(temp);}
    }
    private static void move(Path from,Path to)throws IOException{
        try{Files.move(from,to,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException e){Files.move(from,to,StandardCopyOption.REPLACE_EXISTING);}
    }
}

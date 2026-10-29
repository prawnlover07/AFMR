package dev.animal.afmr.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.ListeningHistory;
import java.util.List;
import java.util.Arrays;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;

public final class LibraryStore implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t=new Thread(r,"AFMR library save");t.setDaemon(true);return t;
    });
    private Library pending;
    private List<ListeningHistory.Entry> pendingHistory;
    private boolean writing, closed;
    private IOException saveFailure;
    public LibraryStore(Path file) { this.file = file; }
    public Library load() throws IOException {
        if (!Files.exists(file)) return Library.defaults();
        try (var reader = Files.newBufferedReader(file)) {
            var json=JsonParser.parseReader(reader).getAsJsonObject();
            int schema=json.has("schemaVersion")?json.get("schemaVersion").getAsInt():2;
            if(schema<1||schema>2)throw new IOException("Unsupported library schema "+schema);
            Library library = SavedPlaylistRules.read(json,GSON);
            if (library == null || library.settings == null || library.tracks == null || library.folders == null || library.playlists == null)
                throw new IOException("Incomplete library configuration");
            if (library.schemaVersion < 1 || library.schemaVersion > 2) throw new IOException("Unsupported library schema " + library.schemaVersion);
            for (var p : library.playlists) if (p == null || p.id == null || p.name == null || p.tracks == null)
                throw new IOException("Invalid playlist in library");
            for (var t : library.tracks) if (t == null || t.id == null || t.path == null || t.title == null||t.content!=null&&(t.content.size()<0||!dev.animal.afmr.core.SongIdentity.validHash(t.content.sha256())))
                throw new IOException("Invalid track in library");
            if(schema==1) {
                Files.copy(file,file.resolveSibling("library-v1-before-upgrade.json"),StandardCopyOption.REPLACE_EXISTING);
            }
            library.normalize(); return library;
        } catch (RuntimeException e) { throw new IOException("Cannot parse library; original file preserved", e); }
    }
    public void save(Library library) throws IOException {
        library.normalize();replace(file,library,true);
    }
    public List<ListeningHistory.Entry> loadHistory()throws IOException{
        Path historyFile=file.resolveSibling("history.json");
        if(!Files.exists(historyFile))return List.of();
        if(Files.size(historyFile)>2*1024*1024)throw new IOException("Listening history exceeds 2 MiB");
        try(var reader=Files.newBufferedReader(historyFile)){
            var entries=GSON.fromJson(reader,ListeningHistory.Entry[].class);
            if(entries==null)throw new IOException("Incomplete listening history");
            var bounded=Arrays.asList(entries).subList(0,Math.min(entries.length,ListeningHistory.LIMIT));
            if(bounded.stream().anyMatch(e->!ListeningHistory.valid(e)))throw new IOException("Invalid listening history");
            return List.copyOf(bounded);
        }catch(RuntimeException e){throw new IOException("Cannot parse listening history; original file preserved",e);}
    }
    private static void replace(Path file,Object snapshot,boolean backup)throws IOException{
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (var writer = Files.newBufferedWriter(temporary)) { GSON.toJson(snapshot, writer); }
        if (backup&&Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }
    /** Keep only the newest waiting snapshot; one writer serializes all atomic replacements. */
    public synchronized void saveAsync(Library snapshot) {
        if(closed)throw new IllegalStateException("Library store is closed");
        pending=snapshot;
        if(!writing){writing=true;writer.execute(this::writePending);}
    }
    /** History has its own small immutable snapshot; never copy the music library on a song start. */
    public synchronized void saveHistoryAsync(List<ListeningHistory.Entry> snapshot){
        if(closed)throw new IllegalStateException("Library store is closed");
        if(snapshot.size()>ListeningHistory.LIMIT||snapshot.stream().anyMatch(e->!ListeningHistory.valid(e)))throw new IllegalArgumentException("Invalid history snapshot");
        pendingHistory=List.copyOf(snapshot);
        if(!writing){writing=true;writer.execute(this::writePending);}
    }
    private void writePending() {
        while(true){
            Library next;List<ListeningHistory.Entry> nextHistory;
            synchronized(this){
                next=pending;pending=null;nextHistory=pendingHistory;pendingHistory=null;
                if(next==null&&nextHistory==null){writing=false;return;}
            }
            if(next!=null)try{save(next);}
            catch(Exception e){synchronized(this){saveFailure=new IOException("Could not save library",e);}}
            if(nextHistory!=null)try{replace(file.resolveSibling("history.json"),nextHistory,false);}
            catch(Exception e){synchronized(this){saveFailure=new IOException("Could not save listening history",e);}}
        }
    }
    public synchronized IOException takeSaveFailure(){IOException error=saveFailure;saveFailure=null;return error;}
    @Override public void close() throws IOException {
        synchronized(this){closed=true;writer.shutdown();}
        try{if(!writer.awaitTermination(30,TimeUnit.SECONDS))throw new IOException("Library save did not finish before shutdown");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Interrupted while finishing library save",e);}
        IOException failure=takeSaveFailure();if(failure!=null)throw failure;
    }
}

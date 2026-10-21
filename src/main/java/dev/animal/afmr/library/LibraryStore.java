package dev.animal.afmr.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.animal.afmr.core.Library;
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
    private boolean writing, closed;
    private IOException saveFailure;
    public LibraryStore(Path file) { this.file = file; }
    public Library load() throws IOException {
        if (!Files.exists(file)) return Library.defaults();
        try (var reader = Files.newBufferedReader(file)) {
            Library library = GSON.fromJson(reader, Library.class);
            if (library == null || library.settings == null || library.tracks == null || library.folders == null || library.playlists == null)
                throw new IOException("Incomplete library configuration");
            if (library.schemaVersion < 1 || library.schemaVersion > 2) throw new IOException("Unsupported library schema " + library.schemaVersion);
            for (var p : library.playlists) if (p == null || p.id == null || p.rule == null || p.tracks == null || p.match == null)
                throw new IOException("Invalid playlist in library");
            for (var t : library.tracks) if (t == null || t.id == null || t.path == null || t.title == null)
                throw new IOException("Invalid track in library");
            if(library.schemaVersion==1) {
                Files.copy(file,file.resolveSibling("library-v1-before-upgrade.json"),StandardCopyOption.REPLACE_EXISTING);
                library.migrate();
            }
            if(library.playlist("dark_exploration")==null){
                var dark=new Library.Playlist("dark_exploration","Dark exploration",Library.Rule.DARK_EXPLORATION,"");
                dark.includeVanilla=false;
                var underground=library.playlist("underground");
                library.playlists.add(underground==null?Math.min(2,library.playlists.size()):library.playlists.indexOf(underground),dark);
            }
            library.normalize(); return library;
        } catch (RuntimeException e) { throw new IOException("Cannot parse library; original file preserved", e); }
    }
    public void save(Library library) throws IOException {
        library.normalize(); Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (var writer = Files.newBufferedWriter(temporary)) { GSON.toJson(library, writer); }
        if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }
    /** Keep only the newest waiting snapshot; one writer serializes all atomic replacements. */
    public synchronized void saveAsync(Library snapshot) {
        if(closed)throw new IllegalStateException("Library store is closed");
        pending=snapshot;
        if(!writing){writing=true;writer.execute(this::writePending);}
    }
    private void writePending() {
        while(true){
            Library next;
            synchronized(this){next=pending;pending=null;if(next==null){writing=false;return;}}
            try{save(next);}
            catch(Exception e){synchronized(this){saveFailure=new IOException("Could not save library",e);}}
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

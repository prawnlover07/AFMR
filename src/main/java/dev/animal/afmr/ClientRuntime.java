package dev.animal.afmr;

import dev.animal.afmr.audio.*;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.*;
import dev.animal.afmr.ui.NowPlaying;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class ClientRuntime {
    private static ClientRuntime instance;
    private static volatile boolean reloadRequested = true;
    private final Minecraft mc = Minecraft.getInstance();
    private final LibraryStore store = new LibraryStore(FMLPaths.CONFIGDIR.get().resolve("afmr/library.json"));
    private final VanillaCatalog vanilla = new VanillaCatalog();
    private final PlaybackEngine engine;
    private final NowPlaying overlay = new NowPlaying(this);
    private final SurfaceCover surfaceCover = new SurfaceCover();
    private final ExecutorService scanner = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "AFMR library"); t.setDaemon(true); return t; });
    private final Library library;
    private boolean failedLoad, scannedLaunch, inWorld, scanning;
    private volatile MusicFiles.ScanProgress scanProgress = MusicFiles.ScanProgress.starting();
    private final CombatTracker combatTracker=new CombatTracker();
    private Object combatPlayer;
    private double clock, lastWall = System.nanoTime() / 1e9;
    private String status = "";
    private String unavailable = "";
    private long catalogRevision;
    private Object worldIdentity;
    private String cachedWorldKey="";
    private PlaylistImportReport lastPlaylistImport;
    private Set<String> applicableVanilla = Set.of();
    private Context context = new Context("", "", false, false, 6000,false,false,false,15,"",0,0,0,0,0.8,false);

    private ClientRuntime() {
        Library loaded;
        try { loaded = store.load(); }
        catch (Exception e) {
            loaded = Library.defaults(); failedLoad = true;
            status = "Library could not load. Original JSON preserved; see logs.";
            Afmr.LOG.error(status, e);
        }
        library = loaded;
        engine=new PlaybackEngine(new GameAudio(mc,library.settings),new Random());
    }
    public static ClientRuntime get() { if (instance == null) instance = new ClientRuntime(); return instance; }
    public static void requestReload() { reloadRequested = true; }
    public static void shutdown() {
        if(instance==null)return;
        instance.scanner.shutdownNow();
        try{instance.store.close();}catch(Exception e){Afmr.LOG.error("Could not finish saving AFMR library",e);}
    }
    public boolean enabled() { return !failedLoad; }
    public Library library() { return library; }
    public PlaybackEngine engine() { return engine; }
    public NowPlaying overlay() { return overlay; }
    public Context context() { return context; }
    public long catalogRevision() { return catalogRevision; }
    public PlaylistImportReport lastPlaylistImport() { return lastPlaylistImport; }
    public double now() { return clock; }
    public boolean scanning() { return scanning; }
    public MusicFiles.ScanProgress scanProgress() { return scanProgress; }
    public String status() {
        if(engine.paused())return "Music paused. Resume with the pause key or Inspector control.";
        if (!engine.error().isBlank()) return engine.error();
        if(context.dead()&&library.settings.preventMusicWhileDead&&engine.current()==null)return "New music resumes after respawning.";
        if(!scanning&&engine.overrideId()!=null){var p=library.playlist(engine.overrideId());if(p!=null)return "Override: "+p.name+(engine.overrideMode()==OverrideMode.ONE_SONG?" / one song":" / until released");}
        if (!scanning && inWorld() && engine.current() == null) {
            if (!unavailable.isBlank()) return unavailable;
        }
        return status;
    }
    public String environmentLabel() {
        return (context.day() ? "Day" : "Night") + " / " + (context.underground() ? "Underground" : "Outside")
            + (EnvironmentConditions.dark(context.dimension(),context.covered(),context.lightLevel(),library.settings)?" / Dark":"")
            + (context.combat() && library.settings.combatEnabled ? " / Combat" : context.atHome() ? " / Home" : "");
    }
    public Set<String> applicableVanilla() { return applicableVanilla; }

    public void tick() {
        var saveFailure=store.takeSaveFailure();
        if(saveFailure!=null){status="Could not save library: "+saveFailure.getMessage();Afmr.LOG.error(status,saveFailure);}
        double wall = System.nanoTime() / 1e9;
        if (!mc.isPaused()) clock += Math.min(0.25, Math.max(0, wall - lastWall));
        lastWall = wall;
        if (reloadRequested && mc.getResourceManager() != null) {
            reloadRequested = false; engine.reset(); vanilla.reload(mc, library); catalogRevision++;
        }
        if (!scannedLaunch) {
            scannedLaunch = true;
            if (library.settings.rescanOnLaunch) rescan();
        }
        if (mc.level == null || mc.player == null) {
            if (inWorld) { engine.reset(); overlay.clear(); surfaceCover.clear();combatTracker.reset();combatPlayer=null; }
            inWorld = false; return;
        }
        if (!enabled()) return;
        if (!inWorld) { mc.getMusicManager().stopPlaying(); inWorld = true; }
        if (mc.isPaused()) return;
        if(combatPlayer!=mc.player){combatPlayer=mc.player;combatTracker.reset();}
        boolean dead=mc.player.isDeadOrDying();
        combatTracker.update(clock,mc.player.getHealth()+mc.player.getAbsorptionAmount(),mc.player.hurtTime,dead,library.settings);
        var pos=mc.player.blockPosition();
        int surface=surfaceCover.height(mc.level,pos.getX(),pos.getZ());
        double coverDepth=surface-mc.player.getY();
        boolean covered=!mc.level.canSeeSky(pos) && coverDepth>1;
        boolean underground=EnvironmentConditions.underground(pos.getY(),covered,coverDepth,library.settings);
        int light=mc.level.getMaxLocalRawBrightness(pos);
        var biome=mc.level.getBiome(pos);
        context = new Context(mc.level.dimension().identifier().toString(),biome.unwrapKey().map(key->key.identifier().toString()).orElse(""),
            combatTracker.active(clock,library.settings),mc.player.isUnderWater(),(int)Math.floorMod(mc.level.getOverworldClockTime(),24000),
            underground,false,covered,light,worldKey(),mc.player.getX(),mc.player.getY(),mc.player.getZ(),coverDepth,biome.value().getBaseTemperature(),dead);
        context=context.withHome(library.home.contains(context));
        applicableVanilla = vanilla.applicable(mc);
        engine.tick(clock, library, context, applicableVanilla); overlay.tick();
        unavailable = engine.selected() == null ? library.playbackUnavailable(context, applicableVanilla) : "";
    }

    public boolean inWorld() { return mc.level!=null && mc.player!=null; }
    public int currentBlockY() { return mc.player==null?0:mc.player.blockPosition().getY(); }
    public String worldKey() {
        Object identity=mc.getSingleplayerServer()!=null?mc.getSingleplayerServer():mc.getCurrentServer();
        if(identity!=worldIdentity){
            worldIdentity=identity;
            cachedWorldKey=mc.getSingleplayerServer()!=null?"singleplayer:"+mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize()
                :mc.getCurrentServer()!=null?"server:"+mc.getCurrentServer().ip.toLowerCase(Locale.ROOT):"";
        }
        return cachedWorldKey;
    }
    public void captureHome() {
        if(!inWorld()) { status="Join a world before capturing Home.";return; }
        Home home=library.home;home.x=mc.player.getX();home.y=mc.player.getY();home.z=mc.player.getZ();
        home.dimension=mc.level.dimension().identifier().toString();home.world=worldKey();home.configured=true;
        save();status="Home center captured at your current location.";
    }
    public static String pretty(String id) {
        String path = id.substring(id.indexOf(':') + 1).replace('_', ' ');
        return path.isEmpty() ? id : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }
    public void attack() { if(inWorld()&&!mc.player.isDeadOrDying())combatTracker.attack(clock,library.settings); }
    public void skip() { engine.skip(clock, library.settings); }
    public void playNow() { engine.playNow(clock); }
    public void nextSong(){
        if(!inWorld()||!enabled()){status="Join a world before selecting the next song.";return;}
        status=engine.nextSong(clock,library,context,applicableVanilla)?"":"No next song available for the current playlist selection.";
    }
    public void togglePause(){
        if(!inWorld()||!enabled()){status="Join a world before pausing music.";return;}
        engine.setPaused(!engine.paused(),clock);status="";
    }
    public boolean overridePlaylist(String id,OverrideMode mode){
        if(!inWorld()||!enabled()){status="Join a world before overriding music.";return false;}
        if(!engine.overridePlaylist(id,mode,clock,library,applicableVanilla)){
            status="Override unavailable: add or enable available songs, or retry failed music.";return false;
        }
        status="";return true;
    }
    public void returnAutomatic(){engine.returnAutomatic();status="Returned to automatic playlist selection.";}
    public void overrideSlot(int index){
        var binding=library.settings.overrideSlots[index];
        if(binding.playlistId.isBlank()){status="Override slot "+(index+1)+" is unassigned. Choose a playlist under Overrides > Keybind slots.";return;}
        if(binding.mode==OverrideMode.UNTIL_RELEASED&&binding.playlistId.equals(engine.overrideId())&&engine.overrideMode()==binding.mode)returnAutomatic();
        else overridePlaylist(binding.playlistId,binding.mode);
    }
    public void preview(Track track) {
        if (!inWorld()) { status = "Join a world before previewing music."; return; }
        if (!enabled()) return;
        engine.preview(track, clock);
        status = engine.error().isBlank() ? "Previewing: " + track.title : engine.error();
    }
    public void message(String message) { status=message; }
    public void save() {
        if (failedLoad) { status = "Save blocked: repair the preserved library.json first."; return; }
        try { library.normalize(); store.saveAsync(library.snapshot()); }
        catch (Exception e) { status = "Could not save library: " + e.getMessage(); Afmr.LOG.error(status, e); }
    }
    public void rescan() {
        List<MusicFiles.Request> requests = library.folders.stream()
            .map(f -> new MusicFiles.Request(Path.of(f.path), f.playlistId, f.recursive)).toList();
        scan(requests,false);
    }
    public void importPaths(List<Path> paths, String playlist, boolean recursive) {
        for (Path path : paths) if (Files.isDirectory(path)) {
            String normalized = path.toAbsolutePath().normalize().toString();
            if (library.folders.stream().noneMatch(f -> f.path.equals(normalized))) {
                Folder folder = new Folder(normalized, playlist); folder.recursive = recursive; library.folders.add(folder);
            }
        }
        save(); scan(paths.stream().map(p -> new MusicFiles.Request(p, playlist, recursive)).toList(),false);
    }
    public void importPlaylists(List<Path> paths,String destination){
        if(library.playlist(destination)==null){status="Choose a destination playlist first.";return;}
        scan(paths.stream().map(p->new MusicFiles.Request(p,destination,false)).toList(),true);
    }
    private void scan(List<MusicFiles.Request> requests,boolean playlistImport) {
        if (scanning) { status = "A scan is already running."; return; }
        if(requests.isEmpty()){status="No files selected.";return;}
        scanProgress = MusicFiles.ScanProgress.starting();
        String destination=playlistImport?library.playlist(requests.getFirst().playlistId()).name:"";
        List<Track> known=playlistImport?List.copyOf(library.tracks):List.of();
        scanning = true; status = playlistImport?"Importing playlists into "+destination+"...":"Scanning music folders...";
        // Publish a single latest snapshot; a large scan must not queue thousands of client tasks.
        CompletableFuture.supplyAsync(() -> MusicFiles.scan(requests,known,progress -> scanProgress = progress), scanner).whenComplete((result, failure) -> mc.execute(() -> {
            try {
                if (failure != null) {
                    status = "Scan failed: " + failure.getMessage(); Afmr.LOG.error(status, failure);
                    if(playlistImport)lastPlaylistImport=new PlaylistImportReport(destination,0,0,0,1,0,0,List.of(),List.of(status));
                    return;
                }
                Map<String, Track> byId = new HashMap<>(); library.tracks.forEach(t -> byId.put(t.id, t));
                int added = 0,assigned=0,existingAssignments=0;
                for (var found : result.found()) {
                    Track existing = byId.get(found.track().id);
                    if (existing == null) { library.tracks.add(found.track()); byId.put(found.track().id, found.track()); added++; }
                    else { existing.title = found.track().title; existing.artist = found.track().artist; existing.album = found.track().album;existing.durationSeconds=found.track().durationSeconds; }
                    Playlist target=library.playlist(found.playlistId());
                    if(target!=null){if(target.tracks.containsKey(found.track().id))existingAssignments++;else assigned++;}
                    library.assignImported(found.playlistId(),found.track().id);
                }
                if(playlistImport){
                    lastPlaylistImport=new PlaylistImportReport(destination,assigned,existingAssignments,added,result.skipped(),result.duplicates(),result.reused(),result.playlists(),result.errors());
                    status="Imported into "+destination+": "+assigned+" added, "+existingAssignments+" already assigned, "+result.skipped()+" skipped.";
                }else status = "Scan complete: " + added + " new tracks, " + result.skipped() + " issues.";
                result.errors().forEach(message -> Afmr.LOG.warn("AFMR scan: {}", message));
                library.rebuildTrackIndex(); catalogRevision++;
                engine.retryFailed(); save();
            } finally { scanning = false; }
        }));
    }
    public static void reportAudioFailure(Path path, Exception error) {
        Afmr.LOG.error("Cannot decode {}", path, error);
        Minecraft.getInstance().execute(() -> {
            ClientRuntime runtime = get();
            Track current = runtime.engine.current();
            if (current != null && !current.vanilla && Path.of(current.path).equals(path))
                runtime.engine.failed(current.id, "Could not play " + current.title + ": " + error.getMessage(), runtime.clock, runtime.library.settings);
        });
    }
}

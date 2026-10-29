package dev.animal.afmr;

import dev.animal.afmr.audio.*;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.*;
import dev.animal.afmr.ui.NowPlaying;
import dev.animal.afmr.ui.CustomBackground;
import dev.animal.afmr.ui.StructureConditionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.ModList;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientRuntime {
    private static ClientRuntime instance;
    private static volatile boolean reloadRequested = true;
    private static final SystemToast.SystemToastIds MISSING_SOURCES=SystemToast.SystemToastIds.PERIODIC_NOTIFICATION;
    private final Minecraft mc = Minecraft.getInstance();
    private final Path configDirectory=FMLPaths.CONFIGDIR.get().resolve("afmr");
    private ConfigBackup.Data pendingBackup;
    private boolean backupQueued,backupBusy;
    private final LibraryStore store = new LibraryStore(FMLPaths.CONFIGDIR.get().resolve("afmr/library.json"));
    private final VanillaCatalog vanilla = new VanillaCatalog();
    private final PlaybackEngine engine;
    private final NowPlaying overlay = new NowPlaying(this);
    private final CustomBackground menuBackground = new CustomBackground(this,FMLPaths.CONFIGDIR.get().resolve("afmr/menu-background.png"),CustomBackground.Target.MENU);
    private final CustomBackground widgetBackground = new CustomBackground(this,FMLPaths.CONFIGDIR.get().resolve("afmr/widget-background.png"),CustomBackground.Target.WIDGET);
    private final dev.animal.afmr.ui.CustomFonts fonts=new dev.animal.afmr.ui.CustomFonts(this,configDirectory);
    private final dev.animal.afmr.ui.Themes themes=new dev.animal.afmr.ui.Themes(this);
    public dev.animal.afmr.ui.Themes themes(){return themes;}
    public dev.animal.afmr.ui.CustomFonts fonts(){return fonts;}
    private final SurfaceCover surfaceCover = new SurfaceCover();
    private final JukeboxTracker jukebox=new JukeboxTracker();
    private final ExecutorService scanner = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "AFMR library"); t.setDaemon(true); return t; });
    private final Library library;
    private boolean failedLoad, scannedLaunch, inWorld, scanning, closed;
    private volatile MusicFiles.ScanProgress scanProgress = MusicFiles.ScanProgress.starting();
    private final CombatTracker combatTracker=new CombatTracker();
    private Object combatPlayer;
    private Item observedMainHand,observedOffHand;
    private String cachedMainHandId="",cachedOffHandId="";
    private double clock, lastWall = System.nanoTime() / 1e9;
    private String status = "";
    private String unavailable = "";
    private long catalogRevision;
    public boolean minecraftMusicMuted(){return mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC)==0||mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0;}
    private final PlaylistEditHistory playlistEdits=new PlaylistEditHistory();
    public PlaylistEditHistory playlistEdits(){return playlistEdits;}
    private Object worldIdentity;
    private String cachedWorldKey="";
    private PlaylistImportReport lastPlaylistImport;
    private LibraryHealth.Report sourceHealth;
    private LibraryRepair.Plan pendingRepair;
    private AtomicBoolean repairCancellation;
    private String repairStatus="";
    public record PresetImport(PlaylistPresets.FileData data,String profileId,String anchor,String fileName){}
    private PresetImport pendingPreset;
    private PresetSongs.Plan presetSongPlan;
    private Map<String,Path> presetFolders=Map.of();
    private Map<String,Path> presetFiles=Map.of();
    private List<Path> presetSearchFolders=List.of();
    private boolean presetSearchLinked;
    private AtomicBoolean presetCancellation;
    private Path presetMissingRoot;
    private record ScanResult(MusicFiles.Result scan,LibraryHealth.Report health){}
    private final LoudnessScan loudnessScanner=new LoudnessScan();
    private LoudnessScan.Job loudnessJob;
    private LoudnessScan.Report lastLoudnessReport;
    private Set<String> applicableVanilla = Set.of();
    private record DurationResult(String id,String path,boolean vanilla,double seconds) {}
    private CompletableFuture<DurationResult> durationCheck;
    private boolean durationDirty;
    private long savedHistoryRevision;
    private boolean failedHistoryLoad;
    private double nextDurationSave;
    private final PlayerProximity playerProximity=new PlayerProximity();
    private Context context = new Context("", "", false, false, 6000,false,15,"",0,0,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,Double.NaN,false,"","",null,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);

    private ClientRuntime() {
        try{
            if(ConfigBackup.installPending(configDirectory))status="Full backup restored.";
            var keys=ConfigBackup.restoredKeys(configDirectory);
            if(!keys.isEmpty()){Afmr.restoreKeys(keys);Files.delete(configDirectory.resolve("restored-keys.json"));}
        }catch(Exception e){status="Backup restore failed; recovery files preserved: "+e.getMessage();Afmr.LOG.error(status,e);}
        Library loaded;
        try { loaded = store.load(); }
        catch (Exception e) {
            loaded = Library.defaults(); failedLoad = true;
            status = "Library could not load. Original JSON preserved; see logs.";
            Afmr.LOG.error(status, e);
        }
        library = loaded;
        if(!failedLoad)library.profiles.activate(library,PlaylistProfiles.DEFAULT);
        engine=new PlaybackEngine(new GameAudio(mc,library.settings),new Random());
        try{engine.history().restore(store.loadHistory());}
        catch(Exception e){
            engine.history().clear();failedHistoryLoad=true;
            status="Listening history could not load. Original preserved; Clear history to reset it.";Afmr.LOG.error(status,e);
        }
        savedHistoryRevision=engine.history().revision();
    }
    public static ClientRuntime get() { if (instance == null) instance = new ClientRuntime(); return instance; }
    public static void requestReload() { reloadRequested = true; }
    /** Clear session state at logout, even when another connection starts before the next client tick. */
    public static void disconnect() {
        if(instance==null)return;
        instance.saveHistory();
        if(instance.durationDirty)instance.save();
        instance.themes.cancelPreview();instance.playerProximity.clear();instance.engine.reset();instance.overlay.clear();instance.surfaceCover.clear();instance.jukebox.clear();
        ClientConditions.clear();
        instance.durationCheck=null;
        instance.observedMainHand=instance.observedOffHand=null;instance.cachedMainHandId=instance.cachedOffHandId="";
        instance.combatTracker.reset();instance.combatPlayer=null;instance.inWorld=false;
        instance.worldIdentity=null;instance.cachedWorldKey="";instance.applicableVanilla=Set.of();instance.unavailable="";
        instance.context=new Context("", "", false, false, 6000,false,15,"",0,0,0,0,0.8,false,StructureContext.UNAVAILABLE,false,false,Precipitation.NONE,Double.NaN,false,"","",null,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
        instance.activateProfile(PlaylistProfiles.DEFAULT);
    }
    public static void shutdown() {
        if(instance==null||instance.closed)return;
        instance.saveHistory();
        instance.finishDurationCheck();
        ClientConditions.clear();
        if(instance.durationDirty)instance.save();
        instance.scanner.shutdownNow();
        instance.loudnessScanner.close();instance.finishLoudnessScan();
        // GameShuttingDown can precede LoggingOut. Save the profile transition while
        // the writer is open, then reject late screen/logout saves after the flush.
        disconnect();
        instance.menuBackground.close();instance.widgetBackground.close();instance.fonts.close();instance.themes.close();
        instance.closed=true;
        try{instance.store.close();}catch(Exception e){Afmr.LOG.error("Could not finish saving AFMR library",e);}
    }
    public boolean enabled() { return !failedLoad&&!closed; }
    public Library library() { return library; }
    public PlaybackEngine engine() { return engine; }
    public NowPlaying overlay() { return overlay; }
    public CustomBackground menuBackground() { return menuBackground; }
    public CustomBackground widgetBackground() { return widgetBackground; }
    public Context context() { return context; }
    public JukeboxTracker jukebox(){return jukebox;}
    public boolean serverConditionsAvailable(){return ClientConditions.supported();}
    public Set<String> structureIds(){return ClientConditions.catalog();}
    public long catalogRevision() { return catalogRevision; }
    public PlaylistImportReport lastPlaylistImport() { return lastPlaylistImport; }
    public LibraryHealth.Report sourceHealth(){return sourceHealth;}
    public LibraryRepair.Plan pendingRepair(){return pendingRepair;}
    public String repairStatus(){return repairStatus;}
    public boolean repairSearchActive(){return repairCancellation!=null;}
    public boolean repairSearchCancelling(){return repairCancellation!=null&&repairCancellation.get();}
    public void cancelRepairSearch(){
        if(repairCancellation==null||repairCancellation.get())return;
        repairCancellation.set(true);repairStatus=status="Cancelling repair search...";
    }
    public String taskStatus(){return status;}
    public PresetImport pendingPreset(){return pendingPreset;}
    public PresetSongs.Plan presetSongPlan(){return presetSongPlan;}
    public boolean presetSearchActive(){return presetCancellation!=null;}
    public boolean presetSearchCancelling(){return presetCancellation!=null&&presetCancellation.get();}
    public void cancelPresetSearch(){
        if(presetCancellation!=null){presetCancellation.set(true);status="Cancelling song search...";}
    }
    public double now() { return clock; }
    public String playbackUnavailable(){return unavailable;}
    public boolean scanning() { return scanning; }
    public MusicFiles.ScanProgress scanProgress() { return scanProgress; }
    public LoudnessScan.Job loudnessJob(){return loudnessJob;}
    public LoudnessScan.Report lastLoudnessReport(){return lastLoudnessReport;}
    public String status() {
        if(engine.paused())return "Paused";
        if (!engine.error().isBlank()) return engine.error();
        if(engine.waitingForTimeFit())return "Waiting / no song fits the time window";
        if(context.dead()&&library.settings.preventMusicWhileDead&&engine.current()==null)return "Waiting / respawn";
        if(!scanning&&engine.overrideId()!=null){var p=library.playlist(engine.overrideId());if(p!=null)return "Override: "+p.name+(engine.overrideMode()==OverrideMode.ONE_SONG?" / one song":" / until released");}
        if (!scanning && inWorld() && engine.current() == null) {
            if (!unavailable.isBlank()) return unavailable;
        }
        return status;
    }
    /** Capture only on an explicit clipboard request; no I/O, log scraping or library snapshot. */
    public String diagnosticSummary(String inspectedPlaylistId){
        String modVersion=ModList.get().getModContainerById("afmr").map(m->m.getModInfo().getVersion().toString()).orElse("unknown");
        String loaderVersion=ModList.get().getModContainerById("forge").map(m->m.getModInfo().getVersion().toString()).orElse("unknown");
        var memory=Runtime.getRuntime();
        var system=new DiagnosticSummary.SystemInfo(modVersion,net.minecraft.SharedConstants.getCurrentVersion().getName(),"Forge",loaderVersion,
            System.getProperty("java.version","unknown"),System.getProperty("os.name","unknown"),System.getProperty("os.arch","unknown"),
            mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER),mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC),
            memory.totalMemory()-memory.freeMemory(),memory.totalMemory(),memory.maxMemory());
        var progress=scanProgress;
        var session=new DiagnosticSummary.Session(inWorld(),mc.getSingleplayerServer()!=null,enabled(),scanning,
            progress.stage().name(),progress.processed(),progress.discovered(),sourceHealth==null?-1:sourceHealth.issues().size());
        return DiagnosticSummary.create(system,session,library,engine,context,applicableVanilla,clock,inspectedPlaylistId);
    }
    public String environmentLabel() {
        return (context.day()?"Day":"Night")+" / "+(context.covered()?"Covered":"Open sky")
            +(context.combat()&&library.settings.combatEnabled?" / Combat":"");
    }

    public Set<String> applicableVanilla() { return applicableVanilla; }

    public void tick() {
        if(closed)return;
        saveHistory();
        finishLoudnessScan();
        var saveFailure=store.takeSaveFailure();
        if(saveFailure!=null){status="Could not save AFMR data: "+saveFailure.getMessage();Afmr.LOG.error(status,saveFailure);}
        double wall = System.nanoTime() / 1e9;
        if (!mc.isPaused()) clock += Math.min(0.25, Math.max(0, wall - lastWall));
        lastWall = wall;
        if (reloadRequested && mc.getResourceManager() != null) {
            reloadRequested = false;durationCheck=null;engine.reset(); overlay.clear(); menuBackground.invalidate();widgetBackground.invalidate(); vanilla.reload(mc, library); catalogRevision++;
        }
        fonts.start();
        finishDurationCheck();
        if(durationDirty&&clock>=nextDurationSave){nextDurationSave=clock+30;save();}
        if (!scannedLaunch) {
            scannedLaunch = true;
            if (library.settings.rescanOnLaunch) rescan();
        }
        if (mc.level == null || mc.player == null) {
            if (inWorld) disconnect();
            return;
        }
        if (!enabled()) return;
        if (!inWorld) {
            activateProfile(library.profiles.forWorld(worldKey()));
            engine.joinedWorld(library.settings);
            mc.getMusicManager().stopPlaying(); inWorld = true;
        }
        if (mc.isPaused()) { overlay.tick(); return; }
        updateCombatPlayer();
        boolean dead=mc.player.isDeadOrDying();
        var pos=mc.player.blockPosition();
        int surface=surfaceCover.height(mc.level.getChunk(pos.getX()>>4,pos.getZ()>>4),mc.level.getGameTime(),pos.getX(),pos.getZ());
        double coverDepth=surface-mc.player.getY();
        boolean covered=!mc.level.canSeeSky(pos) && coverDepth>1;
        int light=mc.level.getMaxLocalRawBrightness(pos);
        var biome=mc.level.getBiome(pos);
        int structureRadius=mc.screen instanceof StructureConditionScreen?0:-1;
        boolean observePlayers=false;
        for(var p:library.playlists){
            if(p.enabled&&!p.manualOnly)structureRadius=Math.max(structureRadius,p.conditions.structureSearchRadius());
            if(!observePlayers)observePlayers=p.conditions.needsNearbyPlayers();
        }
        if(!observePlayers)playerProximity.clear();
        ClientConditions.update(mc,clock,structureRadius,combatTracker.active(clock,library.settings));
        combatTracker.targeting(clock,ClientConditions.targeted(),library.settings);
        var structures=ClientConditions.structures();
        boolean raining=mc.level.isRaining(),thundering=mc.level.isThundering();
        // Match the client's rendered rain/snow type without tying weather to roof exposure.
        Precipitation precipitation=raining?switch((biome.value().getPrecipitation()==net.minecraft.world.level.biome.Biome.Precipitation.NONE?net.minecraft.world.level.biome.Biome.Precipitation.NONE:biome.value().coldEnoughToSnow(pos)?net.minecraft.world.level.biome.Biome.Precipitation.SNOW:net.minecraft.world.level.biome.Biome.Precipitation.RAIN)){
            case NONE->Precipitation.NONE;case RAIN->Precipitation.RAIN;case SNOW->Precipitation.SNOW;
        }:Precipitation.NONE;
        var mainStack=mc.player.getMainHandItem();var offStack=mc.player.getOffhandItem();
        var mainItem=mainStack.isEmpty()?null:mainStack.getItem();var offItem=offStack.isEmpty()?null:offStack.getItem();
        // Cache registry strings by item identity; durability, stack count and names need no rebuild.
        if(mainItem!=observedMainHand){observedMainHand=mainItem;cachedMainHandId=mainItem==null?"":Registry.ITEM.getKey(mainItem).toString();}
        if(offItem!=observedOffHand){observedOffHand=offItem;cachedOffHandId=offItem==null?"":Registry.ITEM.getKey(offItem).toString();}
        context = new Context(mc.level.dimension().location().toString(),biome.unwrapKey().map(key->key.location().toString()).orElse(""),
            combatTracker.active(clock,library.settings),mc.player.isUnderWater(),(int)Math.floorMod(mc.level.getDayTime(),24000),covered,light,worldKey(),mc.player.getX(),mc.player.getY(),mc.player.getZ(),coverDepth,biome.value().getBaseTemperature(),dead,structures,raining,thundering,precipitation,mc.player.getHealth()/2.0,combatTracker.playersActive(clock,library.settings),cachedMainHandId,cachedOffHandId,mc.gameMode==null?null:switch(mc.gameMode.getPlayerMode()){
                case SURVIVAL->GameMode.SURVIVAL;case CREATIVE->GameMode.CREATIVE;
                case ADVENTURE->GameMode.ADVENTURE;case SPECTATOR->GameMode.SPECTATOR;
            },jukebox.audible(mc),observePlayers?playerProximity.update(mc,clock):dev.animal.afmr.core.NearbyPlayers.EMPTY,mc.player.getVehicle() instanceof net.minecraft.world.entity.animal.Animal);
        applicableVanilla = vanilla.applicable(mc);
        engine.tick(clock, library, context, applicableVanilla); overlay.tick();
        saveHistory();
        if(durationCheck==null){
            var track=engine.takeDurationRequest();
            if(track!=null){
                String id=track.id,path=track.path;boolean resource=track.vanilla;
                var resources=mc.getResourceManager();
                // One source snapshot, one existing library worker; no mutable Track crosses threads.
                durationCheck=CompletableFuture.supplyAsync(()->{
                    double seconds=Double.NaN;
                    try{
                        if(resource){
                            var location=new net.minecraft.resources.ResourceLocation(path);
                            var file=new net.minecraft.resources.ResourceLocation(location.getNamespace(),"sounds/"+location.getPath()+".ogg");
                            try(var input=resources.getResourceOrThrow(file).open()){seconds=TrackDuration.vorbis(input);}
                        }else seconds=TrackDuration.local(Path.of(path));
                    }catch(Exception e){Afmr.LOG.debug("Could not determine song length: {}",path,e);}
                    return new DurationResult(id,path,resource,seconds);
                },scanner);
            }
        }
        unavailable = engine.selected() == null ? library.playbackUnavailable(context, applicableVanilla) : "";
    }

    private void finishDurationCheck(){
        if(durationCheck==null||!durationCheck.isDone())return;
        DurationResult result;
        try{result=durationCheck.join();}catch(Exception e){durationCheck=null;Afmr.LOG.debug("Song length check failed",e);return;}
        durationCheck=null;
        var track=library.track(result.id());
        if(track==null||!track.path.equals(result.path())||track.vanilla!=result.vanilla())return;
        if(Double.isFinite(result.seconds())&&result.seconds()>0){
            if(!Double.isFinite(track.durationSeconds)||track.durationSeconds<=0){track.durationSeconds=result.seconds();durationDirty=true;catalogRevision++;}
            if(engine.waitingForTimeFit())engine.playNow(clock);
        }
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
    public static String pretty(String id) {
        String path = id.substring(id.indexOf(':') + 1).replace('_', ' ');
        return path.isEmpty() ? id : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }
    private void updateCombatPlayer(){
        if(combatPlayer!=mc.player){combatPlayer=mc.player;combatTracker.reset();ClientConditions.clear();}
        combatTracker.update(mc.player.isDeadOrDying());
    }
    public void attack(Entity target){
        if(!inWorld()||!enabled())return;
        updateCombatPlayer();combatTracker.attack(clock,dev.animal.afmr.server.CombatMobs.classify(target,mc.player),library.settings);
    }
    public void combatHit(dev.animal.afmr.network.CombatHit hit){
        if(!inWorld()||!enabled()||hit.entityId()!=mc.player.getId()||!hit.dimension().equals(mc.level.dimension().location().toString()))return;
        updateCombatPlayer();
        if(hit.incoming())combatTracker.damage(clock,hit.opponent(),library.settings);
        else combatTracker.attack(clock,hit.opponent(),library.settings);
    }
    public void skip() { engine.skip(clock, library); }
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
    public void returnAutomatic(){engine.returnAutomatic();status="Automatic";}
    public void overrideSlot(int index){
        var binding=library.settings.overrideSlots[index];
        if(binding.playlistId.isBlank()){status="Override slot "+(index+1)+" / unassigned";return;}
        if(binding.mode==OverrideMode.UNTIL_RELEASED&&binding.playlistId.equals(engine.overrideId())&&engine.overrideMode()==binding.mode)returnAutomatic();
        else overridePlaylist(binding.playlistId,binding.mode);
    }
    public void preview(Track track) {
        if (!inWorld()) { status = "Join a world before previewing music."; return; }
        if (!enabled()) return;
        engine.preview(track, clock);
        saveHistory();
        status = engine.error().isBlank() ? "Previewing: " + track.title : engine.error();
    }
    private void saveHistory(){
        if(closed)return;
        var history=engine.history();
        if(failedHistoryLoad||savedHistoryRevision==history.revision())return;
        store.saveHistoryAsync(history.snapshot());savedHistoryRevision=history.revision();
    }
    public void clearHistory(){
        engine.history().clear();failedHistoryLoad=false;saveHistory();status="History cleared";
    }
    public boolean queueTrack(String trackId,String playlistId){
        if(!inWorld()||!enabled()){status="Join a world before queuing music.";return false;}
        boolean added=engine.enqueue(trackId,playlistId,library);status=engine.queueMessage();return added;
    }
    public boolean addTrackToPlaylist(String trackId,String destination){
        var track=library.track(trackId);var playlist=library.playlist(destination);
        if(track==null||playlist==null){status="Song or destination playlist is no longer available.";return false;}
        Boolean previous=playlist.tracks.get(track.id);if(previous==null)playlistEdits.assign(library,playlist,java.util.List.of(track.id),true);save();
        status=previous==null?"Added to "+playlist.name:previous?"Already in "+playlist.name:"Already assigned, disabled in "+playlist.name;
        return true;
    }
    public void message(String message) { status=repairStatus=message; }
    /** Called only at explicit setup/world transitions; shared track storage and audio settings retain identity. */
    public boolean activateProfile(String id){
        if(!enabled()||!library.profiles.activate(library,id))return false;
        saveHistory();engine.reset();overlay.clear();combatTracker.reset();ClientConditions.clear();
        dev.animal.afmr.ui.MusicMenu.clear();catalogRevision++;unavailable="";
        save();status="Profile: "+library.profiles.activeName;return true;
    }
    public boolean normalizePlaylist(String id){
        if(!enabled())return false;
        if(loudnessJob!=null&&loudnessJob.report()==null)return true;
        finishLoudnessScan();
        Playlist playlist=library.playlist(id);if(playlist==null)return false;
        var requests=LoudnessScan.requests(library,playlist);
        if(requests.isEmpty()){status="This playlist has no imported music to normalize.";return false;}
        library.settings.replayGainEnabled=true;
        loudnessJob=loudnessScanner.start(playlist.name,requests);
        save();status="Normalizing imported music in "+playlist.name+"...";
        return true;
    }
    private void finishLoudnessScan(){
        if(loudnessJob==null||loudnessJob.report()==null||loudnessJob.report()==lastLoudnessReport)return;
        lastLoudnessReport=loudnessJob.report();boolean changed=false;
        for(var row:lastLoudnessReport.rows())if(row.measurement()!=null){
            Track track=library.track(row.track().id());
            if(track!=null&&!track.vanilla&&track.path.equals(row.track().path())){track.loudness=row.measurement();changed=true;}
        }
        if(changed){catalogRevision++;save();}
        status=(lastLoudnessReport.cancelled()?"Normalization cancelled: ":"Normalization complete: ")
            +lastLoudnessReport.rows().size()+" / "+lastLoudnessReport.total()+" processed, "
            +lastLoudnessReport.count(LoudnessScan.Source.FAILED)+" issues. View the loudness report for dB adjustments.";
    }
    public void save() {
        if(closed)return;
        if (failedLoad) { status = "Save blocked: repair the preserved library.json first."; return; }
        try { library.normalize(); playlistEdits.checkpoint(library); store.saveAsync(library.snapshot());durationDirty=false; }
        catch (Exception e) { status = "Could not save library: " + e.getMessage(); Afmr.LOG.error(status, e); }
    }
    public void rescan() {
        List<MusicFiles.Request> requests = library.folders.stream()
            .map(f -> new MusicFiles.Request(Path.of(f.path),"library",f.recursive)).toList();
        scan(requests,false,library.profiles.activeId,new FolderAssignments(library),true);
    }
    public void rescanFolder(Folder folder){
        scan(List.of(new MusicFiles.Request(Path.of(folder.path),folder.playlistId,folder.recursive)),false,library.profiles.activeId,null,true);
    }
    public void importPaths(List<Path> paths,String profileId,String playlist,boolean recursive) {
        if(scanning||pendingRepair!=null){status="Finish the current scan or repair first.";return;}
        if(!library.profiles.exists(profileId)){status="Import cancelled: source profile was deleted.";return;}
        for (Path path : paths) if (Files.isDirectory(path)) {
            String normalized = path.toAbsolutePath().normalize().toString();
            if (library.folders.stream().noneMatch(f -> f.path.equals(normalized))) {
                Folder folder = new Folder(normalized,"library"); folder.recursive = recursive; library.folders.add(folder);
                if(profileId.equals(library.profiles.activeId))folder.playlistId=playlist;
                else for(var p:library.profiles.inactive)if(p.id.equals(profileId)){p.folderDestinations.put(normalized,playlist);break;}
            }
        }
        save(); scan(paths.stream().map(p -> new MusicFiles.Request(p, playlist, recursive)).toList(),false,profileId,null,false);
    }
    public void importPlaylists(List<Path> paths,String profileId,String destination){
        if(library.profiles.playlists(library,profileId).stream().noneMatch(p->p.id.equals(destination))){status="Choose an existing destination playlist first.";return;}
        scan(paths.stream().map(p->new MusicFiles.Request(p,destination,false)).toList(),true,profileId,null,false);
    }
    private void scan(List<MusicFiles.Request> requests,boolean playlistImport,String scanProfile,FolderAssignments folders,boolean checkSources) {
        if(!enabled())return;
        if(scanning||pendingRepair!=null){status="Finish the current scan or repair first.";return;}
        if(requests.isEmpty()&&!checkSources){status="No files selected.";return;}
        scanProgress = MusicFiles.ScanProgress.starting();
        String destination=playlistImport?library.profiles.playlists(library,scanProfile).stream()
            .filter(p->p.id.equals(requests.get(0).playlistId())).findFirst().orElseThrow().name:"";
        var sources=LibraryRepair.capture(library);
        scanning = true; status = playlistImport?"Importing playlists into "+destination+"...":"Scanning music folders...";
        // Publish a single latest snapshot; a large scan must not queue thousands of client tasks.
        CompletableFuture.supplyAsync(() -> {
            var result=MusicFiles.scan(requests,sources.tracks(),progress->scanProgress=progress);
            LibraryHealth.Report health=null;
            if(checkSources){
                var tracks=new LinkedHashMap<String,Track>();for(var t:sources.tracks())tracks.put(t.id,t);
                for(var found:result.found())tracks.put(found.track().id,found.track());
                health=LibraryHealth.check(new LibraryRepair.Sources(new ArrayList<>(tracks.values()),sources.folders()),result.errors(),progress->scanProgress=progress,()->false);
            }
            return new ScanResult(result,health);
        },scanner).whenComplete((outcome, failure) -> mc.execute(() -> {
            try {
                if (failure != null) {
                    status=repairStatus="Scan failed: " + failure.getMessage(); Afmr.LOG.error(status, failure);
                    if(playlistImport)lastPlaylistImport=new PlaylistImportReport(destination,0,0,0,1,0,0,List.of(),List.of(status));
                    return;
                }
                var result=outcome.scan();
                Map<String, Track> byId = new HashMap<>(); library.tracks.forEach(t -> byId.put(t.id, t));
                int added = 0,assigned=0,existingAssignments=0;
                for (var found : result.found()) {
                    Track existing = byId.get(found.track().id);
                    if (existing == null) { library.tracks.add(found.track()); byId.put(found.track().id, found.track()); added++; }
                    else {
                        var imported=found.track();existing.title=imported.title;existing.artist=imported.artist;existing.album=imported.album;existing.durationSeconds=imported.durationSeconds;
                        existing.albumArtist=imported.albumArtist;existing.trackNumber=imported.trackNumber;existing.discNumber=imported.discNumber;
                        existing.genre=imported.genre;existing.date=imported.date;existing.replayGain=imported.replayGain;
                    }
                    Playlist target=null;
                    for(var p:library.profiles.playlists(library,scanProfile))if(p.id.equals(found.playlistId())){target=p;break;}
                    if(target!=null){if(target.tracks.containsKey(found.track().id))existingAssignments++;else assigned++;}
                    if(folders!=null)folders.assign(library,found.track().id,found.sourcePath());
                    else library.assignImported(scanProfile,found.playlistId(),found.track().id);
                }
                if(playlistImport){
                    lastPlaylistImport=new PlaylistImportReport(destination,assigned,existingAssignments,added,result.skipped(),result.duplicates(),result.reused(),result.playlists(),result.errors());
                    status="Imported into "+destination+": "+assigned+" added, "+existingAssignments+" already assigned, "+result.skipped()+" skipped.";
                }else status = "Scan complete: " + added + " new tracks, " + result.skipped() + " issues.";
                if(outcome.health()!=null){
                    sourceHealth=outcome.health();repairStatus=status;
                    if(!sourceHealth.issues().isEmpty()){
                        status=repairStatus="Scan error: "+sourceHealth.issues().size()+" missing/unavailable sources. Open Library > Repair / missing.";
                        SystemToast.addOrUpdate(mc.getToasts(),MISSING_SOURCES,Component.literal("AFMR: missing music sources"),
                            Component.literal(sourceHealth.issues().size()+" missing/unavailable. Open AFMR > Repair / missing."));
                    }
                }
                result.errors().forEach(message -> Afmr.LOG.warn("AFMR scan: {}", message));
                library.rebuildTrackIndex(); catalogRevision++;
                engine.retryFailed(); save();
            } finally { scanning = false; }
        }));
    }
    public void checkLibrary(){
        if(!enabled()||scanning||pendingRepair!=null){repairStatus="Finish the current scan or repair first.";return;}
        var sources=LibraryRepair.capture(library);scanning=true;scanProgress=MusicFiles.ScanProgress.starting();
        repairStatus=status="Checking library sources...";
        var cancellation=new AtomicBoolean();repairCancellation=cancellation;
        CompletableFuture.supplyAsync(()->LibraryHealth.check(sources,List.of(),progress->scanProgress=progress,cancellation::get),scanner)
            .whenComplete((report,failure)->mc.execute(()->{
                if(repairCancellation!=cancellation)return;
                repairCancellation=null;scanning=false;
                if(closed)return;
                if(cancellation.get()){repairStatus=status="Repair search cancelled; saved paths unchanged.";return;}
                if(failure!=null){repairStatus=status="Library check failed: "+failure.getMessage();return;}
                sourceHealth=report;repairStatus=status=report.issues().isEmpty()?"All library sources are available.":report.issues().size()+" missing/unavailable sources. Select one to locate it.";
            }));
    }
    public void prepareRepair(LibraryRepair.Request request){
        if(!enabled()||scanning||pendingRepair!=null){repairStatus="Finish the current scan or repair first.";return;}
        var sources=LibraryRepair.capture(library);scanning=true;scanProgress=MusicFiles.ScanProgress.starting();
        repairStatus=status="Preparing repair preview...";
        var cancellation=new AtomicBoolean();repairCancellation=cancellation;
        CompletableFuture.supplyAsync(()->{
            try{return LibraryRepair.plan(request,sources,progress->scanProgress=progress,cancellation::get);}
            catch(Exception e){throw new CompletionException(e);}
        },scanner).whenComplete((plan,failure)->mc.execute(()->{
            if(repairCancellation!=cancellation)return;
            repairCancellation=null;scanning=false;
            if(closed)return;
            if(cancellation.get()){repairStatus=status="Repair search cancelled; saved paths unchanged.";return;}
            if(failure!=null){
                Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();
                repairStatus=status="Repair could not be prepared: "+cause.getMessage();return;
            }
            pendingRepair=plan;repairStatus=status="Repair ready: "+plan.found()+" located, "+plan.missing()+" still missing. Review before applying.";
        }));
    }
    public void cancelRepair(LibraryRepair.Plan plan){if(pendingRepair==plan){pendingRepair=null;repairStatus="Repair cancelled; saved paths unchanged.";}}
    public boolean applyRepair(LibraryRepair.Plan plan){
        if(!enabled()||scanning||pendingRepair!=plan)return false;
        try{
            var current=engine.current();
            boolean affected=current!=null&&(plan.moves().stream().anyMatch(m->m.id().equals(current.id))||plan.merges().stream().anyMatch(m->m.removed().equals(current.id)));
            var remapped=LibraryRepair.apply(library,plan);
            if(affected){engine.skip(clock,library);overlay.clear();}
            engine.queue().relink(remapped);engine.history().relink(remapped);saveHistory();
            engine.retryFailed();durationCheck=null;catalogRevision++;save();pendingRepair=null;
            checkLibrary();return true;
        }catch(Exception e){repairStatus=status="Repair not applied: "+e.getMessage();return false;}
    }
    public ConfigBackup.Data pendingBackup(){return pendingBackup;}
    public boolean backupQueued(){return backupQueued;}
    public boolean backupBusy(){return backupBusy;}
    public boolean backupAvailable(){return enabled()&&!scanning&&pendingRepair==null&&pendingPreset==null&&!menuBackground.busy()&&!widgetBackground.busy()&&!fonts.busy()&&!themes.busy()
        &&(loudnessJob==null||loudnessJob.report()!=null);}
    public void exportBackup(Path path){
        if(!backupAvailable()){message("Finish the current scan, image change or import first.");return;}
        if(!ConfigBackup.supported(path)){message("Choose a filename ending in .afmrb.");return;}
        Library snapshot=library.snapshot();var history=engine.history().snapshot();var keys=Afmr.backupKeys();
        backupBusy=scanning=true;scanProgress=MusicFiles.ScanProgress.starting();status="Exporting full backup...";
        CompletableFuture.runAsync(()->{
            try{ConfigBackup.write(path,ConfigBackup.capture(snapshot,history,keys,configDirectory));}
            catch(Exception e){throw new CompletionException(e);}
        },scanner).whenComplete((nothing,failure)->mc.execute(()->{
            backupBusy=scanning=false;if(closed)return;
            status=failure==null?"Full backup exported: "+path:"Backup export failed: "+backupFailure(failure);
        }));
    }
    public void prepareBackupImport(Path path){
        if(!backupAvailable()||pendingBackup!=null){message("Finish the current scan or import first.");return;}
        backupBusy=scanning=true;scanProgress=MusicFiles.ScanProgress.starting();status="Reading full backup...";
        CompletableFuture.supplyAsync(()->{
            try{return ConfigBackup.read(path);}catch(Exception e){throw new CompletionException(e);}
        },scanner).whenComplete((data,failure)->mc.execute(()->{
            backupBusy=scanning=false;if(closed)return;
            if(failure!=null){status="Backup import failed: "+backupFailure(failure);return;}
            pendingBackup=data;status="Full backup ready for review.";
        }));
    }
    public void cancelBackupImport(ConfigBackup.Data data){if(pendingBackup==data){pendingBackup=null;status="Backup import cancelled.";}}
    public void applyBackupImport(ConfigBackup.Data data){
        if(!backupAvailable()||pendingBackup!=data)return;
        backupBusy=scanning=true;scanProgress=MusicFiles.ScanProgress.starting();status="Preparing full restore...";
        CompletableFuture.runAsync(()->{
            try{ConfigBackup.stage(configDirectory,data);}catch(Exception e){throw new CompletionException(e);}
        },scanner).whenComplete((nothing,failure)->mc.execute(()->{
            backupBusy=scanning=false;if(closed)return;
            if(failure==null){pendingBackup=null;backupQueued=true;status="Backup ready. Restart Minecraft to apply.";}
            else status="Backup restore not queued: "+backupFailure(failure);
        }));
    }
    private static String backupFailure(Throwable failure){while(failure.getCause()!=null)failure=failure.getCause();return failure.getMessage();}
    public void exportPresets(Path path,PlaylistPresets.FileData snapshot){
        if(!enabled()||scanning||pendingRepair!=null){message("Finish the current scan or repair first.");return;}
        if(!PlaylistPresets.supported(path)){message("Choose a filename ending in .afmrp, then export again.");return;}
        Path destination=path;
        scanning=true;scanProgress=new MusicFiles.ScanProgress(MusicFiles.ScanStage.WRITING_PRESET,1,0,destination.getFileName().toString());
        status="Exporting playlist setup...";
        CompletableFuture.runAsync(()->{
            try{
                PresetSongs.identifyExport(snapshot,progress->scanProgress=progress,()->false);
                PlaylistPresets.write(destination,snapshot);
            }catch(Exception e){throw new CompletionException(e);}
        },scanner).whenComplete((nothing,failure)->mc.execute(()->{
            scanning=false;
            if(failure==null){
                for(var song:snapshot.songs)if(!song.vanilla&&!song.sha256.isEmpty()){
                    var track=library.track(song.sourceId);
                    if(track!=null&&track.path.equals(song.sourcePath))track.content=new SongIdentity(song.bytes,song.sha256);
                }
                if(!snapshot.songs.isEmpty())save();
                status="Exported "+snapshot.playlists.size()+" playlists to "+destination;
            }
            else{Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();status="Export failed: "+cause.getMessage();}
        }));
    }
    public void preparePresetImport(Path path,String profileId,String anchor){
        if(!enabled()||scanning||pendingRepair!=null||pendingPreset!=null){message("Finish the current scan, repair or playlist import first.");return;}
        scanning=true;scanProgress=new MusicFiles.ScanProgress(MusicFiles.ScanStage.READING_PRESET,1,0,path.getFileName().toString());
        status="Reading playlist setup...";
        CompletableFuture.supplyAsync(()->{
            try{return PlaylistPresets.read(path);}catch(Exception e){throw new CompletionException(e);}
        },scanner).whenComplete((data,failure)->mc.execute(()->{
            scanning=false;if(!enabled())return;
            if(failure!=null){Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();status="Playlist import failed: "+cause.getMessage();return;}
            pendingPreset=new PresetImport(data,profileId,anchor,path.getFileName().toString());
            presetSongPlan=null;presetFolders=PresetSongs.matchFolders(data,library.folders.stream().map(f->f.path).toList());
            presetFiles=Map.of();presetSearchFolders=List.of();presetSearchLinked=false;
            presetMissingRoot=mc.gameDirectory.toPath().resolve("config/afmr/missing-imports").resolve(UUID.randomUUID().toString());
            status="Ready to import "+data.playlists.size()+" new playlists. Review before adding.";
        }));
    }
    public void cancelPresetImport(PresetImport pending){if(pendingPreset==pending){
        if(presetCancellation!=null)presetCancellation.set(true);
        pendingPreset=null;presetSongPlan=null;presetFolders=Map.of();presetFiles=Map.of();presetSearchFolders=List.of();presetSearchLinked=false;status="Playlist import cancelled.";
    }}
    public void locatePresetFile(String id,Path path){
        if(pendingPreset==null||scanning||pendingPreset.data().songs.stream().noneMatch(song->song.id.equals(id)&&!song.vanilla))return;
        var choices=new HashMap<>(presetFiles);choices.put(id,path);presetFiles=Map.copyOf(choices);preparePresetSongs(pendingPreset);
    }
    public void searchPresetFolder(Path path){
        if(pendingPreset==null||scanning)return;
        if(pendingPreset.data().songs.stream().noneMatch(song->!song.sha256.isEmpty())){status="For content matching, ask the sender to export this song list again. You can still choose individual files or its original folder layout.";return;}
        var folders=new ArrayList<>(presetSearchFolders);folders.add(path);presetSearchFolders=List.copyOf(folders);preparePresetSongs(pendingPreset);
    }
    public void searchPresetLibraries(PresetImport pending){
        if(pendingPreset!=pending||scanning)return;
        if(pending.data().songs.stream().noneMatch(song->!song.sha256.isEmpty())){status="For content matching, ask the sender to export this song list again. You can still choose individual files or its original folder layout.";return;}
        presetSearchLinked=true;preparePresetSongs(pending);
    }
    public void locatePresetFolder(String id,Path path){
        if(pendingPreset==null||scanning||pendingPreset.data().folders.stream().noneMatch(f->f.id.equals(id)))return;
        var choices=new HashMap<>(presetFolders);choices.put(id,path);presetFolders=Map.copyOf(choices);
        preparePresetSongs(pendingPreset);
    }
    public void preparePresetSongs(PresetImport pending){
        if(!enabled()||scanning||pendingPreset!=pending)return;
        var catalog=library.tracks.stream().map(Library::copyTrack).toList();
        var choices=Map.copyOf(presetFolders);Path missingRoot=presetMissingRoot;
        var fileChoices=Map.copyOf(presetFiles);var searchFolders=new ArrayList<>(presetSearchFolders);boolean searchCatalog=presetSearchLinked;
        if(presetSearchLinked)for(var folder:library.folders)searchFolders.add(Path.of(folder.path));
        var cancellation=new AtomicBoolean();presetCancellation=cancellation;
        scanning=true;scanProgress=MusicFiles.ScanProgress.starting();status="Finding playlist songs...";
        CompletableFuture.supplyAsync(()->{
            try{return PresetSongs.plan(pending.data(),catalog,choices,missingRoot,progress->scanProgress=progress,searchFolders,fileChoices,cancellation::get,searchCatalog);}
            catch(Exception e){throw new CompletionException(e);}
        },scanner).whenComplete((plan,failure)->mc.execute(()->{
            if(presetCancellation!=cancellation)return;
            presetCancellation=null;scanning=false;if(!enabled()||pendingPreset!=pending)return;
            if(cancellation.get()){status="Song search cancelled; previous matches kept.";return;}
            if(failure!=null){Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();status="Song matching failed: "+cause.getMessage();return;}
            presetSongPlan=plan;status=plan.found()+" songs matched ("+plan.verified()+" verified by contents), "+plan.missing()+" still missing, "+plan.unavailableResources()+" unavailable resource songs."
                +(plan.errors().isEmpty()?"":" Some chosen files or folders could not be matched/read; review the file list.");
        }));
    }
    public PlaylistPresets.ImportResult applyPresetImport(PresetImport pending,String profileId,String newProfileName,PlaylistPresets.Placement placement,boolean includeSongs){
        if(!enabled()||scanning||pendingPreset!=pending)return null;
        if(includeSongs&&presetSongPlan==null){status="Find the song files before applying the import.";return null;}
        try{
            var plan=includeSongs?presetSongPlan:null;
            var result=PlaylistPresets.apply(library,profileId,newProfileName,pending.anchor(),placement,pending.data(),inWorld()?worldKey():"",plan);
            pendingPreset=null;presetSongPlan=null;presetFolders=Map.of();presetFiles=Map.of();presetSearchFolders=List.of();presetSearchLinked=false;
            catalogRevision++;if(plan!=null)engine.retryFailed();save();
            status="Imported "+result.ids().size()+" playlists into "+library.profiles.entries().stream().filter(p->p.id().equals(result.profileId())).findFirst().orElseThrow().name()+(includeSongs?" with song assignments.":"; add your own songs.");
            if(plan!=null&&plan.missing()>0)checkLibrary();
            return result;
        }catch(Exception e){status="Playlist import not applied: "+e.getMessage();return null;}
    }
    public static void reportAudioFailure(dev.animal.afmr.audio.MusicSound owner, Exception error) {
        Afmr.LOG.error("Cannot decode AFMR stream",error);
        Minecraft.getInstance().execute(() -> {
            ClientRuntime runtime = get();
            Track current = runtime.engine.current();
            if (current != null && !owner.ended())
                runtime.engine.failed(current.id, "Could not play " + current.title + ": " + error.getMessage(), runtime.clock, runtime.library);
        });
    }
}

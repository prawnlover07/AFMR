package dev.animal.afmr.core;

import java.util.*;

/** Version-neutral, persisted user configuration. Paths always refer to the original files. */
public final class Library {
    public int schemaVersion = 2;
    public Settings settings = new Settings();
    public PlaylistProfiles profiles = new PlaylistProfiles();
    public List<Track> tracks = new ArrayList<>();
    public List<Folder> folders = new ArrayList<>();
    /** Earlier entries have higher priority. */
    public List<Playlist> playlists = new ArrayList<>();
    /** Older biome/dimension assignments are preserved, but no longer clutter the editor. */
    public List<Playlist> legacyPlaylists = new ArrayList<>();
    private transient Map<String, Track> tracksById;
    private transient List<Track> indexedTracks;
    private transient int indexedCount = -1;

    public static final class Home {
        public boolean configured;
        public String world = "";
        public String dimension = "minecraft:overworld";
        public double x, y, z;
        public double radius = 64;
        public boolean contains(Context c) {
            if (!configured || !world.equals(c.world()) || !dimension.equals(c.dimension())) return false;
            double dx=c.x()-x, dy=c.y()-y, dz=c.z()-z;
            return dx*dx+dy*dy+dz*dz <= radius*radius;
        }
    }

    public static final class Settings {
        public OverrideBinding[] overrideSlots=defaultOverrideSlots();
        public int songsBeforeSilence = 1;
        public double silenceMinSeconds = 600;
        public double silenceMaxSeconds = 1200;
        public double fadeInSeconds = 0,fadeOutSeconds = 0;
        public JukeboxMode jukeboxMode=JukeboxMode.OFF;
        public double bufferSeconds = 0;
        public boolean continuePlayingOnMatch = false;
        public boolean playImmediatelyOnJoin = false;
        public double combatQuietSeconds = 15;
        public boolean combatEnabled = true;
        public boolean combatThreatsOnly = true;
        public boolean combatOnAttack = true,combatOnDamage = true,preventMusicWhileDead = true;
        public DeathMusic combatDeathMusic = DeathMusic.FADE;
        public double combatDeathFadeSeconds = 1;
        public boolean rescanOnLaunch = false;
        public double menuBackgroundOpacity = 0.80;
        public Map<MenuColor,Integer> menuColors=MenuColor.defaults();
        public boolean menuImageEnabled = false;
        public String menuImageName = "";
        public double menuImageOpacity = 1;
        public MenuImageLayout.Fit menuImageFit = MenuImageLayout.Fit.FILL;
        public boolean menuBlur = true;
        public boolean nowPlaying = true;
        public NowPlayingStyle nowPlayingStyle = NowPlayingStyle.MINECRAFT;
        public WidgetCorner widgetCorner=WidgetCorner.DEFAULT;
        public double widgetScale = 1;
        public int widgetWidth = 260;
        public int widgetBackgroundColor = 0x000000;
        public double widgetBackgroundOpacity = 0.92;
        public boolean widgetArtwork = true, widgetTitle = true, widgetArtist = true;
        public boolean widgetAlbum = false, widgetDuration = false, widgetPlaylist = false;
        public boolean widgetScroll = false;
        public boolean widgetWrap = false;
        public boolean replayGainEnabled = true;
        public Set<SongColumn> songColumns=SongColumn.defaults();
        public double popupSeconds = 7, popupDelaySeconds = 0;
        /** Normal GAME/CREATIVE music delays in Minecraft 26.3, without AFMR's extra transitions. */
        public void restoreMinecraftPlaybackDefaults() {
            silenceMinSeconds=600;silenceMaxSeconds=1200;fadeInSeconds=fadeOutSeconds=0;bufferSeconds=0;songsBeforeSilence=1;
        }
        public void normalizePlayback(){
            songsBeforeSilence=Math.max(1,Math.min(1000,songsBeforeSilence));
            silenceMinSeconds=bounded(silenceMinSeconds,0,86400,600);
            silenceMaxSeconds=bounded(silenceMaxSeconds,silenceMinSeconds,86400,1200);
            fadeInSeconds=bounded(fadeInSeconds,0,60,0);fadeOutSeconds=bounded(fadeOutSeconds,0,60,0);
            if(jukeboxMode==null)jukeboxMode=JukeboxMode.OFF;
            bufferSeconds=bounded(bufferSeconds,0,60,0);
            combatQuietSeconds=bounded(combatQuietSeconds,1,600,15);
            combatDeathFadeSeconds=bounded(combatDeathFadeSeconds,0,60,1);
            if(combatDeathMusic==null)combatDeathMusic=DeathMusic.FADE;
        }
    }

    public enum NowPlayingStyle { MINECRAFT, HYBRID, AFMR }
    public enum JukeboxMode {
        OFF("off"),RESUME("resume song"),AFTER_SILENCE("new after silence"),IMMEDIATE("new immediately");
        private final String label;
        JukeboxMode(String label){this.label=label;}
        public String label(){return label;}
    }
    public enum WidgetCorner {
        DEFAULT("Default"),TOP_LEFT("Top left"),TOP_RIGHT("Top right"),BOTTOM_LEFT("Bottom left"),BOTTOM_RIGHT("Bottom right");
        private final String label;
        WidgetCorner(String label){this.label=label;}
        public String label(){return label;}
        public WidgetCorner resolved(NowPlayingStyle style){return this==DEFAULT?(style==NowPlayingStyle.AFMR?BOTTOM_LEFT:TOP_LEFT):this;}
        public boolean right(){return this==TOP_RIGHT||this==BOTTOM_RIGHT;}
        public boolean bottom(){return this==BOTTOM_LEFT||this==BOTTOM_RIGHT;}
    }
    public enum OverrideMode { ONE_SONG, UNTIL_RELEASED }
    public static final int OVERRIDE_SLOTS=8;
    public static final class OverrideBinding {
        public String playlistId="";
        public OverrideMode mode=OverrideMode.ONE_SONG;
    }
    private static OverrideBinding[] defaultOverrideSlots(){
        var slots=new OverrideBinding[OVERRIDE_SLOTS];for(int i=0;i<slots.length;i++)slots[i]=new OverrideBinding();
        slots[0].playlistId="combat";slots[0].mode=OverrideMode.UNTIL_RELEASED;return slots;
    }

    public static final class Folder {
        public String path = "";
        public boolean recursive = true;
        public String playlistId = "library";
        public Folder() {}
        public Folder(String path, String playlistId) { this.path = path; this.playlistId = playlistId; }
    }

    public static final class Track {
        public String id = "";
        public String path = "";
        public String title = "";
        public String artist = "";
        public String album = "";
        public String albumArtist="",trackNumber="",discNumber="",genre="",date="";
        public ReplayGainInfo replayGain;
        public boolean vanilla;
        public double durationSeconds;
        public TrackLoudness loudness;
        public Track() {}
        public Track(String id, String path, String title, boolean vanilla) {
            this.id = id; this.path = path; this.title = title; this.vanilla = vanilla;
        }
    }
    /** Owned metadata snapshot for a writer or scanner; never publish mutable live tracks to workers. */
    public static Track copyTrack(Track t){
        Track next=new Track(t.id,t.path,t.title,t.vanilla);
        next.artist=t.artist;next.album=t.album;next.durationSeconds=t.durationSeconds;next.loudness=t.loudness;
        next.albumArtist=t.albumArtist;next.trackNumber=t.trackNumber;next.discNumber=t.discNumber;
        next.genre=t.genre;next.date=t.date;next.replayGain=t.replayGain;return next;
    }

    public enum DeathMusic { FADE, CUT, KEEP_PLAYING }

    public static final class Playlist {
        public String id = UUID.randomUUID().toString();
        public String name = "New playlist";
        /** Controls automatic selection; explicit overrides and previews remain available. */
        public boolean enabled = true;
        public boolean manualOnly = false;
        /** On entry, let music already playing from another playlist finish. */
        public boolean finishCurrentSong = false;
        /** On exit, let a song started by this playlist finish. */
        public boolean finishCurrentSongOnExit = false;
        /** On automatic entry, skip buffer and silence while retaining fades and finish controls. */
        public boolean playOnTrigger = false;
        /** Override the global gap for this playlist, including initial automatic selection. */
        public boolean customSilence = false;
        public boolean customSongCount = false;
        public int songsBeforeSilence = 1;
        public double silenceMinSeconds = 600, silenceMaxSeconds = 1200;
        public boolean includeVanilla = true;
        /** Linear playlist gain; independent of the fade envelope and ReplayGain. */
        public double volume = 1;
        public PlaylistConditions conditions = new PlaylistConditions();
        /** Explicit entries take precedence over automatic vanilla membership. */
        public Map<String, Boolean> tracks = new LinkedHashMap<>();
        public Playlist() {}
        public Playlist(String id,String name){this.id=id;this.name=name;}
        public boolean matches(Context c,Settings settings){return enabled&&conditions.matches(c,settings);}
        public boolean combatDriven(Context c,Settings settings){return conditions.combatDriven(c,settings);}
        public void autoplayHere(Context context){
            if(context.world().isBlank()||context.dimension().isBlank()||!Double.isFinite(context.x())||!Double.isFinite(context.y())||!Double.isFinite(context.z()))
                throw new IllegalArgumentException("Join a world before setting an autoplay area.");
            var next=new PlaylistConditions();next.areaEnabled=true;
            var area=next.area;area.configured=true;area.world=context.world();area.dimension=context.dimension();
            area.x=context.x();area.y=context.y();area.z=context.z();area.radius=64;
            conditions=next;enabled=true;manualOnly=false;
        }
    }

    public enum Precipitation { NONE, RAIN, SNOW }
    public enum GameMode {
        SURVIVAL("Survival"),CREATIVE("Creative"),ADVENTURE("Adventure"),SPECTATOR("Spectator");
        private final String label;
        GameMode(String label){this.label=label;}
        public String label(){return label;}
    }

    public record Context(String dimension, String biome, boolean combat, boolean underwater, int timeOfDay,
                          boolean covered, int lightLevel,
                          String world, double x, double y, double z,double coverDepth,double biomeTemperature,boolean dead,
                          StructureContext structures,boolean raining,boolean thundering,Precipitation precipitation,
                          double healthHearts,boolean playerCombat,String mainHandItem,String offHandItem,GameMode gameMode,boolean jukeboxPlaying,NearbyPlayers nearbyPlayers) {
        public boolean day(){return Math.floorMod(timeOfDay,24000)<12000;}
    }

    public Playlist playlist(String id) {
        for (int i = 0; i < playlists.size(); i++) if (playlists.get(i).id.equals(id)) return playlists.get(i);
        return null;
    }

    public Playlist createPlaylist(String name){
        String clean=name.replaceAll("\\p{Cc}","").strip();
        if(clean.isBlank()||clean.length()>80)throw new IllegalArgumentException("Use a playlist name of 1-80 characters.");
        Playlist p=new Playlist("custom:"+UUID.randomUUID(),clean);p.includeVanilla=false;
        // Start above broad day/night fallbacks, retaining the existing special-playlist order.
        int index=playlists.size();for(int i=0;i<playlists.size();i++)if(playlists.get(i).id.equals("day_outside")||playlists.get(i).id.equals("night_outside")){index=i;break;}
        playlists.add(index,p);return p;
    }
    public boolean movePlaylist(String id,int index){
        Playlist p=playlist(id);if(p==null)return false;int old=playlists.indexOf(p),next=Math.clamp(index,0,playlists.size()-1);
        if(old==next)return false;playlists.remove(old);playlists.add(next,p);return true;
    }
    public boolean removePlaylist(String id){
        Playlist p=playlist(id);if(p==null)return false;
        playlists.remove(p);for(Folder f:folders)if(id.equals(f.playlistId))f.playlistId="library";return true;
    }

    /** Called at library changes; never rebuild the index during steady gameplay or drawing. */
    public void rebuildTrackIndex() {
        tracksById = HashMap.newHashMap(tracks.size());
        for (int i = 0; i < tracks.size(); i++) { Track track = tracks.get(i); tracksById.put(track.id, track); }
        indexedTracks = tracks; indexedCount = tracks.size();
    }
    private void ensureTrackIndex() {
        if (indexedTracks != tracks || indexedCount != tracks.size()) rebuildTrackIndex();
    }
    public Track track(String id) { ensureTrackIndex(); return tracksById.get(id); }

    /** Enumerates only assignments and eligible vanilla IDs, including disabled rows for the editor. */
    public List<Track> pool(Playlist p, Set<String> applicableVanilla, boolean includeDisabled) {
        if (p == null) return List.of();
        ensureTrackIndex();
        List<Track> result = new ArrayList<>(p.tracks.size() + (p.includeVanilla ? applicableVanilla.size() : 0));
        for (var entry : p.tracks.entrySet()) if (includeDisabled || Boolean.TRUE.equals(entry.getValue())) {
            Track track = tracksById.get(entry.getKey()); if (track != null) result.add(track);
        }
        if (p.includeVanilla) for (String id : applicableVanilla) if (!p.tracks.containsKey(id)) {
            Track track = tracksById.get(id); if (track != null && track.vanilla) result.add(track);
        }
        return result;
    }

    public Playlist select(Context c) {
        for(var p:playlists)if(!p.manualOnly&&p.matches(c,settings))return p;
        return null;
    }
    public Playlist selectPlayable(Context c, Set<String> vanilla, Set<String> failed) {
        ensureTrackIndex();
        for (int i = 0; i < playlists.size(); i++) {
            Playlist p = playlists.get(i);
            if (p.manualOnly||!p.matches(c, settings)) continue;
            if(hasPlayable(p,vanilla,failed))return p;
        }
        return null;
    }
    public boolean hasPlayable(Playlist p,Set<String> vanilla,Set<String> failed){
        ensureTrackIndex();
        for(var entry:p.tracks.entrySet())if(Boolean.TRUE.equals(entry.getValue())&&!failed.contains(entry.getKey())&&tracksById.containsKey(entry.getKey()))return true;
        if(p.includeVanilla)for(String id:vanilla)if(!p.tracks.containsKey(id)&&!failed.contains(id)){
            Track t=tracksById.get(id);if(t!=null&&t.vanilla)return true;
        }
        return false;
    }
    /** Indexed membership check; explicit exclusions override automatic vanilla inclusion. */
    public boolean isPlayable(Playlist p,String trackId,Set<String> vanilla,Set<String> failed){
        if(p==null||failed.contains(trackId))return false;
        Track track=track(trackId);if(track==null)return false;
        if(p.tracks.containsKey(trackId))return Boolean.TRUE.equals(p.tracks.get(trackId));
        return p.includeVanilla&&track.vanilla&&vanilla.contains(trackId);
    }
    public String playbackUnavailable(Context c, Set<String> vanilla) {
        if (selectPlayable(c, vanilla, Set.of()) != null) return "";
        Playlist matched = select(c);
        if (matched != null) return "No enabled songs for " + matched.name;
        for(var p:playlists)if(p.conditions.matches(c,settings)){
            if(!p.enabled)return p.name+" playlist is off";
            if(p.manualOnly)return p.name+" / manual only";
        }
        return "No playlist matches this environment";
    }

    public static Library defaults(){
        Library library=new Library();library.playlists.addAll(PlaylistTemplates.create());return library;
    }

    public Playlist clonePlaylist(String sourceId,String name){
        Playlist source=playlist(sourceId);if(source==null)throw new IllegalArgumentException("Playlist no longer exists.");
        String clean=name.replaceAll("\\p{Cc}","").strip();
        if(clean.isBlank()||clean.length()>80)throw new IllegalArgumentException("Use a playlist name of 1-80 characters.");
        Playlist copy=copyPlaylist(source,true);copy.id="custom:"+UUID.randomUUID();copy.name=clean;
        playlists.add(playlists.indexOf(source)+1,copy);return copy;
    }

    public void assignImported(String profileId,String destination,String trackId) {
        List<Playlist> targets=profiles.playlists(this,profileId);
        if (destination.equals("ambient")) {
            for(var p:targets)if(p.id.equals("day_outside")||p.id.equals("night_outside")||p.id.equals("underground"))p.tracks.putIfAbsent(trackId,true);
        } else {
            for(var p:targets)if(p.id.equals(destination)){p.tracks.putIfAbsent(trackId,true);break;}
        }
    }

    /** The writer owns this copy; later edits and scan results cannot change an in-flight save. */
    public Library snapshot() {
        Library copy = new Library(); copy.schemaVersion = schemaVersion;
        Settings s = copy.settings;
        s.songsBeforeSilence=settings.songsBeforeSilence;s.popupDelaySeconds=settings.popupDelaySeconds;
        s.silenceMinSeconds=settings.silenceMinSeconds;s.silenceMaxSeconds=settings.silenceMaxSeconds;
        s.fadeInSeconds=settings.fadeInSeconds;s.fadeOutSeconds=settings.fadeOutSeconds;s.bufferSeconds=settings.bufferSeconds;s.combatQuietSeconds=settings.combatQuietSeconds;
        s.jukeboxMode=settings.jukeboxMode;
        s.continuePlayingOnMatch=settings.continuePlayingOnMatch;
        s.playImmediatelyOnJoin=settings.playImmediatelyOnJoin;
        s.combatEnabled=settings.combatEnabled;s.combatThreatsOnly=settings.combatThreatsOnly;s.rescanOnLaunch=settings.rescanOnLaunch;
        s.combatOnAttack=settings.combatOnAttack;s.combatOnDamage=settings.combatOnDamage;s.preventMusicWhileDead=settings.preventMusicWhileDead;
        s.combatDeathMusic=settings.combatDeathMusic;s.combatDeathFadeSeconds=settings.combatDeathFadeSeconds;
        s.nowPlaying=settings.nowPlaying;s.nowPlayingStyle=settings.nowPlayingStyle;s.popupSeconds=settings.popupSeconds;
        s.widgetCorner=settings.widgetCorner;
        s.menuColors=MenuColor.normalize(settings.menuColors);
        s.menuImageEnabled=settings.menuImageEnabled;s.menuImageName=settings.menuImageName;
        s.menuImageOpacity=settings.menuImageOpacity;s.menuImageFit=settings.menuImageFit;
        s.menuBackgroundOpacity=settings.menuBackgroundOpacity;s.menuBlur=settings.menuBlur;
        s.widgetScale=settings.widgetScale;s.widgetWidth=settings.widgetWidth;
        s.widgetBackgroundColor=settings.widgetBackgroundColor;s.widgetBackgroundOpacity=settings.widgetBackgroundOpacity;
        s.widgetArtwork=settings.widgetArtwork;s.widgetTitle=settings.widgetTitle;
        s.widgetArtist=settings.widgetArtist;s.widgetAlbum=settings.widgetAlbum;s.widgetDuration=settings.widgetDuration;s.widgetPlaylist=settings.widgetPlaylist;
        s.widgetScroll=settings.widgetScroll;
        s.widgetWrap=settings.widgetWrap;
        s.replayGainEnabled=settings.replayGainEnabled;
        s.songColumns=SongColumn.normalize(settings.songColumns);
        for(int i=0;i<OVERRIDE_SLOTS;i++){s.overrideSlots[i].playlistId=settings.overrideSlots[i].playlistId;s.overrideSlots[i].mode=settings.overrideSlots[i].mode;}
        copy.tracks = new ArrayList<>(tracks.size());
        for (int i=0;i<tracks.size();i++) {
            copy.tracks.add(copyTrack(tracks.get(i)));
        }
        for (Folder f:folders) { Folder next=new Folder(f.path,f.playlistId);next.recursive=f.recursive;copy.folders.add(next); }
        for (Playlist p:playlists) copy.playlists.add(copyPlaylist(p,true));
        for (Playlist p:legacyPlaylists) copy.legacyPlaylists.add(copyPlaylist(p,true));
        copy.profiles=profiles.copy();
        return copy;
    }
    public static Playlist copyPlaylist(Playlist p,boolean includeTracks) {
        Playlist next=new Playlist(p.id,p.name);next.enabled=p.enabled;
        next.manualOnly=p.manualOnly;
        next.finishCurrentSong=p.finishCurrentSong;next.finishCurrentSongOnExit=p.finishCurrentSongOnExit;
        next.playOnTrigger=p.playOnTrigger;
        next.customSongCount=p.customSongCount;next.songsBeforeSilence=p.songsBeforeSilence;
        next.customSilence=p.customSilence;next.silenceMinSeconds=p.silenceMinSeconds;next.silenceMaxSeconds=p.silenceMaxSeconds;
        next.volume=p.volume;
        next.includeVanilla=p.includeVanilla;next.conditions=p.conditions.copy();
        if(includeTracks)next.tracks.putAll(p.tracks);return next;
    }

    public void normalize() {
        if(settings.overrideSlots==null)settings.overrideSlots=defaultOverrideSlots();
        else if(settings.overrideSlots.length!=OVERRIDE_SLOTS)settings.overrideSlots=Arrays.copyOf(settings.overrideSlots,OVERRIDE_SLOTS);
        for(int i=0;i<OVERRIDE_SLOTS;i++){
            var b=settings.overrideSlots[i];if(b==null)b=settings.overrideSlots[i]=new OverrideBinding();
            if(b.playlistId==null)b.playlistId="";if(b.mode==null)b.mode=OverrideMode.ONE_SONG;
        }
        settings.normalizePlayback();
        settings.popupDelaySeconds=bounded(settings.popupDelaySeconds,0,60,0);
        settings.popupSeconds = bounded(settings.popupSeconds, 1, 60, 7);
        settings.widgetScale = bounded(settings.widgetScale, 0.5, 2, 1);
        settings.widgetWidth = Math.clamp(settings.widgetWidth, 140, 600);
        settings.widgetBackgroundColor = Math.clamp(settings.widgetBackgroundColor, 0, 0xFFFFFF);
        settings.widgetBackgroundOpacity = bounded(settings.widgetBackgroundOpacity, 0, 1, 0.92);
        settings.menuBackgroundOpacity = bounded(settings.menuBackgroundOpacity, 0, 1, 0.80);
        settings.menuColors=MenuColor.normalize(settings.menuColors);
        settings.menuImageOpacity = bounded(settings.menuImageOpacity, 0, 1, 1);
        if(settings.menuImageName==null)settings.menuImageName="";
        if(settings.menuImageFit==null)settings.menuImageFit=MenuImageLayout.Fit.FILL;
        if(settings.nowPlayingStyle==null)settings.nowPlayingStyle=NowPlayingStyle.MINECRAFT;
        if(settings.widgetCorner==null)settings.widgetCorner=WidgetCorner.DEFAULT;
        settings.songColumns=SongColumn.normalize(settings.songColumns);
        for(Track t:tracks){
            if(t.albumArtist==null)t.albumArtist="";if(t.trackNumber==null)t.trackNumber="";if(t.discNumber==null)t.discNumber="";
            if(t.genre==null)t.genre="";if(t.date==null)t.date="";
        }
        for(Playlist p:playlists)normalizePlaylist(p);
        for(Playlist p:legacyPlaylists)normalizePlaylist(p);
        profiles.normalize();
        for(Track t:tracks)if(t.loudness!=null&&!t.loudness.valid())t.loudness=null;
    }
    static void normalizePlaylist(Playlist p){
        if(p==null||p.id==null||p.name==null||p.tracks==null)throw new IllegalArgumentException("Invalid playlist");
        if(p.conditions==null)p.conditions=new PlaylistConditions();p.conditions.normalize();
        p.songsBeforeSilence=Math.max(1,Math.min(1000,p.songsBeforeSilence));
        p.volume=bounded(p.volume,0,1,1);
        p.silenceMinSeconds=bounded(p.silenceMinSeconds,0,86400,600);
        p.silenceMaxSeconds=bounded(p.silenceMaxSeconds,p.silenceMinSeconds,86400,1200);
    }
    private static double bounded(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : Math.clamp(fallback, min, max);
    }
}

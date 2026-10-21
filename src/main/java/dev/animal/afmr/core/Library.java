package dev.animal.afmr.core;

import java.util.*;

/** Version-neutral, persisted user configuration. Paths always refer to the original files. */
public final class Library {
    public int schemaVersion = 2;
    public Settings settings = new Settings();
    public List<Track> tracks = new ArrayList<>();
    public List<Folder> folders = new ArrayList<>();
    /** Earlier entries have higher priority. */
    public List<Playlist> playlists = new ArrayList<>();
    /** Older biome/dimension assignments are preserved, but no longer clutter the editor. */
    public List<Playlist> legacyPlaylists = new ArrayList<>();
    public Home home = new Home();
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
        public double silenceMinSeconds = 600;
        public double silenceMaxSeconds = 1200;
        public double fadeSeconds = 0;
        public double bufferSeconds = 0;
        public double combatQuietSeconds = 15;
        public boolean combatEnabled = true;
        public boolean combatOnAttack = true,combatOnDamage = true,preventMusicWhileDead = true;
        public DeathMusic combatDeathMusic = DeathMusic.FADE;
        public double combatDeathFadeSeconds = 1;
        public boolean rescanOnLaunch = false;
        public boolean nowPlaying = true;
        public boolean replayGainEnabled = true;
        public double popupSeconds = 7;
        public double undergroundDepth = 5;
        public int undergroundMaxY = 64;
        public int darkMaxLight = 4;
        public boolean darkInNether = false;
        public boolean darkInEnd = false;

        /** Normal GAME/CREATIVE music delays in Minecraft 26.3, without AFMR's extra transitions. */
        public void restoreMinecraftPlaybackDefaults() {
            silenceMinSeconds=600;silenceMaxSeconds=1200;fadeSeconds=0;bufferSeconds=0;
        }
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
        public boolean vanilla;
        public double durationSeconds;
        public Track() {}
        public Track(String id, String path, String title, boolean vanilla) {
            this.id = id; this.path = path; this.title = title; this.vanilla = vanilla;
        }
    }

    public enum Rule { GENERAL, DIMENSION, BIOME, COMBAT, UNDERWATER, DAY, NIGHT, DAY_OUTSIDE, NIGHT_OUTSIDE, UNDERGROUND, HOME, DARK_EXPLORATION, CUSTOM }
    public enum DeathMusic { FADE, CUT, KEEP_PLAYING }

    public static final class Playlist {
        public String id = UUID.randomUUID().toString();
        public String name = "New playlist";
        public Rule rule = Rule.GENERAL;
        public String match = "";
        /** Controls automatic selection; explicit overrides and previews remain available. */
        public boolean enabled = true;
        public boolean manualOnly = false;
        /** On entry, let music already playing from another playlist finish. */
        public boolean finishCurrentSong = false;
        /** On exit, let a song started by this playlist finish. */
        public boolean finishCurrentSongOnExit = false;
        public boolean includeVanilla = true;
        public PlaylistConditions conditions = new PlaylistConditions();
        /** Explicit entries take precedence over automatic vanilla membership. */
        public Map<String, Boolean> tracks = new LinkedHashMap<>();
        public Playlist() {}
        public Playlist(String id, String name, Rule rule, String match) {
            this.id = id; this.name = name; this.rule = rule; this.match = match;
        }
        public boolean matches(Context c, Settings settings) {
            if (!enabled) return false;
            return switch (rule) {
                case GENERAL -> true;
                case DIMENSION -> match.equals(c.dimension());
                case BIOME -> match.equals(c.biome());
                case COMBAT -> settings.combatEnabled && c.combat();
                case UNDERWATER -> c.underwater();
                case DAY -> c.day();
                case NIGHT -> !c.day();
                case DAY_OUTSIDE -> c.day() && !c.underground();
                case NIGHT_OUTSIDE -> !c.day() && !c.underground();
                case UNDERGROUND -> c.underground();
                case HOME -> c.atHome();
                case DARK_EXPLORATION -> EnvironmentConditions.dark(c.dimension(),c.covered(),c.lightLevel(),settings);
                case CUSTOM -> conditions.matches(c,settings);
            };
        }
        public boolean custom(){return rule==Rule.CUSTOM;}
        public boolean combatDriven(){return rule==Rule.COMBAT||custom()&&conditions.combat==PlaylistConditions.State.YES;}
    }

    public record Context(String dimension, String biome, boolean combat, boolean underwater, int timeOfDay,
                          boolean underground, boolean atHome, boolean covered, int lightLevel,
                          String world, double x, double y, double z,double coverDepth,double biomeTemperature,boolean dead) {
        public boolean day(){return Math.floorMod(timeOfDay,24000)<12000;}
        public Context withHome(boolean value) { return new Context(dimension,biome,combat,underwater,timeOfDay,underground,value,covered,lightLevel,world,x,y,z,coverDepth,biomeTemperature,dead); }
    }

    public Playlist playlist(String id) {
        for (int i = 0; i < playlists.size(); i++) if (playlists.get(i).id.equals(id)) return playlists.get(i);
        return null;
    }

    public Playlist createPlaylist(String name){
        String clean=name.replaceAll("\\p{Cc}","").strip();
        if(clean.isBlank()||clean.length()>80)throw new IllegalArgumentException("Use a playlist name of 1-80 characters.");
        Playlist p=new Playlist("custom:"+UUID.randomUUID(),clean,Rule.CUSTOM,"");p.includeVanilla=false;
        // Start above broad day/night fallbacks, retaining the existing special-playlist order.
        int index=playlists.size();for(int i=0;i<playlists.size();i++)if(playlists.get(i).rule==Rule.DAY_OUTSIDE||playlists.get(i).rule==Rule.NIGHT_OUTSIDE){index=i;break;}
        playlists.add(index,p);return p;
    }
    public boolean movePlaylist(String id,int index){
        Playlist p=playlist(id);if(p==null)return false;int old=playlists.indexOf(p),next=Math.clamp(index,0,playlists.size()-1);
        if(old==next)return false;playlists.remove(old);playlists.add(next,p);return true;
    }
    public boolean removePlaylist(String id){
        Playlist p=playlist(id);if(p==null||!p.custom())return false;
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
        return playlists.stream().filter(p -> !p.manualOnly&&p.matches(c, settings))
            .filter(p -> !legacyRule(p.rule) || p.rule == Rule.GENERAL || !p.tracks.isEmpty()).findFirst().orElse(null);
    }
    public Playlist selectPlayable(Context c, Set<String> vanilla, Set<String> failed) {
        ensureTrackIndex();
        for (int i = 0; i < playlists.size(); i++) {
            Playlist p = playlists.get(i);
            if (p.manualOnly||!p.matches(c, settings) || (legacyRule(p.rule) && p.tracks.isEmpty())) continue;
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
    public String playbackUnavailable(Context c, Set<String> vanilla) {
        if (selectPlayable(c, vanilla, Set.of()) != null) return "";
        Playlist matched = select(c);
        if (matched != null) return "No enabled songs for " + matched.name + ". Add songs or use Play selected.";
        String ambient = c.underground() ? "Underground" : c.day() ? "Day outside" : "Night outside";
        var ambientPlaylist=playlist(c.underground()?"underground":c.day()?"day_outside":"night_outside");
        if(ambientPlaylist!=null&&ambientPlaylist.manualOnly)return ambient+" is manual only. Use Overrides to play it.";
        return ambient + " playlist is off. Enable it in Playlist settings or use Play selected.";
    }

    public static Library defaults() {
        Library l = new Library();
        l.playlists.add(new Playlist("combat", "Combat", Rule.COMBAT, ""));
        l.playlists.add(new Playlist("home", "Home", Rule.HOME, ""));
        Playlist dark=new Playlist("dark_exploration","Dark exploration",Rule.DARK_EXPLORATION,"");
        dark.includeVanilla=false;l.playlists.add(dark);
        l.playlists.add(new Playlist("underground", "Underground", Rule.UNDERGROUND, ""));
        l.playlists.add(new Playlist("night_outside", "Night outside", Rule.NIGHT_OUTSIDE, ""));
        l.playlists.add(new Playlist("day_outside", "Day outside", Rule.DAY_OUTSIDE, ""));
        return l;
    }

    private static boolean legacyRule(Rule rule) {
        return rule == Rule.BIOME || rule == Rule.DIMENSION || rule == Rule.UNDERWATER || rule == Rule.DAY || rule == Rule.NIGHT;
    }

    /** Non-destructive one-time upgrade from 0.1: keep music, toggles, and an archive of old rules. */
    public void migrate() {
        if (schemaVersion != 1) return;
        List<Playlist> old = new ArrayList<>(playlists);
        Library fresh=defaults(); playlists=fresh.playlists;
        if (legacyPlaylists==null) legacyPlaylists=new ArrayList<>();
        legacyPlaylists.addAll(old);
        for (String[] mapping : new String[][]{{"combat","combat"},{"day","day_outside"},{"night","night_outside"}}) {
            Playlist from=old.stream().filter(p->p.id.equals(mapping[0])).findFirst().orElse(null);
            Playlist to=playlist(mapping[1]);
            if(from!=null){to.tracks.putAll(from.tracks);to.finishCurrentSong=from.finishCurrentSong;to.finishCurrentSongOnExit=from.finishCurrentSongOnExit;to.includeVanilla=from.includeVanilla;
                // Old day/night defaults were disabled and empty; new ambient rules must work immediately.
                if(!from.tracks.isEmpty())to.enabled=from.enabled;
            }
        }
        Playlist general=old.stream().filter(p->p.id.equals("general")).findFirst().orElse(null);
        if(general!=null) for(String id:List.of("day_outside","night_outside","underground")){
            Playlist target=playlist(id);general.tracks.forEach(target.tracks::putIfAbsent);
            if(target.tracks.equals(general.tracks)) { target.includeVanilla=general.includeVanilla;target.finishCurrentSong=general.finishCurrentSong;target.finishCurrentSongOnExit=general.finishCurrentSongOnExit; }
        }
        for(Folder f:folders) f.playlistId=switch(f.playlistId){
            case "general" -> "ambient";
            case "day" -> "day_outside";
            case "night" -> "night_outside";
            case "combat" -> "combat";
            default -> "library";
        };
        schemaVersion=2;
    }

    public void assignImported(String destination,String trackId) {
        if (destination.equals("ambient")) {
            for(String id:List.of("day_outside","night_outside","underground")) playlist(id).tracks.putIfAbsent(trackId,true);
        } else {
            Playlist p=playlist(destination);if(p!=null)p.tracks.putIfAbsent(trackId,true);
        }
    }

    /** The writer owns this copy; later edits and scan results cannot change an in-flight save. */
    public Library snapshot() {
        Library copy = new Library(); copy.schemaVersion = schemaVersion;
        Settings s = copy.settings;
        s.silenceMinSeconds=settings.silenceMinSeconds;s.silenceMaxSeconds=settings.silenceMaxSeconds;
        s.fadeSeconds=settings.fadeSeconds;s.bufferSeconds=settings.bufferSeconds;s.combatQuietSeconds=settings.combatQuietSeconds;
        s.combatEnabled=settings.combatEnabled;s.rescanOnLaunch=settings.rescanOnLaunch;
        s.combatOnAttack=settings.combatOnAttack;s.combatOnDamage=settings.combatOnDamage;s.preventMusicWhileDead=settings.preventMusicWhileDead;
        s.combatDeathMusic=settings.combatDeathMusic;s.combatDeathFadeSeconds=settings.combatDeathFadeSeconds;
        s.nowPlaying=settings.nowPlaying;s.popupSeconds=settings.popupSeconds;s.undergroundDepth=settings.undergroundDepth;
        s.replayGainEnabled=settings.replayGainEnabled;
        for(int i=0;i<OVERRIDE_SLOTS;i++){s.overrideSlots[i].playlistId=settings.overrideSlots[i].playlistId;s.overrideSlots[i].mode=settings.overrideSlots[i].mode;}
        s.undergroundMaxY=settings.undergroundMaxY;s.darkMaxLight=settings.darkMaxLight;
        s.darkInNether=settings.darkInNether;s.darkInEnd=settings.darkInEnd;
        Home h=copy.home;h.configured=home.configured;h.world=home.world;h.dimension=home.dimension;
        h.x=home.x;h.y=home.y;h.z=home.z;h.radius=home.radius;
        copy.tracks = new ArrayList<>(tracks.size());
        for (int i=0;i<tracks.size();i++) {
            Track t=tracks.get(i), next=new Track(t.id,t.path,t.title,t.vanilla);
            next.artist=t.artist;next.album=t.album;next.durationSeconds=t.durationSeconds;copy.tracks.add(next);
        }
        for (Folder f:folders) { Folder next=new Folder(f.path,f.playlistId);next.recursive=f.recursive;copy.folders.add(next); }
        for (Playlist p:playlists) copy.playlists.add(copyPlaylist(p));
        for (Playlist p:legacyPlaylists) copy.legacyPlaylists.add(copyPlaylist(p));
        return copy;
    }
    private static Playlist copyPlaylist(Playlist p) {
        Playlist next=new Playlist(p.id,p.name,p.rule,p.match);next.enabled=p.enabled;
        next.manualOnly=p.manualOnly;
        next.finishCurrentSong=p.finishCurrentSong;next.finishCurrentSongOnExit=p.finishCurrentSongOnExit;
        next.includeVanilla=p.includeVanilla;next.conditions=p.conditions.copy();next.tracks.putAll(p.tracks);return next;
    }

    public void normalize() {
        if(settings.overrideSlots==null)settings.overrideSlots=defaultOverrideSlots();
        else if(settings.overrideSlots.length!=OVERRIDE_SLOTS)settings.overrideSlots=Arrays.copyOf(settings.overrideSlots,OVERRIDE_SLOTS);
        for(int i=0;i<OVERRIDE_SLOTS;i++){
            var b=settings.overrideSlots[i];if(b==null)b=settings.overrideSlots[i]=new OverrideBinding();
            if(b.playlistId==null)b.playlistId="";if(b.mode==null)b.mode=OverrideMode.ONE_SONG;
        }
        settings.silenceMinSeconds = bounded(settings.silenceMinSeconds, 0, 86400, 600);
        settings.silenceMaxSeconds = bounded(settings.silenceMaxSeconds, settings.silenceMinSeconds, 86400, 1200);
        settings.fadeSeconds = bounded(settings.fadeSeconds, 0, 60, 0);
        settings.bufferSeconds = bounded(settings.bufferSeconds, 0, 60, 0);
        settings.combatQuietSeconds = bounded(settings.combatQuietSeconds, 1, 600, 15);
        settings.combatDeathFadeSeconds=bounded(settings.combatDeathFadeSeconds,0,60,1);
        if(settings.combatDeathMusic==null)settings.combatDeathMusic=DeathMusic.FADE;
        settings.popupSeconds = bounded(settings.popupSeconds, 1, 60, 7);
        settings.undergroundDepth = bounded(settings.undergroundDepth, 1, 64, 5);
        settings.darkMaxLight = Math.clamp(settings.darkMaxLight,0,15);
        if(home==null)home=new Home();
        home.radius=bounded(home.radius,1,100000,64);
        for(Playlist p:playlists){if(p.conditions==null)p.conditions=new PlaylistConditions();p.conditions.normalize();}
        for(Playlist p:legacyPlaylists){if(p.conditions==null)p.conditions=new PlaylistConditions();p.conditions.normalize();}
    }
    private static double bounded(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : Math.clamp(fallback, min, max);
    }
}

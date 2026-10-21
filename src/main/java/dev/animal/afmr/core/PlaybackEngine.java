package dev.animal.afmr.core;

import java.util.*;
import dev.animal.afmr.core.Library.*;

/** All times are monotonic seconds. No Minecraft or audio-device dependencies. */
public final class PlaybackEngine {
    public interface Audio {
        void start(Track track) throws Exception;
        boolean playing();
        void gain(float gain);
        void stop();
        void setPaused(boolean paused);
    }
    public enum Phase { SILENCE, PLAYING, FADING }
    private final Audio audio;
    private final Random random;
    private Phase phase = Phase.SILENCE;
    private String activeId, candidateId, previousTrack,playingPlaylistId;
    private double candidateSince, silenceUntil = Double.NaN, fadeStarted, fadeInStarted,fadeDuration;
    private float fadeFrom = 1;
    private float currentGain;
    private Track current;
    private boolean previewing;
    private boolean playingCombat,wasDead,finishAfterDeath;
    private boolean paused;
    private double pausedAt;
    private Playlist selected,automaticSelected;
    private String overrideId;
    private OverrideMode overrideMode;
    private boolean overrideStarted,immediateNext;
    private final Map<String,ShuffleBag> shuffleBags=new HashMap<>();
    private final Set<String> failed = new HashSet<>();
    private final Set<String> failedView=Collections.unmodifiableSet(failed);
    private String error = "";

    public PlaybackEngine(Audio audio, Random random) { this.audio = audio; this.random = random; }

    public void tick(double now, Library lib, Context context, Set<String> vanilla) {
        now=playbackTime(now);
        automaticSelected = lib.selectPlayable(context, vanilla, failed);
        Playlist forced=overrideId==null?null:lib.playlist(overrideId);
        if(overrideId!=null&&(forced==null||!lib.hasPlayable(forced,vanilla,failed)))clearOverride();
        selected = overrideId==null?automaticSelected:forced;
        if(context.dead()&&!wasDead&&playingCombat&&!previewing&&current!=null){
            switch(lib.settings.combatDeathMusic){
                case CUT -> {audio.stop();enterSilence(now,lib.settings);}
                case FADE -> {phase=Phase.FADING;fadeStarted=now;fadeFrom=currentGain;fadeDuration=lib.settings.combatDeathFadeSeconds;}
                case KEEP_PLAYING -> {if(phase==Phase.FADING)fadeInStarted=now-lib.settings.fadeSeconds*currentGain;phase=Phase.PLAYING;finishAfterDeath=true;}
            }
        }
        wasDead=context.dead();
        if(paused)return;
        String selectedId = selected == null ? null : selected.id;
        if (!Objects.equals(selectedId, candidateId)) { candidateId = selectedId; candidateSince = now; }
        boolean accepted = now - candidateSince >= lib.settings.bufferSeconds;
        if (accepted && !Objects.equals(activeId, candidateId)) {
            activeId = candidateId;
            if (phase == Phase.PLAYING && !previewing && !finishAfterDeath && !Objects.equals(playingPlaylistId,candidateId)) {
                // A retained song still belongs to the playlist that started it, even after several transitions.
                Playlist outgoing=lib.playlist(playingPlaylistId);
                if((selected==null||!selected.finishCurrentSong)&&(outgoing==null||!outgoing.finishCurrentSongOnExit)){
                    phase = Phase.FADING; fadeStarted = now; fadeFrom = currentGain;fadeDuration=lib.settings.fadeSeconds;
                }
            }
        }
        if (phase != Phase.SILENCE && !audio.playing()) enterSilence(now, lib.settings);
        if (phase == Phase.FADING) {
            double fraction = fadeDuration == 0 ? 1 : (now - fadeStarted) / fadeDuration;
            currentGain = (float) (fadeFrom * (1 - Math.clamp(fraction, 0, 1)));
            audio.gain(currentGain);
            if (fraction >= 1) { audio.stop(); enterSilence(now, lib.settings); }
        } else if (phase == Phase.PLAYING) {
            currentGain = lib.settings.fadeSeconds == 0 ? 1 : (float) Math.clamp((now - fadeInStarted) / lib.settings.fadeSeconds, 0, 1);
            audio.gain(currentGain);
        }
        if (phase == Phase.SILENCE) {
            accepted=now-candidateSince>=lib.settings.bufferSeconds;
            if (Double.isNaN(silenceUntil)) silenceUntil = now + delay(lib.settings);
            if(context.dead()&&lib.settings.preventMusicWhileDead)return;
            // Resolve against the current context only after its grace period has elapsed.
            if (accepted && now >= silenceUntil) {
                if (selected == null) { silenceUntil = now + 1; return; }
                List<Track> pool = lib.pool(selected, vanilla, false);
                pool.removeIf(t -> failed.contains(t.id));
                if (pool.isEmpty()) { silenceUntil = now + 1; return; }
                Track next = shuffleBags.computeIfAbsent(selected.id,id->new ShuffleBag()).next(pool,previousTrack,random);
                try {
                    audio.gain(0); audio.start(next);
                    current = next; previousTrack = next.id; phase = Phase.PLAYING;
                    playingPlaylistId=selected.id;
                    overrideStarted=overrideId!=null;immediateNext=false;
                    playingCombat=selected.combatDriven();finishAfterDeath=false;
                    fadeInStarted = now; currentGain = 0; error = "";
                } catch (Exception e) {
                    audio.stop(); failed.add(next.id); error = next.title + ": " + e.getMessage();
                    silenceUntil = now + 1;
                }
            }
        }
    }

    private void enterSilence(double now, Settings settings) {
        if(overrideMode==OverrideMode.ONE_SONG&&overrideStarted){
            clearOverride();selected=automaticSelected;candidateId=selected==null?null:selected.id;activeId=candidateId;candidateSince=now;
        }
        phase = Phase.SILENCE; current = null; previewing = false;playingPlaylistId=null;playingCombat=false;finishAfterDeath=false; currentGain = 0;
        silenceUntil=immediateNext?now:now+delay(settings);immediateNext=false;
    }
    private double delay(Settings s) {
        return s.silenceMinSeconds + random.nextDouble() * (s.silenceMaxSeconds - s.silenceMinSeconds);
    }
    public void skip(double now, Settings settings) { audio.stop(); enterSilence(playbackTime(now), settings); }
    public void playNow(double now) { if (phase == Phase.SILENCE) silenceUntil = playbackTime(now); }
    /** Skip to the effective playlist's next shuffle entry, preserving any override. */
    public boolean nextSong(double now,Library lib,Context context,Set<String> vanilla){
        Playlist forced=overrideId==null?null:lib.playlist(overrideId);
        if(overrideId!=null&&(forced==null||!lib.hasPlayable(forced,vanilla,failed)))clearOverride();
        automaticSelected=lib.selectPlayable(context,vanilla,failed);Playlist target=overrideId==null?automaticSelected:forced;
        if(target==null)return false;
        now=playbackTime(now);overrideStarted=false;immediateNext=true;audio.stop();enterSilence(now,lib.settings);
        selected=target;candidateId=target.id;activeId=target.id;candidateSince=now-lib.settings.bufferSeconds;return true;
    }
    public void setPaused(boolean value,double now){
        if(paused==value)return;
        if(value){pausedAt=now;paused=true;}
        else{
            double elapsed=Math.max(0,now-pausedAt);candidateSince+=elapsed;fadeStarted+=elapsed;fadeInStarted+=elapsed;
            if(!Double.isNaN(silenceUntil))silenceUntil+=elapsed;paused=false;
        }
        audio.setPaused(value);
    }
    private double playbackTime(double now){return paused?pausedAt:now;}
    /** An explicit request bypasses conditions, finish toggles, context buffer and the initial silent gap. */
    public boolean overridePlaylist(String id,OverrideMode mode,double now,Library lib,Set<String> vanilla){
        Playlist p=lib.playlist(id);if(mode==null||p==null||!lib.hasPlayable(p,vanilla,failed))return false;
        now=playbackTime(now);
        overrideId=id;overrideMode=mode;overrideStarted=false;immediateNext=true;previewing=false;finishAfterDeath=false;
        selected=p;candidateId=id;activeId=id;candidateSince=now-lib.settings.bufferSeconds;
        if(current!=null){phase=Phase.FADING;fadeStarted=now;fadeFrom=currentGain;fadeDuration=lib.settings.fadeSeconds;}
        else{phase=Phase.SILENCE;silenceUntil=now;}
        return true;
    }
    public void returnAutomatic(){clearOverride();}
    private void clearOverride(){overrideId=null;overrideMode=null;overrideStarted=false;immediateNext=false;}
    /** Explicit previews bypass automatic playlist rules and finish before scheduling resumes. */
    public void preview(Track track, double now) {
        now=playbackTime(now);
        clearOverride();
        audio.stop(); current = null; previewing = false; phase = Phase.SILENCE;
        playingPlaylistId=null;playingCombat=false;finishAfterDeath=false;
        try {
            audio.gain(0); audio.start(track);
            current = track; previousTrack = track.id; phase = Phase.PLAYING; previewing = true;
            fadeInStarted = now; currentGain = 0; failed.remove(track.id); error = "";
        } catch (Exception e) {
            audio.stop(); failed.add(track.id); error = track.title + ": " + e.getMessage(); silenceUntil = now + 1;
        }
    }
    public boolean previewing() { return previewing; }
    public boolean paused(){return paused;}
    public void reset() {
        paused=false;
        audio.stop(); phase = Phase.SILENCE; current = null; previewing = false; activeId = null; candidateId = null;
        audio.setPaused(false);
        silenceUntil = Double.NaN; failed.clear(); error = "";
        selected = null;automaticSelected=null;clearOverride();shuffleBags.clear();
        playingPlaylistId=null;playingCombat=false;wasDead=false;finishAfterDeath=false;
    }
    public void retryFailed() { failed.clear();error=""; }
    public void failed(String trackId, String message, double now, Settings settings) {
        now=playbackTime(now);
        if (current != null && current.id.equals(trackId)) {
            failed.add(trackId); error = message; audio.stop(); enterSilence(now, settings);
            silenceUntil = now + 1;
        }
    }
    public Phase phase() { return phase; }
    public String sourcePlaylistId(){return playingPlaylistId;}
    public Set<String> failedTracks(){return failedView;}
    public Playlist automaticSelected(){return automaticSelected;}
    public String overrideId(){return overrideId;}
    public OverrideMode overrideMode(){return overrideMode;}
    public boolean overridePending(){return immediateNext;}
    public boolean finishingAfterDeath(){return finishAfterDeath;}
    public double remainingBuffer(double now,Settings settings){return Math.max(0,settings.bufferSeconds-(playbackTime(now)-candidateSince));}
    public Playlist selected() { return selected; }
    public Track current() { return current; }
    public String error() { return error; }
    public double remainingSilence(double now) { return Double.isNaN(silenceUntil) ? 0 : Math.max(0, silenceUntil - playbackTime(now)); }
}

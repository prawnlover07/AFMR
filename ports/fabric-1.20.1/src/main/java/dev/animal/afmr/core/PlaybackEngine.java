package dev.animal.afmr.core;

import java.util.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.ListeningHistory.Origin;

/** All times are monotonic seconds. No Minecraft or audio-device dependencies. */
public final class PlaybackEngine {
    public interface Audio {
        void start(Track track) throws Exception;
        boolean playing();
        boolean started();
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
    // Only ordinary gaps track an anchor. Explicit bypasses and failure retries do not.
    private double silenceStarted=Double.NaN,silenceMinimum,silenceMaximum;
    private Playlist silenceSource;
    private boolean silenceCustom;
    private float fadeFrom = 1;
    private float currentGain;
    private Track current;
    private Playlist playingSource;
    private long playbackSequence;
    private double trackStarted;
    private String groupPlaylistId;
    private int groupStarted;
    private boolean previewing,joinPending;
    private boolean queuedPlaying,queueWaitingForOverride;
    private final PlaybackQueue queue=new PlaybackQueue();
    private final ListeningHistory history=new ListeningHistory();
    private String queueMessage="";
    private boolean playingCombat,wasDead,finishAfterDeath;
    private boolean paused;
    private enum JukeboxPhase { NONE, FADING_OUT, HELD }
    private JukeboxPhase jukeboxPhase=JukeboxPhase.NONE;
    private JukeboxMode jukeboxMode=JukeboxMode.OFF;
    private boolean jukeboxHeld;
    private double jukeboxFadeStarted;
    private float jukeboxFadeFrom;
    private double pausedAt;
    private Playlist selected,automaticSelected;
    private String overrideId;
    private OverrideMode overrideMode;
    private boolean overrideStarted,immediateNext,triggerPending,triggerBufferBypass;
    private final Map<String,ShuffleBag> shuffleBags=new HashMap<>();
    private final Set<String> failed = new HashSet<>();
    private final Set<String> failedView=Collections.unmodifiableSet(failed);
    private String error = "";
    private boolean waitingForTimeFit;
    private Track durationNeeded;
    private final Set<String> durationAttempted=new HashSet<>();

    public PlaybackEngine(Audio audio, Random random) { this.audio = audio; this.random = random; }

    /** Only an actual world/server join arms this; reset/profile changes never infer a join. */
    public void joinedWorld(Settings settings){
        joinPending=settings.playImmediatelyOnJoin&&phase==Phase.SILENCE&&overrideId==null&&queue.empty();
    }

    public void tick(double now, Library lib, Context context, Set<String> vanilla) {
        double wallNow=now;
        now=playbackTime(now);
        automaticSelected = lib.selectPlayable(context, vanilla, failed);
        Playlist forced=overrideId==null?null:lib.playlist(overrideId);
        if(overrideId!=null&&(forced==null||!lib.hasPlayable(forced,vanilla,failed)))clearOverride();
        var previousSelection=selected;
        selected = overrideId==null?automaticSelected:forced;
        if(previousSelection!=selected){waitingForTimeFit=false;durationNeeded=null;}
        if(context.dead()&&!wasDead&&playingCombat&&!previewing&&current!=null){
            switch(lib.settings.combatDeathMusic){
                case CUT -> {audio.stop();enterSilence(now,lib,false);}
                case FADE -> {phase=Phase.FADING;fadeStarted=now;fadeFrom=currentGain;fadeDuration=lib.settings.combatDeathFadeSeconds;}
                case KEEP_PLAYING -> {if(phase==Phase.FADING)fadeInStarted=now-lib.settings.fadeInSeconds*currentGain;phase=Phase.PLAYING;finishAfterDeath=true;}
            }
        }
        wasDead=context.dead();
        if(updateJukebox(wallNow,lib,context.jukeboxPlaying()))return;
        now=playbackTime(wallNow);
        if(current!=null&&Double.isNaN(trackStarted)&&audio.started())trackStarted=now;
        if(paused)return;
        String selectedId = selected == null ? null : selected.id;
        if (!Objects.equals(selectedId, candidateId)) {
            candidateId=selectedId;candidateSince=now;
            triggerBufferBypass=overrideId==null&&selected!=null&&selected.playOnTrigger&&!previewing&&!queuedPlaying&&!queueReady();
            triggerPending=triggerBufferBypass&&(phase!=Phase.PLAYING||!Objects.equals(playingPlaylistId,selectedId));
            if(triggerPending&&phase==Phase.SILENCE){silenceUntil=now;silenceStarted=Double.NaN;}
        }
        if(joinPending&&!lib.settings.playImmediatelyOnJoin)joinPending=false;
        if(joinPending&&selected!=null&&phase==Phase.SILENCE){
            joinPending=false;
            if(overrideId==null&&!queueReady()&&!previewing){
                silenceUntil=now;silenceStarted=Double.NaN;candidateSince=now-lib.settings.bufferSeconds;
            }
        }
        boolean accepted = triggerBufferBypass||now-candidateSince>=lib.settings.bufferSeconds;
        if (accepted && !Objects.equals(activeId, candidateId)) {
            activeId = candidateId;groupPlaylistId=null;groupStarted=0;
            if (phase == Phase.PLAYING && !previewing && !queuedPlaying && !finishAfterDeath && !Objects.equals(playingPlaylistId,candidateId)) {
                // A retained song still belongs to the playlist that started it, even after several transitions.
                Playlist outgoing=lib.playlist(playingPlaylistId);
                boolean matching=overrideId==null&&lib.settings.continuePlayingOnMatch&&current!=null
                    &&lib.isPlayable(selected,current.id,vanilla,failed);
                if(!matching&&(selected==null||!selected.finishCurrentSong)&&(outgoing==null||!outgoing.finishCurrentSongOnExit)){
                    phase = Phase.FADING; fadeStarted = now; fadeFrom = currentGain;fadeDuration=lib.settings.fadeOutSeconds;
                }
            }
        }
        if (phase != Phase.SILENCE && !audio.playing()) enterSilence(now, lib,true);
        if (phase == Phase.FADING) {
            double fraction = fadeDuration == 0 ? 1 : (now - fadeStarted) / fadeDuration;
            currentGain = (float) (fadeFrom * (1 - Math.max( 0,Math.min(fraction, 1))));
            audio.gain(currentGain*(playingSource==null?1:(float)playingSource.volume));
            if (fraction >= 1) { audio.stop(); enterSilence(now, lib,false); }
        } else if (phase == Phase.PLAYING) {
            currentGain = lib.settings.fadeInSeconds == 0 ? 1 : (float) Math.max( 0,Math.min((now - fadeInStarted) / lib.settings.fadeInSeconds, 1));
            audio.gain(currentGain*(playingSource==null?1:(float)playingSource.volume));
        }
        if (phase == Phase.SILENCE) {
            accepted=triggerBufferBypass||now-candidateSince>=lib.settings.bufferSeconds;
            // Discard stale queue heads before choosing a range, so their old playlist cannot delay the next request.
            while(queueReady()){
                var request=queue.first();
                if(queueAvailable(request.trackId(),request.playlistId(),lib))break;
                queue.removeFirst();queueMessage="Skipped "+request.title()+": missing, disabled, removed or failed";
            }
            if (Double.isNaN(silenceUntil)) scheduleSilence(now,lib);
            else if(!Double.isNaN(silenceStarted)) refreshSilence(lib);
            if(context.dead()&&lib.settings.preventMusicWhileDead)return;
            if(queueReady()&&now>=silenceUntil){
                while(!queue.empty()){
                    var request=queue.first();queue.removeFirst();
                    Track track=lib.track(request.trackId());Playlist source=lib.playlist(request.playlistId());
                    if(!queueAvailable(request.trackId(),request.playlistId(),lib)){
                        queueMessage="Skipped "+request.title()+": missing, disabled, removed or failed";continue;
                    }
                    try{
                        startTrack(track,source,Origin.QUEUE,"Queued manually"+(source==null?"":" from "+source.name),
                            source!=null&&source.combatDriven(context,lib.settings),now);
                        overrideStarted=false;immediateNext=false;triggerPending=triggerBufferBypass=false;
                    }catch(Exception e){
                        audio.stop();failed.add(track.id);error=track.title+": "+e.getMessage();queueMessage=error;silenceUntil=now+1;silenceStarted=Double.NaN;
                    }
                    return;
                }
            }
            // Resolve against the current context only after its grace period has elapsed.
            if (accepted && now >= silenceUntil) {
                if (selected == null) { silenceUntil = now + 1; return; }
                List<Track> pool = lib.pool(selected, vanilla, false);
                pool.removeIf(t -> failed.contains(t.id));
                double budget=overrideId==null?selected.conditions.songTimeBudget(context,lib.settings):Double.POSITIVE_INFINITY;
                if(Double.isFinite(budget)){
                    durationNeeded=null;
                    for(var track:pool){
                        double duration=track.durationSeconds;
                        if(!Double.isFinite(duration)||duration<=0){
                            if(durationNeeded==null&&!durationAttempted.contains(track.id))durationNeeded=track;
                        }
                    }
                    pool.removeIf(t->!Double.isFinite(t.durationSeconds)||t.durationSeconds<=0||t.durationSeconds>budget);
                    waitingForTimeFit=pool.isEmpty();
                }else{waitingForTimeFit=false;durationNeeded=null;}
                if (pool.isEmpty()) { silenceUntil = now + 1; return; }
                Track next = shuffleBags.computeIfAbsent(selected.id,id->new ShuffleBag()).next(pool,previousTrack,random);
                try {
                    startTrack(next,selected,overrideId==null?Origin.AUTOMATIC:Origin.OVERRIDE,
                        overrideId==null?automaticReason(selected,context):"Playlist override: "+overrideMode+"; conditions bypassed",
                        selected.combatDriven(context,lib.settings),now);
                    overrideStarted=overrideId!=null;immediateNext=false;triggerPending=false;
                } catch (Exception e) {
                    audio.stop(); failed.add(next.id); error = next.title + ": " + e.getMessage();
                    silenceUntil = now + 1;silenceStarted=Double.NaN;
                }
            }
        }
    }

    private static String automaticReason(Playlist playlist,Context context){
        return "Automatic winner: "+context.dimension()+" / "+context.biome()+"; tick "+context.timeOfDay()
            +"; combat "+context.combat()+"; PvP "+context.playerCombat()+"; health "+context.healthHearts()+" hearts"
            +"; Y "+(int)Math.floor(context.y())+"; covered "+context.covered()+"; light "+context.lightLevel()
            +"; weather "+(context.thundering()?"thunderstorm":context.raining()?context.precipitation().name().toLowerCase(Locale.ROOT):"clear")
            +"; conditions: "+playlist.conditions.summary();
    }

    private void startTrack(Track track,Playlist source,Origin origin,String reason,boolean combat,double now)throws Exception{
        joinPending=false;audio.gain(0);audio.start(track);
        silenceStarted=Double.NaN;silenceUntil=Double.NaN;
        if(source!=null&&(origin==Origin.AUTOMATIC||origin==Origin.OVERRIDE)){
            if(!Objects.equals(groupPlaylistId,source.id)){groupPlaylistId=source.id;groupStarted=0;}
            groupStarted++;
        }else{groupPlaylistId=null;groupStarted=0;}
        trackStarted=audio.started()?now:Double.NaN;
        current=track;playbackSequence++;previousTrack=track.id;phase=Phase.PLAYING;
        playingPlaylistId=source==null?null:source.id;playingCombat=combat;finishAfterDeath=false;
        playingSource=source;
        previewing=origin==Origin.PREVIEW;queuedPlaying=origin==Origin.QUEUE;
        fadeInStarted=now;currentGain=0;error="";waitingForTimeFit=false;durationNeeded=null;
        history.record(track,source,origin,reason);
    }

    public boolean enqueue(String trackId,String playlistId,Library lib){
        String sourceId=playlistId==null?"":playlistId;
        if(!queueAvailable(trackId,sourceId,lib)){queueMessage="Cannot queue: song is missing, disabled, removed or failed";return false;}
        if(!queue.add(lib.track(trackId),lib.playlist(sourceId))){queueMessage="Queue is full ("+PlaybackQueue.LIMIT+" songs)";return false;}
        joinPending=false;queueWaitingForOverride=false;waitingForTimeFit=false;durationNeeded=null;triggerPending=triggerBufferBypass=false;
        queueMessage="Queued: "+lib.track(trackId).title;return true;
    }
    private boolean queueAvailable(String trackId,String playlistId,Library lib){
        var track=lib.track(trackId);if(track==null||failed.contains(trackId))return false;
        if(playlistId.isBlank())return true;
        var playlist=lib.playlist(playlistId);if(playlist==null)return false;
        Boolean explicit=playlist.tracks.get(trackId);
        return explicit!=null?explicit:track.vanilla&&playlist.includeVanilla;
    }
    private boolean queueReady(){return !queue.empty()&&!queueWaitingForOverride;}
    public void useQueueNext(){queueWaitingForOverride=false;}
    public PlaybackQueue queue(){return queue;}
    public ListeningHistory history(){return history;}
    public String queueMessage(){return queueMessage;}
    public boolean queuedPlaying(){return queuedPlaying;}
    public boolean queueWaitingForOverride(){return queueWaitingForOverride&&!queue.empty();}

    private void enterSilence(double now, Library lib,boolean natural) {
        boolean consecutive=natural&&phase==Phase.PLAYING&&!previewing&&!queuedPlaying&&!finishAfterDeath
            &&playingSource!=null&&playingSource==selected&&Objects.equals(groupPlaylistId,playingSource.id)
            &&!queueReady()&&overrideMode!=OverrideMode.ONE_SONG&&groupStarted<songsBeforeSilence(playingSource,lib.settings);
        if(!consecutive){groupPlaylistId=null;groupStarted=0;}
        playingSource=null;
        waitingForTimeFit=false;durationNeeded=null;
        if(overrideMode==OverrideMode.ONE_SONG&&overrideStarted){
            clearOverride();selected=automaticSelected;candidateId=selected==null?null:selected.id;activeId=candidateId;candidateSince=now;
        }
        phase = Phase.SILENCE; current = null; previewing = false;queuedPlaying=false;playingPlaylistId=null;playingCombat=false;finishAfterDeath=false; currentGain = 0;
        if(consecutive||immediateNext||triggerPending){silenceUntil=now;silenceStarted=Double.NaN;}
        else scheduleSilence(now,lib);
        immediateNext=false;triggerPending=false;
    }
    private Playlist silencePlaylist(Library lib){
        return queueReady()?lib.playlist(queue.first().playlistId()):selected;
    }
    private void scheduleSilence(double now,Library lib){
        silenceStarted=now;
        silenceMinimum=Double.NaN;silenceMaximum=Double.NaN;
        refreshSilence(lib);
    }
    /** Range changes reuse the original start; unchanged ranges never reroll the gap. */
    private void refreshSilence(Library lib){
        Playlist p=silencePlaylist(lib);
        boolean custom=p!=null&&p.customSilence;
        // Ordinary global edits keep an existing global gap; explicit frequency changes reschedule it.
        if(p==silenceSource&&custom==silenceCustom&&!custom&&!Double.isNaN(silenceMinimum))return;
        silenceSource=p;silenceCustom=custom;
        double min=custom?p.silenceMinSeconds:lib.settings.silenceMinSeconds;
        double max=custom?p.silenceMaxSeconds:lib.settings.silenceMaxSeconds;
        if(min==silenceMinimum&&max==silenceMaximum)return;
        silenceMinimum=min;silenceMaximum=max;
        silenceUntil=silenceStarted+min+random.nextDouble()*(max-min);
    }
    public void skip(double now, Library lib) { joinPending=false;triggerPending=false;audio.stop();enterSilence(playbackTime(now),lib,false); }
    public void playNow(double now) { if (phase == Phase.SILENCE){silenceUntil=playbackTime(now);silenceStarted=Double.NaN;} }
    /** Explicit global frequency changes restart only gaps which use global timing. */
    public void rescheduleSilence(double now,Library lib){
        if(phase==Phase.SILENCE){
            Playlist p=silencePlaylist(lib);
            if(p!=null&&p.customSilence)return;
            joinPending=false;scheduleSilence(playbackTime(now),lib);waitingForTimeFit=false;durationNeeded=null;
        }
    }
    /** Skip to the effective playlist's next shuffle entry, preserving any override. */
    public boolean nextSong(double now,Library lib,Context context,Set<String> vanilla){
        Playlist forced=overrideId==null?null:lib.playlist(overrideId);
        if(overrideId!=null&&(forced==null||!lib.hasPlayable(forced,vanilla,failed)))clearOverride();
        automaticSelected=lib.selectPlayable(context,vanilla,failed);Playlist target=overrideId==null?automaticSelected:forced;
        if(target==null&&!queueReady())return false;
        joinPending=false;now=playbackTime(now);overrideStarted=false;immediateNext=true;triggerPending=triggerBufferBypass=false;audio.stop();enterSilence(now,lib,false);
        selected=target;candidateId=target==null?null:target.id;activeId=candidateId;candidateSince=now-lib.settings.bufferSeconds;return true;
    }
    private boolean updateJukebox(double wallNow,Library lib,boolean playing){
        Settings settings=lib.settings;
        double now=playbackTime(wallNow);
        var mode=settings.jukeboxMode;
        if(!playing||mode==JukeboxMode.OFF){
            if(jukeboxPhase==JukeboxPhase.NONE)return false;
            var endingMode=jukeboxMode;
            if(endingMode!=JukeboxMode.RESUME&&current!=null){audio.stop();enterSilence(now,lib,false);}
            boolean wasPaused=paused||jukeboxHeld;jukeboxHeld=false;updatePauseState(wasPaused,wallNow);
            now=playbackTime(wallNow);
            if(endingMode==JukeboxMode.RESUME&&current!=null){
                if(phase==Phase.FADING){
                    fadeDuration=Math.max(0,fadeDuration-(now-fadeStarted));fadeFrom=currentGain;fadeStarted=now;
                }else fadeInStarted=now-settings.fadeInSeconds*currentGain;
            }else if(endingMode!=JukeboxMode.RESUME){
                if(endingMode==JukeboxMode.IMMEDIATE){silenceUntil=now;silenceStarted=Double.NaN;}
                else scheduleSilence(now,lib);
                candidateId=selected==null?null:selected.id;activeId=candidateId;candidateSince=now;
                triggerPending=false;triggerBufferBypass=endingMode==JukeboxMode.IMMEDIATE;
            }
            jukeboxPhase=JukeboxPhase.NONE;jukeboxMode=JukeboxMode.OFF;return false;
        }
        if(jukeboxPhase==JukeboxPhase.NONE){
            joinPending=false;jukeboxPhase=JukeboxPhase.FADING_OUT;jukeboxFadeStarted=now;jukeboxFadeFrom=currentGain;
        }
        jukeboxMode=mode;
        if(jukeboxPhase==JukeboxPhase.HELD){
            if(mode!=JukeboxMode.RESUME&&current!=null){audio.stop();enterSilence(now,lib,false);}
            return true;
        }
        if(current!=null&&!audio.playing())enterSilence(now,lib,false);
        double fraction=settings.fadeOutSeconds==0?1:(now-jukeboxFadeStarted)/settings.fadeOutSeconds;
        currentGain=jukeboxFadeFrom*(float)(1-Math.max(0,Math.min(fraction,1)));audio.gain(currentGain*(playingSource==null?1:(float)playingSource.volume));
        if(current==null||fraction>=1){
            if(mode!=JukeboxMode.RESUME&&current!=null){audio.stop();enterSilence(now,lib,false);}
            boolean wasPaused=paused||jukeboxHeld;jukeboxHeld=true;updatePauseState(wasPaused,wallNow);
            jukeboxPhase=JukeboxPhase.HELD;
        }
        return true;
    }
    public void setPaused(boolean value,double now){
        if(paused==value)return;
        boolean wasPaused=paused||jukeboxHeld;paused=value;updatePauseState(wasPaused,now);
    }
    private void updatePauseState(boolean wasPaused,double now){
        boolean pause=paused||jukeboxHeld;if(wasPaused==pause)return;
        if(pause)pausedAt=now;
        else{
            double elapsed=Math.max(0,now-pausedAt);trackStarted+=elapsed;candidateSince+=elapsed;fadeStarted+=elapsed;fadeInStarted+=elapsed;
            jukeboxFadeStarted+=elapsed;
            if(!Double.isNaN(silenceUntil))silenceUntil+=elapsed;
            if(!Double.isNaN(silenceStarted))silenceStarted+=elapsed;
        }
        audio.setPaused(pause);
    }
    private double playbackTime(double now){return paused||jukeboxHeld?pausedAt:now;}
    /** An explicit request bypasses conditions, finish toggles, context buffer and the initial silent gap. */
    public boolean overridePlaylist(String id,OverrideMode mode,double now,Library lib,Set<String> vanilla){
        Playlist p=lib.playlist(id);if(mode==null||p==null||!lib.hasPlayable(p,vanilla,failed))return false;
        joinPending=false;now=playbackTime(now);groupPlaylistId=null;groupStarted=0;
        waitingForTimeFit=false;durationNeeded=null;
        overrideId=id;overrideMode=mode;overrideStarted=false;immediateNext=true;triggerPending=triggerBufferBypass=false;previewing=false;finishAfterDeath=false;
        queueWaitingForOverride=true;
        selected=p;candidateId=id;activeId=id;candidateSince=now-lib.settings.bufferSeconds;
        if(current!=null){phase=Phase.FADING;fadeStarted=now;fadeFrom=currentGain;fadeDuration=lib.settings.fadeOutSeconds;}
        else{phase=Phase.SILENCE;silenceUntil=now;silenceStarted=Double.NaN;}
        return true;
    }
    public void returnAutomatic(){clearOverride();}
    private void clearOverride(){overrideId=null;overrideMode=null;overrideStarted=false;immediateNext=false;queueWaitingForOverride=false;}
    /** Explicit previews bypass automatic playlist rules and finish before scheduling resumes. */
    public void preview(Track track, double now) {
        joinPending=false;triggerPending=triggerBufferBypass=false;
        waitingForTimeFit=false;durationNeeded=null;
        now=playbackTime(now);
        clearOverride();
        audio.stop(); current = null; previewing = false;queuedPlaying=false;phase = Phase.SILENCE;
        playingPlaylistId=null;playingCombat=false;finishAfterDeath=false;
        try {
            startTrack(track,null,Origin.PREVIEW,"Manual preview",false,now);failed.remove(track.id);
        } catch (Exception e) {
            audio.stop(); failed.add(track.id); error = track.title + ": " + e.getMessage(); silenceUntil = now + 1;silenceStarted=Double.NaN;
        }
    }
    public boolean previewing() { return previewing; }
    /** Distinguishes new starts of the same track; pause/resume and retained songs keep their identity. */
    public long playbackSequence(){return playbackSequence;}
    public double trackElapsed(double now){return current==null||Double.isNaN(trackStarted)?0:Math.max(0,playbackTime(now)-trackStarted);}
    public int groupStarted(){return groupStarted;}
    public static int songsBeforeSilence(Playlist p,Settings s){return p!=null&&p.customSongCount?p.songsBeforeSilence:s.songsBeforeSilence;}
    public boolean paused(){return paused;}
    public void reset() {
        joinPending=false;playingSource=null;silenceSource=null;groupPlaylistId=null;groupStarted=0;
        queue.clear();queueMessage="";queuedPlaying=queueWaitingForOverride=false;
        triggerPending=triggerBufferBypass=false;
        jukeboxPhase=JukeboxPhase.NONE;jukeboxHeld=false;jukeboxMode=JukeboxMode.OFF;
        paused=false;
        audio.stop(); phase = Phase.SILENCE; current = null; previewing = false; activeId = null; candidateId = null;
        audio.setPaused(false);
        silenceUntil = silenceStarted = Double.NaN; failed.clear(); error = "";
        selected = null;automaticSelected=null;clearOverride();shuffleBags.clear();
        waitingForTimeFit=false;durationNeeded=null;durationAttempted.clear();
        playingPlaylistId=null;playingCombat=false;wasDead=false;finishAfterDeath=false;
    }
    public void retryFailed() { failed.clear();durationAttempted.clear();error=""; }
    public boolean waitingForTimeFit(){return waitingForTimeFit;}
    /** One owned source request is consumed by the client; failed duration checks do not disable audio. */
    public Track takeDurationRequest(){
        var track=durationNeeded;durationNeeded=null;
        if(track!=null)durationAttempted.add(track.id);return track;
    }
    public void failed(String trackId, String message, double now, Library lib) {
        now=playbackTime(now);
        if (current != null && current.id.equals(trackId)) {
            failed.add(trackId); error = message; audio.stop(); enterSilence(now, lib,false);
            silenceUntil = now + 1;silenceStarted=Double.NaN;
        }
    }
    public Phase phase() { return phase; }
    public String sourcePlaylistId(){return playingPlaylistId;}
    public Set<String> failedTracks(){return failedView;}
    public Playlist automaticSelected(){return automaticSelected;}
    public String overrideId(){return overrideId;}
    public OverrideMode overrideMode(){return overrideMode;}
    public boolean overridePending(){return immediateNext;}
    public boolean triggerPending(){return triggerPending;}
    public boolean jukeboxActive(){return jukeboxPhase!=JukeboxPhase.NONE;}
    public boolean jukeboxHolding(){return jukeboxHeld;}
    public boolean finishingAfterDeath(){return finishAfterDeath;}
    public double remainingBuffer(double now,Settings settings){return triggerBufferBypass?0:Math.max(0,settings.bufferSeconds-(playbackTime(now)-candidateSince));}
    public Playlist selected() { return selected; }
    public Track current() { return current; }
    public String error() { return error; }
    public double remainingSilence(double now) { return Double.isNaN(silenceUntil) ? 0 : Math.max(0, silenceUntil - playbackTime(now)); }
}

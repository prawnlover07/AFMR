package dev.animal.afmr.core;

/** Compact transport state. Uses scheduler state already computed by the client; no library scans. */
public final class PlaybackStatus {
    private PlaybackStatus(){}
    public static String label(PlaybackEngine engine,Library.Settings settings,boolean inWorld,boolean dead,String unavailable,double now){
        if(!inWorld)return "Join a world to play music";
        if(engine.paused())return "Paused";
        if(engine.jukeboxActive())return engine.jukeboxHolding()?"Jukebox playing":"Fading for jukebox";
        if(engine.current()!=null)return engine.previewing()?"Preview":engine.phase()==PlaybackEngine.Phase.FADING?"Fading out":"Playing";
        if(dead&&settings.preventMusicWhileDead)return "Waiting for respawn";
        if(!engine.error().isBlank())return "Playback error";
        boolean queued=!engine.queue().empty()&&!engine.queueWaitingForOverride();
        if(engine.waitingForTimeFit())return "Waiting for a fitting song";
        if(!queued&&engine.selected()==null)return unavailable.isBlank()?"No playable songs":unavailable;
        double delay=engine.remainingSilence(now);
        if(!queued)delay=Math.max(delay,engine.remainingBuffer(now,settings));
        if(delay>0)return "Next song in "+countdown(delay);
        return queued?"Starting queued song":"Starting music";
    }
    public static String countdown(double seconds){
        long whole=(long)Math.ceil(Math.max(0,Double.isFinite(seconds)?seconds:0));
        return whole/60+":"+(whole%60<10?"0":"")+whole%60;
    }
}

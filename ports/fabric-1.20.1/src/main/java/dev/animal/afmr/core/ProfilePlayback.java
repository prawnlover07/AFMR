package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.*;

/** Only playback policy travels with a profile. Display, imports, bindings and history stay global. */
public final class ProfilePlayback {
    public double silenceMinSeconds=600,silenceMaxSeconds=1200,fadeInSeconds,fadeOutSeconds,bufferSeconds;
    public int songsBeforeSilence=1;
    public JukeboxMode jukeboxMode=JukeboxMode.OFF;
    public boolean continuePlayingOnMatch,playImmediatelyOnJoin,combatEnabled=true,combatThreatsOnly=true;
    public boolean combatOnAttack=true,combatOnDamage=true,preventMusicWhileDead=true,replayGainEnabled=true;
    public double combatQuietSeconds=15,combatDeathFadeSeconds=1;
    public DeathMusic combatDeathMusic=DeathMusic.FADE;

    public static ProfilePlayback capture(Settings s){
        var p=new ProfilePlayback();p.songsBeforeSilence=s.songsBeforeSilence;
        p.silenceMinSeconds=s.silenceMinSeconds;p.silenceMaxSeconds=s.silenceMaxSeconds;
        p.fadeInSeconds=s.fadeInSeconds;p.fadeOutSeconds=s.fadeOutSeconds;p.bufferSeconds=s.bufferSeconds;
        p.jukeboxMode=s.jukeboxMode;p.continuePlayingOnMatch=s.continuePlayingOnMatch;p.playImmediatelyOnJoin=s.playImmediatelyOnJoin;
        p.combatEnabled=s.combatEnabled;p.combatThreatsOnly=s.combatThreatsOnly;
        p.combatOnAttack=s.combatOnAttack;p.combatOnDamage=s.combatOnDamage;p.preventMusicWhileDead=s.preventMusicWhileDead;
        p.replayGainEnabled=s.replayGainEnabled;p.combatQuietSeconds=s.combatQuietSeconds;
        p.combatDeathMusic=s.combatDeathMusic;p.combatDeathFadeSeconds=s.combatDeathFadeSeconds;return p;
    }
    public void apply(Settings s){
        s.songsBeforeSilence=songsBeforeSilence;
        s.silenceMinSeconds=silenceMinSeconds;s.silenceMaxSeconds=silenceMaxSeconds;
        s.fadeInSeconds=fadeInSeconds;s.fadeOutSeconds=fadeOutSeconds;s.bufferSeconds=bufferSeconds;
        s.jukeboxMode=jukeboxMode;s.continuePlayingOnMatch=continuePlayingOnMatch;s.playImmediatelyOnJoin=playImmediatelyOnJoin;
        s.combatEnabled=combatEnabled;s.combatThreatsOnly=combatThreatsOnly;
        s.combatOnAttack=combatOnAttack;s.combatOnDamage=combatOnDamage;s.preventMusicWhileDead=preventMusicWhileDead;
        s.replayGainEnabled=replayGainEnabled;s.combatQuietSeconds=combatQuietSeconds;
        s.combatDeathMusic=combatDeathMusic;s.combatDeathFadeSeconds=combatDeathFadeSeconds;
    }
}

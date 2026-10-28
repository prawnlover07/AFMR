package dev.animal.afmr.core;

/** One directional automatic-playback exception; unset controls inherit ordinary settings. */
public final class TransitionRule {
    public enum Finish { INHERIT, FINISH, INTERRUPT }
    public Finish finish=Finish.INHERIT;
    public boolean customSilence,customFadeIn,customFadeOut,customBuffer;
    public double silenceMinSeconds=0,silenceMaxSeconds=0,fadeInSeconds=0,fadeOutSeconds=0,bufferSeconds=0;
    public boolean inheritsAll(){return finish==Finish.INHERIT&&!customSilence&&!customFadeIn&&!customFadeOut&&!customBuffer;}
    public boolean valid(){
        return finish!=null&&bounded(silenceMinSeconds,86400)&&bounded(silenceMaxSeconds,86400)
            &&silenceMaxSeconds>=silenceMinSeconds&&bounded(fadeInSeconds,60)&&bounded(fadeOutSeconds,60)&&bounded(bufferSeconds,60);
    }
    private static boolean bounded(double value,double max){return Double.isFinite(value)&&value>=0&&value<=max;}
    public TransitionRule copy(){
        var r=new TransitionRule();r.finish=finish;r.customSilence=customSilence;r.customFadeIn=customFadeIn;
        r.customFadeOut=customFadeOut;r.customBuffer=customBuffer;r.silenceMinSeconds=silenceMinSeconds;
        r.silenceMaxSeconds=silenceMaxSeconds;r.fadeInSeconds=fadeInSeconds;r.fadeOutSeconds=fadeOutSeconds;r.bufferSeconds=bufferSeconds;return r;
    }
    public String summary(){
        if(inheritsAll())return "Default settings";
        return switch(finish){case INHERIT->"Inherit finish";case FINISH->"Finish song";case INTERRUPT->"Interrupt song";}
            +(customBuffer?" / buffer":"")+(customFadeOut?" / fade out":"")+(customSilence?" / silence":"")+(customFadeIn?" / fade in":"");
    }
}

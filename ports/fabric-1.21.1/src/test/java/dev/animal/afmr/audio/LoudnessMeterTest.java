package dev.animal.afmr.audio;

import org.junit.jupiter.api.*;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class LoudnessMeterTest {
    public static byte[] tone(int rate,int channels,double amplitude,double seconds){
        byte[] data=new byte[(int)(rate*seconds)*channels*2];
        for(int frame=0;frame<data.length/(channels*2);frame++){
            int sample=(int)Math.round(amplitude*32767*Math.sin(2*Math.PI*997*frame/rate));
            for(int c=0;c<channels;c++){int p=(frame*channels+c)*2;data[p]=(byte)sample;data[p+1]=(byte)(sample>>8);}
        }
        return data;
    }
    public static void wav(Path path,byte[] data,int rate,int channels) throws IOException {
        try(var stream=new AudioInputStream(new ByteArrayInputStream(data),new AudioFormat(rate,16,channels,true,false),data.length/(channels*2))){
            AudioSystem.write(stream,AudioFileFormat.Type.WAVE,path.toFile());
        }
    }
    private static LoudnessMeter.Measurement measure(byte[] data,int rate,int channels,int chunk) throws Exception {
        var meter=new LoudnessMeter();meter.reset(new AudioFormat(rate,16,channels,true,false));
        for(int start=0;start<data.length;start+=chunk){byte[] part=Arrays.copyOfRange(data,start,Math.min(data.length,start+chunk));meter.add(part,part.length);}
        return meter.finish();
    }
    @TestFactory List<DynamicTest> reference997HzToneAtCommonSampleRates(){
        return List.of(44100,48000,96000,192000).stream().map(rate->DynamicTest.dynamicTest(rate+" Hz",()->{
            var mono=measure(tone(rate,1,0.1,3),rate,1,8192);assertEquals(-23.01,mono.lufs(),0.06);assertEquals(0.1,mono.peak(),0.0001);
            var stereo=measure(tone(rate,2,0.1,3),rate,2,8192);assertEquals(mono.lufs()+10*Math.log10(2),stereo.lufs(),1e-9);
        })).toList();
    }
    @Test void differentStartingLevelsNormalizeToSameTargetAndChunksDoNotChangeMeasurement() throws Exception {
        for(double amplitude:List.of(0.025,0.4)){
            byte[] data=tone(48000,2,amplitude,3);var before=measure(data,48000,2,8192);
            assertEquals(before.lufs(),measure(data,48000,2,76).lufs(),1e-10);
            ReplayGain.apply(data,before.gain().multiplier());assertEquals(-18,measure(data,48000,2,8192).lufs(),0.01);
        }
    }
    @Test void absoluteAndRelativeGatesExcludeSilenceAndVeryQuietPassages() throws Exception {
        byte[] signal=tone(48000,2,0.2,6),quiet=tone(48000,2,0.002,6);
        byte[] combined=new byte[signal.length*3];System.arraycopy(signal,0,combined,signal.length,signal.length);
        System.arraycopy(quiet,0,combined,signal.length*2,quiet.length);
        assertEquals(measure(signal,48000,2,8192).lufs(),measure(combined,48000,2,8192).lufs(),0.25);
    }
    @Test void silenceShortClipsAndInvalidFormatsFailWithoutInfiniteGain() throws Exception {
        assertThrows(IOException.class,()->measure(new byte[48000*4],48000,2,8192));
        assertThrows(IOException.class,()->measure(tone(48000,1,0.1,0.1),48000,1,8192));
        var meter=new LoudnessMeter();assertThrows(IOException.class,()->meter.reset(new AudioFormat(Float.NaN,16,2,true,false)));
        assertThrows(IOException.class,()->meter.reset(new AudioFormat(48000,16,6,true,false)));
    }
    @Test void peaksLimitTheReportedEffectiveAdjustment(){
        var gain=new ReplayGain.Gain(12,0.8);assertEquals(20*Math.log10(1.25),gain.effectiveDb(),1e-10);
        assertTrue(gain.effectiveDb()<gain.db());
    }
    @TestFactory List<DynamicTest> mixedSpectrumAndGatingMatchIndependentFfmpegReferences(){
        // FFmpeg 7.1 ebur128, generated PCM16: silence, bass, treble, seeded noise, silence.
        int[] rates={8000,11025,22050,44100,48000,96000,192000};
        double[] mono={-19.7,-19.6,-19.5,-19.4,-19.4,-19.4,-19.4},stereo={-16.7,-16.6,-16.5,-16.4,-16.4,-16.4,-16.4};
        List<DynamicTest> tests=new ArrayList<>();
        for(int i=0;i<rates.length;i++){
            int rate=rates[i],index=i;tests.add(DynamicTest.dynamicTest(rate+" Hz mixed reference",()->{
                for(int channels=1;channels<=2;channels++){
                    byte[] data=new byte[rate*channels*2*14];int seed=12345;
                    for(int frame=0;frame<rate*14;frame++)for(int c=0;c<channels;c++){
                        double t=(double)frame/rate;seed=1664525*seed+1013904223;
                        double x=t<2||t>=12?0:t<6?0.2*Math.sin(2*Math.PI*100*t):t<10?0.05*Math.sin(2*Math.PI*3000*t):0.2*(seed/(double)Integer.MAX_VALUE);
                        int sample=(int)Math.round(32767*x),p=(frame*channels+c)*2;data[p]=(byte)sample;data[p+1]=(byte)(sample>>8);
                    }
                    assertEquals(channels==1?mono[index]:stereo[index],measure(data,rate,channels,16384).lufs(),0.08);
                }
            }));
        }
        return tests;
    }
}

package dev.animal.afmr.audio;

import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;

/** Streaming mono/stereo BS.1770 K weighting and 400-ms gates, with a bounded 0.01-LU histogram. */
public final class LoudnessMeter {
    public static final double TARGET_LUFS = -18;
    private static final int BINS = 10001;
    private static final double ABSOLUTE_ENERGY = Math.pow(10,(-70+0.691)/10);
    private final double[] energies = new double[BINS], quarters = new double[4];
    private final long[] counts = new long[BINS];
    private final Filter[] shelf = new Filter[2], highPass = new Filter[2];
    private int channels, quarterFrames, position, quarterIndex;
    private long frames, quarterCount, blocks;
    private double quarterEnergy, peak;

    public void reset(AudioFormat format) throws IOException {
        double rate=format.getSampleRate();channels=format.getChannels();
        if(!Double.isFinite(rate)||rate<8000||rate>384000||channels<1||channels>2
            ||format.getSampleSizeInBits()!=16||format.isBigEndian()||!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding()))
            throw new IOException("Unsupported loudness-analysis PCM format: "+format);
        quarterFrames=(int)Math.round(rate/10);position=quarterIndex=0;frames=quarterCount=blocks=0;quarterEnergy=peak=0;
        Arrays.fill(energies,0);Arrays.fill(counts,0);Arrays.fill(quarters,0);
        for(int c=0;c<channels;c++){
            // Bilinear-transform parameters reproduce the BS.1770 48-kHz reference response at each rate.
            double k=Math.tan(Math.PI*1681.974450955533/rate),q=0.7071752369554196;
            double h=Math.pow(10,3.999843853973347/20),m=Math.pow(h,0.4996667741545416),d=1+k/q+k*k;
            shelf[c]=new Filter((h+m*k/q+k*k)/d,2*(k*k-h)/d,(h-m*k/q+k*k)/d,2*(k*k-1)/d,(1-k/q+k*k)/d);
            k=Math.tan(Math.PI*38.13547087602444/rate);q=0.5003270373238773;d=1+k/q+k*k;
            highPass[c]=new Filter(1,-2,1,2*(k*k-1)/d,(1-k/q+k*k)/d);
        }
    }
    public void add(byte[] pcm,int length) throws IOException {
        int stride=channels*2;if(length%stride!=0)throw new IOException("Incomplete decoded audio frame");
        for(int i=0;i<length;i+=stride){
            for(int c=0;c<channels;c++){
                int p=i+c*2;double sample=(short)((pcm[p]&255)|(pcm[p+1]<<8))/32768.0;
                peak=Math.max(peak,Math.abs(sample));
                double weighted=highPass[c].apply(shelf[c].apply(sample));quarterEnergy+=weighted*weighted;
            }
            frames++;
            if(++position==quarterFrames){
                quarters[quarterIndex]=quarterEnergy;quarterIndex=(quarterIndex+1)%4;quarterCount++;
                position=0;quarterEnergy=0;
                if(quarterCount>=4){
                    double energy=(quarters[0]+quarters[1]+quarters[2]+quarters[3])/(4*quarterFrames);blocks++;
                    if(energy>ABSOLUTE_ENERGY){
                        double lu=-0.691+10*Math.log10(energy);
                        int bin=Math.max(0,Math.min((int)((lu+70)*100),BINS-1));energies[bin]+=energy;counts[bin]++;
                    }
                }
            }
        }
    }
    public record Measurement(double lufs,double peak) {
        public ReplayGain.Gain gain() { return new ReplayGain.Gain(TARGET_LUFS-lufs,peak); }
    }
    public Measurement finish() throws IOException {
        if(blocks==0)throw new IOException("Audio is shorter than 400 ms");
        double sum=0;long count=0;
        for(int i=0;i<BINS;i++){sum+=energies[i];count+=counts[i];}
        if(count==0||peak==0)throw new IOException("No measurable loudness (silent or below -70 LUFS)");
        double threshold=Math.max(ABSOLUTE_ENERGY,sum/count/10);sum=0;count=0;
        // Gate by each bin's actual mean energy, keeping the approximation within a 0.01-LU bin.
        for(int i=0;i<BINS;i++)if(counts[i]>0&&energies[i]/counts[i]>threshold){sum+=energies[i];count+=counts[i];}
        if(count==0)throw new IOException("No audio above the loudness gate");
        return new Measurement(-0.691+10*Math.log10(sum/count),peak);
    }
    public long frames() { return frames; }
    private static final class Filter {
        final double b0,b1,b2,a1,a2;double z1,z2;
        Filter(double b0,double b1,double b2,double a1,double a2){this.b0=b0;this.b1=b1;this.b2=b2;this.a1=a1;this.a2=a2;}
        double apply(double x){double y=b0*x+z1;z1=b1*x-a1*y+z2;z2=b2*x-a2*y;return y;}
    }
}

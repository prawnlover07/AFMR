package dev.animal.afmr.core;

/** Imported tag values, distinct from AFMR's measured loudness and the applied playback gain. */
public final class ReplayGainInfo {
    // Immutable fields are readable by Minecraft's bundled Gson 2.8.9.
    private final Double trackDb;
    private final Double trackPeak;
    private final Double albumDb;
    private final Double albumPeak;
    public ReplayGainInfo(Double trackDb,Double trackPeak,Double albumDb,Double albumPeak){
        this.trackDb=trackDb;
        this.trackPeak=trackPeak;
        this.albumDb=albumDb;
        this.albumPeak=albumPeak;
    }
    public Double trackDb(){return trackDb;}
    public Double trackPeak(){return trackPeak;}
    public Double albumDb(){return albumDb;}
    public Double albumPeak(){return albumPeak;}
    @Override public boolean equals(Object value){
        return value instanceof ReplayGainInfo other
            &&java.util.Objects.equals(trackDb,other.trackDb)
            &&java.util.Objects.equals(trackPeak,other.trackPeak)
            &&java.util.Objects.equals(albumDb,other.albumDb)
            &&java.util.Objects.equals(albumPeak,other.albumPeak);
    }
    @Override public int hashCode(){return java.util.Objects.hash(trackDb,trackPeak,albumDb,albumPeak);}
}

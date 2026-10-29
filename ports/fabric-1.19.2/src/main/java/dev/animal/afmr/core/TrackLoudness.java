package dev.animal.afmr.core;

/** AFMR-owned measurement of decoded audio; shared across playlists and immutable save snapshots. */
public final class TrackLoudness {
    // Immutable fields are readable by Minecraft's bundled Gson 2.8.9.
    private final long size;
    private final String modifiedTime;
    private final double gainDb;
    private final double peak;
    public TrackLoudness(long size,String modifiedTime,double gainDb,double peak){
        this.size=size;
        this.modifiedTime=modifiedTime;
        this.gainDb=gainDb;
        this.peak=peak;
    }
    public long size(){return size;}
    public String modifiedTime(){return modifiedTime;}
    public double gainDb(){return gainDb;}
    public double peak(){return peak;}
    @Override public boolean equals(Object value){
        return value instanceof TrackLoudness other
            &&size==other.size
            &&java.util.Objects.equals(modifiedTime,other.modifiedTime)
            &&Double.compare(gainDb,other.gainDb)==0
            &&Double.compare(peak,other.peak)==0;
    }
    @Override public int hashCode(){return java.util.Objects.hash(size,modifiedTime,gainDb,peak);}
    public boolean valid() {
        return size > 0 && modifiedTime != null && Double.isFinite(gainDb) && Math.abs(gainDb) <= 100
            && Double.isFinite(peak) && peak > 0 && peak <= 1;
    }
    public boolean matches(long bytes, String modified) { return valid() && size == bytes && modifiedTime.equals(modified); }
}

package dev.animal.afmr.core;

/** AFMR-owned measurement of decoded audio; shared across playlists and immutable save snapshots. */
public record TrackLoudness(long size, String modifiedTime, double gainDb, double peak) {
    public boolean valid() {
        return size > 0 && modifiedTime != null && Double.isFinite(gainDb) && Math.abs(gainDb) <= 100
            && Double.isFinite(peak) && peak > 0 && peak <= 1;
    }
    public boolean matches(long bytes, String modified) { return valid() && size == bytes && modifiedTime.equals(modified); }
}

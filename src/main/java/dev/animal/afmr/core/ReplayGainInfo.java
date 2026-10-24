package dev.animal.afmr.core;

/** Imported tag values, distinct from AFMR's measured loudness and the applied playback gain. */
public record ReplayGainInfo(Double trackDb, Double trackPeak, Double albumDb, Double albumPeak) {}

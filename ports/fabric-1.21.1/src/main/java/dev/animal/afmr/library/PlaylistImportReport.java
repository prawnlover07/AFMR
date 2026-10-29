package dev.animal.afmr.library;

import java.util.List;

public record PlaylistImportReport(String destination,int addedToPlaylist,int alreadyAssigned,int newTracks,
                                   int skipped,int duplicates,int reused,List<String> sources,List<String> details) {}

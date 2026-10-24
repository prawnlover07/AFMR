package dev.animal.afmr.library;

import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.audio.ReplayGain;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;

public final class MusicFiles {
    private static final Set<String> EXTENSIONS = Set.of("mp3", "flac", "ogg", "wav");
    public record Request(Path path, String playlistId, boolean recursive) {}
    public record Found(Track track,String playlistId,Path sourcePath) {}
    public record Result(List<Found> found,List<String> errors,List<String> playlists,int skipped,int duplicates,int reused) {}
    private record WorkFile(Path path,String playlistId,boolean reuseMetadata) {}
    public enum ScanStage { DISCOVERING, READING_TAGS, CHECKING, REPAIRING, READING_PRESET, WRITING_PRESET, FINALIZING }
    /** Immutable snapshots can be published directly from the scanner to the render thread. */
    public record ScanProgress(ScanStage stage, int discovered, int processed, String currentPath) {
        public static ScanProgress starting() { return new ScanProgress(ScanStage.DISCOVERING, 0, 0, ""); }
        public double fraction() { return discovered == 0 ? (stage == ScanStage.FINALIZING ? 1 : 0) : (double)processed / discovered; }
    }

    public static boolean supported(Path path) {
        String name = path.getFileName().toString();
        return EXTENSIONS.contains(name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT));
    }
    public static Result scan(List<Request> requests,List<Track> knownTracks,Consumer<ScanProgress> progress) {
        List<Found> found = new ArrayList<>(); List<String> errors = new ArrayList<>();
        List<WorkFile> files = new ArrayList<>();List<String> playlists=new ArrayList<>();
        int skipped=0,duplicates=0,reused=0;
        Map<Path,Track> known=new HashMap<>();
        for(Track track:knownTracks)if(!track.vanilla)known.put(Path.of(track.path).toAbsolutePath().normalize(),track);
        progress.accept(ScanProgress.starting());
        for (Request request : requests) {
            try {
                Path path = request.path().toAbsolutePath().normalize();
                if (Files.isDirectory(path)) {
                    // Do not follow symbolic directory links: a folder cannot accidentally recurse forever.
                    try (var paths = Files.walk(path, request.recursive() ? Integer.MAX_VALUE : 1)) {
                        var entries = paths.iterator();
                        while (entries.hasNext()) {
                            Path entry = entries.next();
                            if (Files.isRegularFile(entry) && supported(entry))
                                files.add(new WorkFile(entry, request.playlistId(), false));
                            progress.accept(new ScanProgress(ScanStage.DISCOVERING, files.size(), 0, entry.toString()));
                        }
                    }
                } else if (Files.isRegularFile(path) && FpliteFiles.supported(path)) {
                    int[] unsupported={0};
                    var parsed=FpliteFiles.read(path,entry->{
                        if(supported(entry))files.add(new WorkFile(entry,request.playlistId(),true));else unsupported[0]++;
                        progress.accept(new ScanProgress(ScanStage.DISCOVERING,files.size(),0,path.toString()));
                    });
                    playlists.add(parsed.name());skipped+=parsed.skippedCount()+unsupported[0];duplicates+=parsed.duplicates();
                    for(var issue:parsed.skipped().entrySet())if(errors.size()<100)
                        errors.add(parsed.name()+": skipped "+issue.getValue()+" "+issue.getKey().description);
                    if(unsupported[0]>0&&errors.size()<100)errors.add(parsed.name()+": skipped "+unsupported[0]+" unsupported audio formats");
                } else if (Files.isRegularFile(path) && supported(path)) {
                    files.add(new WorkFile(path, request.playlistId(), false));
                    progress.accept(new ScanProgress(ScanStage.DISCOVERING, files.size(), 0, path.toString()));
                }
                else {skipped++;if(errors.size()<100)errors.add(path + ": missing path or unsupported audio format");}
            } catch (Exception e) {skipped++;if(errors.size()<100)errors.add(request.path() + ": " + e.getMessage());}
        }
        files.sort(Comparator.comparing(WorkFile::path));
        Map<String,Set<String>> imported=new HashMap<>();
        for (int i = 0; i < files.size(); i++) {
            WorkFile file = files.get(i);
            progress.accept(new ScanProgress(ScanStage.READING_TAGS, files.size(), i, file.path().toString()));
            try{
                Track existing=known.get(file.path());
                Track cached=file.reuseMetadata()?existing:null;
                if(!Files.isRegularFile(file.path()))throw new IOException("File missing or unavailable");
                Track track=cached==null?read(file.path()):cached;
                // A repair preserves identity even though it changes the path used for future rescans.
                if(existing==null&&cached==null)existing=known.get(Path.of(track.path));
                if(existing!=null)track.id=existing.id;
                if(file.reuseMetadata()&&cached==null)known.put(file.path(),track);
                if(imported.computeIfAbsent(file.playlistId(),unused->new HashSet<>()).add(track.id)){
                    found.add(new Found(track,file.playlistId(),file.path()));if(cached!=null)reused++;
                }else duplicates++;
            }catch(Exception e){skipped++;if(errors.size()<100)errors.add(file.path()+": "+e.getMessage());}
            progress.accept(new ScanProgress(ScanStage.READING_TAGS, files.size(), i + 1, file.path().toString()));
        }
        progress.accept(new ScanProgress(ScanStage.FINALIZING, files.size(), files.size(), ""));
        return new Result(List.copyOf(found),List.copyOf(errors),List.copyOf(playlists),skipped,duplicates,reused);
    }
    static Track read(Path file) throws IOException {
            Path canonical = file.toRealPath();
            String id = "file:" + UUID.nameUUIDFromBytes(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String name = canonical.getFileName().toString();
            Track track = new Track(id, canonical.toString(), name.substring(0, name.lastIndexOf('.')), false);
            try {
                var audioFile=AudioFileIO.read(canonical.toFile());
                track.durationSeconds=audioFile.getAudioHeader().getPreciseTrackLength();
                var tag = audioFile.getTag();
                if (tag != null) {
                    String title = clean(tag.getFirst(FieldKey.TITLE));
                    if (!title.isBlank()) track.title = title;
                    track.artist = clean(tag.getFirst(FieldKey.ARTIST)); track.album = clean(tag.getFirst(FieldKey.ALBUM));
                    track.albumArtist=clean(tag.getFirst(FieldKey.ALBUM_ARTIST));
                    track.trackNumber=clean(tag.getFirst(FieldKey.TRACK));track.discNumber=clean(tag.getFirst(FieldKey.DISC_NO));
                    track.genre=clean(tag.getFirst(FieldKey.GENRE));track.date=clean(tag.getFirst(FieldKey.YEAR));
                    track.replayGain=ReplayGain.metadata(tag);
                }
            } catch (Exception ignored) { /* Untagged audio remains importable. Playback verifies the audio. */ }
            return track;
    }
    private static String clean(String value) { return value == null ? "" : value.replaceAll("\\p{Cc}", "").strip(); }

    /** Returns a bounded PNG, since Minecraft's native image reader expects PNG data. */
    public static byte[] cover(Track track) {
        if (track == null || track.vanilla) return null;
        try {
            Path path = Path.of(track.path); BufferedImage image = null;
            try {
                var tag = AudioFileIO.read(path.toFile()).getTag();
                var art = tag == null ? null : tag.getFirstArtwork();
                if (art != null && art.getBinaryData().length <= 16 * 1024 * 1024)
                    image = ImageIO.read(new ByteArrayInputStream(art.getBinaryData()));
            } catch (Exception ignored) {}
            if (image == null) for (String name : List.of("cover.jpg", "folder.jpg", "cover.png", "folder.png", "Cover.jpg", "Folder.jpg")) {
                Path candidate = path.resolveSibling(name);
                if (Files.isRegularFile(candidate) && Files.size(candidate) <= 16 * 1024 * 1024) {
                    image = ImageIO.read(candidate.toFile()); if (image != null) break;
                }
            }
            if (image == null) return null;
            BufferedImage small = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            var graphics = small.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double scale = Math.min(256.0 / image.getWidth(), 256.0 / image.getHeight());
            int w = (int)(image.getWidth() * scale), h = (int)(image.getHeight() * scale);
            graphics.drawImage(image, (256-w)/2, (256-h)/2, w, h, null); graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream(); ImageIO.write(small, "png", output);
            return output.toByteArray();
        } catch (Exception ignored) { return null; }
    }
}

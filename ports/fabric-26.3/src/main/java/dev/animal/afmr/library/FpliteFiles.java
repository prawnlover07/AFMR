package dev.animal.afmr.library;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Reads foobar2000's UTF-8 path lists and adjacent name index. Never writes either file. */
public final class FpliteFiles {
    public enum Skip {
        ARCHIVE("archive-backed entries"), PROTOCOL("non-file locations"),
        SUBSONG("subsong/chapter entries"), INVALID("invalid file locations");
        public final String description;
        Skip(String description){this.description=description;}
    }
    public record Parsed(String name,int entries,int duplicates,Map<Skip,Integer> skipped) {
        public int skippedCount(){int total=0;for(int count:skipped.values())total+=count;return total;}
    }
    public static boolean supported(Path file){return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".fplite");}

    public static Parsed read(Path source,Consumer<Path> track) throws IOException {
        Path file=source.toAbsolutePath().normalize();
        if(!supported(file))throw new IOException("Choose a .fplite file; binary .fpl files are not supported");
        if(Files.size(file)>64L*1024*1024)throw new IOException("Playlist exceeds the 64 MiB import limit");
        Set<Path> seen=new HashSet<>();Map<Skip,Integer> skipped=new EnumMap<>(Skip.class);
        int entries=0,duplicates=0,lineNumber=0;
        try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){
            String line;
            while((line=reader.readLine())!=null){
                if(lineNumber++==0&&line.startsWith("\uFEFF"))line=line.substring(1);
                if(line.isBlank())continue;
                entries++;
                if(line.regionMatches(true,0,"unpack://",0,9)){skipped.merge(Skip.ARCHIVE,1,Integer::sum);continue;}
                if(line.contains("://")&&!line.regionMatches(true,0,"file://",0,7)){skipped.merge(Skip.PROTOCOL,1,Integer::sum);continue;}
                if(line.indexOf('|')>=0){skipped.merge(Skip.SUBSONG,1,Integer::sum);continue;}
                Path path;
                try{path=location(file.getParent(),line);}
                catch(IllegalArgumentException e){skipped.merge(Skip.INVALID,1,Integer::sum);continue;}
                if(!seen.add(path)){duplicates++;continue;}
                track.accept(path);
            }
        }
        return new Parsed(name(file),entries,duplicates,Map.copyOf(skipped));
    }
    private static Path location(Path base,String line){
        Path path;
        if(line.regionMatches(true,0,"file://",0,7)){
            String raw=line.substring(7);
            if(raw.isBlank())throw new IllegalArgumentException("Empty file location");
            boolean drive=raw.length()>2&&Character.isLetter(raw.charAt(0))&&raw.charAt(1)==':'&&(raw.charAt(2)=='\\'||raw.charAt(2)=='/');
            // foobar's native file://C:\... spelling is a literal path: '%' and '+' stay literal.
            if(drive||raw.startsWith("\\\\"))path=Path.of(raw);
            else path=Path.of(URI.create("file://"+raw.replace(" ","%20")));
        }else path=Path.of(line);
        return (path.isAbsolute()?path:base.resolve(path)).toAbsolutePath().normalize();
    }
    private static String name(Path file){
        String name=file.getFileName().toString(),stem=name.substring(0,name.length()-7);
        if(!stem.regionMatches(true,0,"playlist-",0,9))return stem;
        String id=stem.substring(9);if(id.length()!=36)return stem;
        Path index=file.resolveSibling("index.txt");
        if(!Files.isRegularFile(index))return stem;
        try(var reader=Files.newBufferedReader(index,StandardCharsets.UTF_8)){
            String line;
            while((line=reader.readLine())!=null){
                if(line.startsWith("\uFEFF"))line=line.substring(1);
                int colon=line.indexOf(':');
                if(colon==36&&line.substring(0,colon).equalsIgnoreCase(id)){
                    String title=line.substring(colon+1).replaceAll("\\p{Cc}","").strip();
                    return title.isBlank()?stem:title;
                }
            }
        }catch(IOException ignored){/* The adjacent index is optional; the selected file remains importable. */}
        return stem;
    }
}

package dev.animal.afmr.library;

import com.google.gson.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Bounded user font assets and the AFMR-owned Minecraft resource pack. No gameplay I/O. */
public final class FontFiles {
    public static final int MAX_BYTES=16*1024*1024;
    public static final String PACK="afmr-generated-fonts";
    public record Asset(byte[] bytes,String name){}
    public static Asset read(Path source)throws IOException{
        if(!source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ttf"))throw new IOException("Choose a TrueType font (.ttf).");
        if(Files.size(source)>MAX_BYTES)throw new IOException("Font exceeds 16 MiB.");
        byte[] bytes;try(var stream=Files.newInputStream(source)){bytes=stream.readNBytes(MAX_BYTES+1);}validate(bytes);
        try{var font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,new ByteArrayInputStream(bytes));return new Asset(bytes,font.getFontName(Locale.ROOT));}
        catch(java.awt.FontFormatException ex){throw new IOException("Invalid TrueType font.",ex);}
    }
    public static void validate(byte[] bytes)throws IOException{
        if(bytes.length<12||bytes.length>MAX_BYTES)throw new IOException("Invalid font size.");
        var data=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        if(data.getInt(0)!=0x00010000&&data.getInt(0)!=0x74727565)throw new IOException("Font must use TrueType outlines.");
        int tables=Short.toUnsignedInt(data.getShort(4));if(tables<1||tables>512||12+tables*16>bytes.length)throw new IOException("Invalid font table directory.");
        var tags=new HashSet<Integer>();
        for(int i=0;i<tables;i++){int at=12+i*16;int tag=data.getInt(at);long offset=Integer.toUnsignedLong(data.getInt(at+8)),size=Integer.toUnsignedLong(data.getInt(at+12));
            if(!tags.add(tag)||offset>bytes.length||size>bytes.length-offset)throw new IOException("Invalid font table bounds.");
        }
        if(!tags.contains(0x676C7966)||!tags.contains(0x636D6170)||!tags.contains(0x68656164))throw new IOException("Font lacks TrueType glyph tables.");
    }
    public static void install(Path target,Asset asset)throws IOException{validate(asset.bytes);write(target,asset.bytes);}
    public static Path packDirectory(Path packs)throws IOException{
        Path root=packs.toAbsolutePath().normalize().resolve(PACK),marker=root.resolve(".afmr-owned");
        if(Files.isSymbolicLink(root)||Files.exists(root)&&(!Files.isRegularFile(marker)||Files.isSymbolicLink(marker)||Files.size(marker)!=PACK.length()||!Files.readString(marker).equals(PACK)))throw new IOException("AFMR font pack directory is already in use.");
        if(Files.isSymbolicLink(root.resolve("assets"))||Files.isSymbolicLink(root.resolve("assets/afmr"))||Files.isSymbolicLink(root.resolve("assets/afmr/font")))throw new IOException("Invalid font pack directory.");
        return root;
    }
    public static void buildPack(Path config,Path packs,String metadata)throws IOException{
        Path root=packDirectory(packs);Files.createDirectories(root);Files.writeString(root.resolve(".afmr-owned"),PACK);
        Path fonts=root.resolve("assets/afmr/font");
        Files.createDirectories(fonts);
        for(String target:List.of("menu","widget")){
            Path source=config.resolve(target+"-font.ttf");var providers=new JsonArray();
            if(Files.isRegularFile(source)){
                byte[] bytes=readBytes(source);validate(bytes);write(fonts.resolve(target+".ttf"),bytes);
                var ttf=new JsonObject();ttf.addProperty("type","ttf");ttf.addProperty("file","afmr:"+target+".ttf");ttf.addProperty("size",11);ttf.addProperty("oversample",2);providers.add(ttf);
            }else Files.deleteIfExists(fonts.resolve(target+".ttf"));
            var fallback=new JsonObject();fallback.addProperty("type","reference");fallback.addProperty("id","minecraft:default");providers.add(fallback);
            var definition=new JsonObject();definition.add("providers",providers);write(fonts.resolve("custom_"+target+".json"),definition.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        write(root.resolve("pack.mcmeta"),metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static byte[] readBytes(Path source)throws IOException{
        if(Files.size(source)>MAX_BYTES)throw new IOException("Font exceeds 16 MiB.");
        try(var stream=Files.newInputStream(source)){byte[] bytes=stream.readNBytes(MAX_BYTES+1);if(bytes.length>MAX_BYTES)throw new IOException("Font exceeds 16 MiB.");return bytes;}
    }
    public static boolean currentPack(Path config,Path packs,String metadata)throws IOException{
        Path root=packDirectory(packs),file=root.resolve("pack.mcmeta");
        if(!Files.isRegularFile(file)||Files.size(file)>4096||!Files.readString(file).equals(metadata))return false;
        for(String target:List.of("menu","widget")){
            Path source=config.resolve(target+"-font.ttf"),copy=root.resolve("assets/afmr/font/"+target+".ttf");
            if(!Files.isRegularFile(root.resolve("assets/afmr/font/custom_"+target+".json")))return false;
            if(Files.isRegularFile(source)!=Files.isRegularFile(copy))return false;
            if(Files.isRegularFile(source)&&(Files.size(source)>MAX_BYTES||Files.size(source)!=Files.size(copy)||Files.mismatch(source,copy)!=-1))return false;
        }return true;
    }
    private static void write(Path target,byte[] bytes)throws IOException{
        Files.createDirectories(target.getParent());Path temporary=Files.createTempFile(target.getParent(),"afmr-font-",".tmp");
        try{Files.write(temporary,bytes);try{Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temporary);}
    }
}

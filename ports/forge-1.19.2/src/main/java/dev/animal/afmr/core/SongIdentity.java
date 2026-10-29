package dev.animal.afmr.core;

/** Identity of the complete file bytes, independent of path/name; tag changes alter it. */
public final class SongIdentity {
    // Immutable fields are readable by Minecraft's bundled Gson 2.8.9.
    private final long size;
    private final String sha256;
    public SongIdentity(long size,String sha256){
        if(size<0||!validHash(sha256))throw new IllegalArgumentException("Invalid song content identity");
        this.size=size;
        this.sha256=sha256;
    }
    public long size(){return size;}
    public String sha256(){return sha256;}
    @Override public boolean equals(Object value){
        return value instanceof SongIdentity other
            &&size==other.size
            &&java.util.Objects.equals(sha256,other.sha256);
    }
    @Override public int hashCode(){return java.util.Objects.hash(size,sha256);}
    public static boolean validHash(String hash){
        if(hash==null||hash.length()!=64)return false;
        for(int i=0;i<64;i++){char c=hash.charAt(i);if(!(c>='0'&&c<='9'||c>='a'&&c<='f'))return false;}
        return true;
    }
}

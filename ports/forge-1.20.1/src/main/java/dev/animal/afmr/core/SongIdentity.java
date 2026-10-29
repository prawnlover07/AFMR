package dev.animal.afmr.core;

/** Identity of the complete file bytes, independent of path/name; tag changes alter it. */
public record SongIdentity(long size,String sha256) {
    public SongIdentity {
        if(size<0||!validHash(sha256))throw new IllegalArgumentException("Invalid song content identity");
    }
    public static boolean validHash(String hash){
        if(hash==null||hash.length()!=64)return false;
        for(int i=0;i<64;i++){char c=hash.charAt(i);if(!(c>='0'&&c<='9'||c>='a'&&c<='f'))return false;}
        return true;
    }
}

package dev.animal.afmr.core;

import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;

/** Immutable, client-visible distance snapshot. Neither coordinates nor identities enter diagnostics. */
public final class NearbyPlayers {
    public static final NearbyPlayers EMPTY=new NearbyPlayers(Map.of());
    private final Map<String,Double> distancesSquared;
    public NearbyPlayers(Map<String,Double> distancesSquared){
        var copy=new TreeMap<String,Double>(String.CASE_INSENSITIVE_ORDER);copy.putAll(distancesSquared);
        this.distancesSquared=Collections.unmodifiableMap(copy);
    }
    public boolean matches(String uuid,String name,double radius){
        if(!Double.isFinite(radius)||radius<1||radius>1024)return false;
        String key;
        if(!uuid.isBlank()){
            if(!validUuid(uuid))return false;
            key=uuid;
        }else{
            if(!validName(name))return false;
            key=name;
        }
        Double distance=distancesSquared.get(key);
        return distance!=null&&Double.isFinite(distance)&&distance>=0&&distance<=radius*radius;
    }
    public static boolean validName(String name){
        if(name==null||name.isEmpty()||name.length()>16)return false;
        for(int i=0;i<name.length();i++){char c=name.charAt(i);if(!(c>='a'&&c<='z'||c>='A'&&c<='Z'||c>='0'&&c<='9'||c=='_'))return false;}
        return true;
    }
    public static boolean validUuid(String uuid){
        if(uuid==null||uuid.length()!=36)return false;
        for(int i=0;i<36;i++){
            char c=uuid.charAt(i);
            if(i==8||i==13||i==18||i==23){if(c!='-')return false;}
            else if(!(c>='0'&&c<='9'||c>='a'&&c<='f'||c>='A'&&c<='F'))return false;
        }
        return true;
    }
    @Override public String toString(){return "NearbyPlayers[identities omitted]";}
}

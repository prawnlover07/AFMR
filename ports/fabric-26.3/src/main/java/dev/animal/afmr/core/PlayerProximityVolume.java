package dev.animal.afmr.core;

/** Two linear distance/gain endpoints; percentages scale the source playlist's volume. */
public final class PlayerProximityVolume {
    public boolean enabled;
    public double nearDistance=4,farDistance=32,nearVolume=1,farVolume=0;
    public float gain(double distance){
        if(!enabled)return 1;
        if(!Double.isFinite(distance))return (float)farVolume;
        double fraction=Math.max(0,Math.min(1,(distance-nearDistance)/(farDistance-nearDistance)));
        return (float)(nearVolume+(farVolume-nearVolume)*fraction);
    }
    public boolean valid(){
        return Double.isFinite(nearDistance)&&Double.isFinite(farDistance)&&nearDistance>=0&&farDistance<=1024&&farDistance>nearDistance
            &&Double.isFinite(nearVolume)&&Double.isFinite(farVolume)&&nearVolume>=0&&nearVolume<=1&&farVolume>=0&&farVolume<=1;
    }
    public void normalize(){
        nearDistance=bounded(nearDistance,0,Math.nextDown(1024d),4);farDistance=bounded(farDistance,Math.nextUp(nearDistance),1024,32);
        nearVolume=bounded(nearVolume,0,1,1);farVolume=bounded(farVolume,0,1,0);
    }
    private static double bounded(double value,double min,double max,double fallback){
        return Math.max(min,Math.min(max,Double.isFinite(value)?value:fallback));
    }
    public PlayerProximityVolume copy(){
        var copy=new PlayerProximityVolume();copy.enabled=enabled;copy.nearDistance=nearDistance;copy.farDistance=farDistance;
        copy.nearVolume=nearVolume;copy.farVolume=farVolume;return copy;
    }
}

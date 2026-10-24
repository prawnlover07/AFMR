package dev.animal.afmr.core;

/** Immutable looping frame clock. Rendering only selects an already uploaded texture. */
public final class MenuImageAnimation {
    private final long[] ends;
    private final long duration;
    public MenuImageAnimation(int[] millis){
        if(millis.length==0)throw new IllegalArgumentException("Animation has no frames");
        ends=new long[millis.length];long sum=0;
        for(int i=0;i<millis.length;i++){
            if(millis[i]<1)throw new IllegalArgumentException("Frame has no duration");
            ends[i]=sum+=millis[i];
        }
        duration=sum;
    }
    public int frame(long elapsedMillis){
        long time=Math.max(0,elapsedMillis)%duration;
        int low=0,high=ends.length-1;
        while(low<high){int middle=(low+high)>>>1;if(time<ends[middle])high=middle;else low=middle+1;}
        return low;
    }
}

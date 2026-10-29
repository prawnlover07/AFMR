package dev.animal.afmr.core;

import java.util.List;
import java.util.Set;

/** Owned observation of nearby generated structures, never live server objects. */
public record StructureContext(boolean available,List<Bounds> bounds) {
    public static final int MAX_RADIUS=128;
    public static final StructureContext UNAVAILABLE=new StructureContext(false,List.of());
    public StructureContext { bounds=List.copyOf(bounds); }

    public record Bounds(String id,int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        /** Minecraft structure bounds include both end blocks. Use the player's block position. */
        public double distanceSquared(double x,double y,double z){
            x=Math.floor(x);y=Math.floor(y);z=Math.floor(z);
            double dx=Math.max(0,Math.max(minX-x,x-maxX));
            double dy=Math.max(0,Math.max(minY-y,y-maxY));
            double dz=Math.max(0,Math.max(minZ-z,z-maxZ));
            return dx*dx+dy*dy+dz*dz;
        }
    }

    public boolean matches(Set<String> selected,double radius,double x,double y,double z){
        if(!available)return false;
        double limit=radius*radius;
        for(int i=0;i<bounds.size();i++){
            var box=bounds.get(i);
            if(selected.contains(box.id())&&box.distanceSquared(x,y,z)<=limit)return true;
        }
        return false;
    }
}

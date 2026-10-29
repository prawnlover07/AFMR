package dev.animal.afmr.core;

import java.util.Set;

/** Client-owned request lifetime and expiring server observations, independent of the transport. */
public final class ServerConditionSession {
    private long token;
    private double nextRequest,sentAt,receivedAt;
    private boolean combat,pending,targeted;
    private int radius=-1;
    private StructureContext structures=StructureContext.UNAVAILABLE;
    private Set<String> catalog=Set.of();

    public void clear(){token++;nextRequest=receivedAt=0;pending=combat=targeted=false;radius=-1;structures=StructureContext.UNAVAILABLE;catalog=Set.of();}
    public long token(){return token;}
    public Set<String> catalog(){return catalog;}
    /** At most one outstanding request, with a two-second timeout. Idle clients send nothing. */
    public boolean request(double now,int requestedRadius,boolean requestedCombat){
        requestedRadius=Math.max(-1,Math.min(requestedRadius,StructureContext.MAX_RADIUS));
        boolean changed=radius!=requestedRadius||combat!=requestedCombat;
        if(changed){
            if(radius!=requestedRadius)structures=StructureContext.UNAVAILABLE;
            if(combat!=requestedCombat)targeted=false;
            radius=requestedRadius;combat=requestedCombat;nextRequest=0;pending=false;
        }
        if(radius<0&&!combat&&!changed)return false;
        if(now<nextRequest||pending&&now-sentAt<2)return false;
        token++;nextRequest=now+0.5;sentAt=now;pending=true;return true;
    }
    public boolean receive(long responseToken,double now,boolean hostileTarget,StructureContext bounds,Set<String> ids){
        if(!pending||responseToken!=token)return false;
        pending=false;receivedAt=now;structures=bounds;targeted=hostileTarget;
        if(!ids.isEmpty())catalog=Set.copyOf(ids);
        return true;
    }
    public StructureContext structures(double now){return radius>=0&&now-receivedAt<=2?structures:StructureContext.UNAVAILABLE;}
    public boolean targeted(double now){return combat&&now-receivedAt<=2&&targeted;}
}

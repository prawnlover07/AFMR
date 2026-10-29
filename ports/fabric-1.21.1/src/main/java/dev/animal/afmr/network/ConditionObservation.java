package dev.animal.afmr.network;

import dev.animal.afmr.core.StructureContext;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded, immutable server observation; no entities, library settings or music cross the wire. */
public record ConditionObservation(long token,String dimension,int entityId,boolean targeted,
    StructureContext structures,Set<String> catalog) implements CustomPacketPayload {
    public static final int MAX_BOXES=256,MAX_CATALOG=2048;
    public static final Type<ConditionObservation> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("afmr","condition_observation"));
    public static final StreamCodec<FriendlyByteBuf,ConditionObservation> CODEC=StreamCodec.ofMember(ConditionObservation::write,ConditionObservation::read);
    public ConditionObservation {catalog=Set.copyOf(catalog);if(catalog.size()>MAX_CATALOG||structures.bounds().size()>MAX_BOXES)throw new IllegalArgumentException("Observation too large");}
    public static ConditionObservation read(FriendlyByteBuf b){
        long token=b.readLong();String dimension=b.readUtf(256);int entity=b.readVarInt();boolean targeted=b.readBoolean(),available=b.readBoolean();
        int count=count(b,MAX_BOXES);var boxes=new ArrayList<StructureContext.Bounds>(count);
        for(int i=0;i<count;i++){
            String id=b.readUtf(256);int x=b.readInt(),y=b.readInt(),z=b.readInt(),xx=b.readInt(),yy=b.readInt(),zz=b.readInt();
            if(x>xx||y>yy||z>zz)throw new IllegalArgumentException("Invalid structure bounds");
            boxes.add(new StructureContext.Bounds(id,x,y,z,xx,yy,zz));
        }
        count=count(b,MAX_CATALOG);var ids=new HashSet<String>();for(int i=0;i<count;i++)ids.add(b.readUtf(256));
        return new ConditionObservation(token,dimension,entity,targeted,new StructureContext(available,boxes),ids);
    }
    private static int count(FriendlyByteBuf b,int max){int n=b.readVarInt();if(n<0||n>max)throw new IllegalArgumentException("Observation count out of range");return n;}
    public void write(FriendlyByteBuf b){
        b.writeLong(token);b.writeUtf(dimension,256);b.writeVarInt(entityId);b.writeBoolean(targeted);b.writeBoolean(structures.available());
        b.writeVarInt(structures.bounds().size());for(var box:structures.bounds()){
            b.writeUtf(box.id(),256);b.writeInt(box.minX());b.writeInt(box.minY());b.writeInt(box.minZ());b.writeInt(box.maxX());b.writeInt(box.maxY());b.writeInt(box.maxZ());
        }
        b.writeVarInt(catalog.size());for(var id:catalog)b.writeUtf(id,256);
    }
    @Override public Type<ConditionObservation> type(){return TYPE;}
}

package dev.animal.afmr.network;

import dev.animal.afmr.core.StructureContext;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConditionProtocolTest {
    private FriendlyByteBuf buffer(){return new FriendlyByteBuf(Unpooled.buffer());}
    @Test void queryRoundTripRetainsOnlyScopedObservationInputs(){
        var q=new ConditionQuery(Long.MAX_VALUE,"example:dimension",42,128,true,true);var b=buffer();
        try{q.write(b);assertEquals(q,ConditionQuery.read(b));assertEquals(0,b.readableBytes());}finally{b.release();}
    }
    @Test void observationRoundTripPreservesNegativeBoundsAndModdedIds(){
        var structures=new StructureContext(true,List.of(new StructureContext.Bounds("example:castle",-300,-64,-100,-200,320,0)));
        var observation=new ConditionObservation(17,"minecraft:overworld",3,true,structures,Set.of("example:castle"));var b=buffer();
        try{observation.write(b);assertEquals(observation,ConditionObservation.read(b));assertEquals(0,b.readableBytes());}finally{b.release();}
    }
    @Test void unavailableIsNotAnEmptyAvailableScan(){
        var observation=new ConditionObservation(1,"minecraft:the_end",3,false,StructureContext.UNAVAILABLE,Set.of());var b=buffer();
        try{observation.write(b);assertFalse(ConditionObservation.read(b).structures().available());}finally{b.release();}
    }
    private void header(FriendlyByteBuf b){b.writeLong(1);b.writeUtf("minecraft:overworld");b.writeVarInt(1);b.writeBoolean(false);b.writeBoolean(true);}
    @Test void rejectsNegativeAndOversizedBoundsCountsBeforeAllocating(){
        for(int count:new int[]{-1,ConditionObservation.MAX_BOXES+1,Integer.MAX_VALUE}){
            var b=buffer();try{header(b);b.writeVarInt(count);assertThrows(IllegalArgumentException.class,()->ConditionObservation.read(b));}finally{b.release();}
        }
    }
    @Test void rejectsOversizedRegistryCountsBeforeAllocating(){
        var b=buffer();try{header(b);b.writeVarInt(0);b.writeVarInt(ConditionObservation.MAX_CATALOG+1);assertThrows(IllegalArgumentException.class,()->ConditionObservation.read(b));}finally{b.release();}
    }
    @Test void rejectsInvertedBounds(){
        var b=buffer();try{
            header(b);b.writeVarInt(1);b.writeUtf("example:castle");b.writeInt(10);b.writeInt(0);b.writeInt(0);b.writeInt(9);b.writeInt(10);b.writeInt(10);b.writeVarInt(0);
            assertThrows(IllegalArgumentException.class,()->ConditionObservation.read(b));
        }finally{b.release();}
    }
    @Test void maximumBoundedPacketFitsClientboundLimit(){
        var ids=new HashSet<String>();for(int i=0;i<ConditionObservation.MAX_CATALOG;i++)ids.add("example:"+"s".repeat(240)+i);
        var boxes=new ArrayList<StructureContext.Bounds>();for(int i=0;i<ConditionObservation.MAX_BOXES;i++)boxes.add(new StructureContext.Bounds("example:"+"s".repeat(240),0,0,0,16,16,16));
        var observation=new ConditionObservation(1,"minecraft:overworld",3,true,new StructureContext(true,boxes),ids);var b=buffer();
        try{observation.write(b);assertTrue(b.readableBytes()<1_048_576);assertEquals(observation,ConditionObservation.read(b));}finally{b.release();}
    }
}

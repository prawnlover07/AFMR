package dev.animal.afmr.network;
import dev.animal.afmr.core.CombatOpponent;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CombatHitTest {
 @Test void roundTripsEveryCategoryAndDirection(){
  for(var kind:CombatOpponent.values())for(boolean incoming:new boolean[]{false,true}){
   var data=new FriendlyByteBuf(Unpooled.buffer());try{var hit=new CombatHit("minecraft:overworld",42,incoming,kind);hit.write(data);assertEquals(hit,CombatHit.read(data));assertEquals(0,data.readableBytes());}finally{data.release();}
  }
 }
 @Test void rejectsUnknownOrNegativeCategory(){
  for(int id:new int[]{-1,4,Integer.MAX_VALUE}){var data=new FriendlyByteBuf(Unpooled.buffer());try{data.writeUtf("minecraft:overworld");data.writeVarInt(42);data.writeBoolean(true);data.writeVarInt(id);assertThrows(IllegalArgumentException.class,()->CombatHit.read(data));}finally{data.release();}}
 }
 @Test void rejectsOversizedDimension(){var data=new FriendlyByteBuf(Unpooled.buffer());try{data.writeUtf("x".repeat(257));data.writeVarInt(42);data.writeBoolean(true);data.writeVarInt(0);assertThrows(RuntimeException.class,()->CombatHit.read(data));}finally{data.release();}}
 @Test void rejectsTruncatedEvent(){var data=new FriendlyByteBuf(Unpooled.buffer());try{data.writeUtf("minecraft:overworld");assertThrows(IndexOutOfBoundsException.class,()->CombatHit.read(data));}finally{data.release();}}
}

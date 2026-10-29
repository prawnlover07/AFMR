package dev.animal.afmr.network;
import dev.animal.afmr.core.CombatOpponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
/** A confirmed event for the receiving player, never another player's identity. */
public record CombatHit(String dimension,int entityId,boolean incoming,CombatOpponent opponent) {
 public static final ResourceLocation ID=new ResourceLocation("afmr","combat_hit");
 private static final CombatOpponent[] OPPONENTS=CombatOpponent.values();
 public CombatHit {java.util.Objects.requireNonNull(opponent);}
 public static CombatHit read(FriendlyByteBuf data){
  String dimension=data.readUtf(256);int id=data.readVarInt();boolean incoming=data.readBoolean();int kind=data.readVarInt();
  if(kind<0||kind>=OPPONENTS.length)throw new IllegalArgumentException("Invalid combat category");
  return new CombatHit(dimension,id,incoming,OPPONENTS[kind]);
 }
 public void write(FriendlyByteBuf data){data.writeUtf(dimension,256);data.writeVarInt(entityId);data.writeBoolean(incoming);data.writeVarInt(opponent.ordinal());}
}

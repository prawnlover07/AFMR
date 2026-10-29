package dev.animal.afmr.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests only the sender's surroundings. No remote coordinates or player identities. */
public record ConditionQuery(long token,String dimension,int entityId,int radius,boolean combat,boolean catalog)
    implements CustomPacketPayload {
    public static final Type<ConditionQuery> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("afmr","condition_query"));
    public static final StreamCodec<FriendlyByteBuf,ConditionQuery> CODEC=StreamCodec.ofMember(ConditionQuery::write,ConditionQuery::read);
    public static ConditionQuery read(FriendlyByteBuf b){return new ConditionQuery(b.readLong(),b.readUtf(256),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean());}
    public void write(FriendlyByteBuf b){b.writeLong(token);b.writeUtf(dimension,256);b.writeVarInt(entityId);b.writeVarInt(radius);b.writeBoolean(combat);b.writeBoolean(catalog);}
    @Override public Type<ConditionQuery> type(){return TYPE;}
}

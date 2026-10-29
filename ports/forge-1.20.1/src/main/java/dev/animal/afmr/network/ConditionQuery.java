package dev.animal.afmr.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Requests only the sender's surroundings. No remote coordinates or player identities. */
public record ConditionQuery(long token,String dimension,int entityId,int radius,boolean combat,boolean catalog)
    {
    public static final ResourceLocation ID=new ResourceLocation("afmr","condition_query");
    public static ConditionQuery read(FriendlyByteBuf b){return new ConditionQuery(b.readLong(),b.readUtf(256),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean());}
    public void write(FriendlyByteBuf b){b.writeLong(token);b.writeUtf(dimension,256);b.writeVarInt(entityId);b.writeVarInt(radius);b.writeBoolean(combat);b.writeBoolean(catalog);}
}

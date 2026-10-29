package dev.animal.afmr.network;

import dev.animal.afmr.server.ServerConditions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ConditionNetworking {
    private ConditionNetworking(){}
    public static void register(){
        PayloadTypeRegistry.serverboundPlay().register(ConditionQuery.TYPE,ConditionQuery.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConditionObservation.TYPE,ConditionObservation.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ConditionQuery.TYPE,(query,context)->{
            if(ServerPlayNetworking.canSend(context.player(),ConditionObservation.TYPE))ServerConditions.request(context.player(),query);
        });
    }
}

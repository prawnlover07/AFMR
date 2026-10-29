package dev.animal.afmr.network;

import dev.animal.afmr.server.ServerConditions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ConditionNetworking {
    private ConditionNetworking(){}
    public static void register(){
        PayloadTypeRegistry.playC2S().register(ConditionQuery.TYPE,ConditionQuery.CODEC);
        PayloadTypeRegistry.playS2C().register(ConditionObservation.TYPE,ConditionObservation.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ConditionQuery.TYPE,(query,context)->{
            if(ServerPlayNetworking.canSend(context.player(),ConditionObservation.TYPE))ServerConditions.request(context.player(),query);
        });
    }
}

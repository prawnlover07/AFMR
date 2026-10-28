package dev.animal.afmr.network;

import dev.animal.afmr.server.ServerConditions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid="afmr")
public final class ConditionNetworking {
    private ConditionNetworking(){}
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("afmr").optional();
        registrar.playToServer(ConditionQuery.TYPE,ConditionQuery.CODEC,(query,context)->{
            if(context.listener().hasChannel(ConditionObservation.TYPE))ServerConditions.request((ServerPlayer)context.player(),query);
        });
        registrar.playToClient(ConditionObservation.TYPE,ConditionObservation.CODEC);
    }
}

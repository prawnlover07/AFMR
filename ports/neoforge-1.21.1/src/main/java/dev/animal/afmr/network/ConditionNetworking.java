package dev.animal.afmr.network;
@net.neoforged.fml.common.EventBusSubscriber(modid="afmr",bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class ConditionNetworking {
 private ConditionNetworking(){}
 @net.neoforged.bus.api.SubscribeEvent public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){
  var registrar=event.registrar("afmr").optional();
  registrar.playToServer(ConditionQuery.TYPE,ConditionQuery.CODEC,(query,context)->{if(context.player() instanceof net.minecraft.server.level.ServerPlayer p&&p.connection.hasChannel(ConditionObservation.TYPE))dev.animal.afmr.server.ServerConditions.request(p,query);});
  registrar.playToClient(ConditionObservation.TYPE,ConditionObservation.CODEC,(value,context)->dev.animal.afmr.ClientConditions.accept(value));
 }
}

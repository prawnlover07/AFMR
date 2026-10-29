package dev.animal.afmr.server;

import dev.animal.afmr.network.ConditionQuery;
import dev.animal.afmr.network.ConditionObservation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Common entry point for dedicated servers and the client's integrated server. */
public final class ServerAfmr implements ModInitializer {
    @Override public void onInitialize(){
        ServerConditions.register();
        ServerPlayNetworking.registerGlobalReceiver(ConditionQuery.ID,(server,player,handler,buffer,sender)->{
            var query=ConditionQuery.read(buffer);
            server.execute(()->{if(ServerPlayNetworking.canSend(player,ConditionObservation.ID))ServerConditions.request(player,query);});
        });
    }
}

package dev.animal.afmr;

import dev.animal.afmr.core.NearbyPlayers;
import net.minecraft.client.Minecraft;
import java.util.HashMap;

/** Observe only entities already tracked by the client, at most four times a second. */
final class PlayerProximity {
    private Object level;
    private double nextObservation;
    private NearbyPlayers snapshot=NearbyPlayers.EMPTY;
    void clear(){level=null;nextObservation=0;snapshot=NearbyPlayers.EMPTY;}
    NearbyPlayers update(Minecraft mc,double now){
        if(mc.level==null||mc.player==null){level=null;snapshot=NearbyPlayers.EMPTY;return snapshot;}
        if(level==mc.level&&now<nextObservation)return snapshot;
        level=mc.level;nextObservation=now+.25;
        var distances=new HashMap<String,Double>();
        for(var player:mc.level.players())if(player!=mc.player&&player.isAlive()){
            double distance=player.distanceToSqr(mc.player);
            distances.put(player.getUUID().toString(),distance);
            distances.put(player.getGameProfile().getName(),distance);
        }
        snapshot=distances.isEmpty()?NearbyPlayers.EMPTY:new NearbyPlayers(distances);return snapshot;
    }
}

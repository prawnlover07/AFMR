package dev.animal.afmr;

import dev.animal.afmr.core.CombatTracker.Opponent;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Entity;

/** Server-owned loaded-mob index; the client exchanges only scoped requests and booleans. */
public final class HostileTargeting {
    private record Query(MinecraftServer server,ResourceKey<Level> dimension,UUID player,int entityId) {}
    private record Observation(Query query,boolean targeted) {}
    private static volatile Query requested;
    private static volatile Observation observation;
    // These fields belong exclusively to the integrated server thread.
    private static final Map<ServerLevel,Mobs> levels=new IdentityHashMap<>();
    private static Query active;
    private static int cursor,nextCheck;

    private static final class Mobs {
        final ArrayList<Mob> entries=new ArrayList<>();
        final Int2IntOpenHashMap indices=new Int2IntOpenHashMap();
        Mobs(){indices.defaultReturnValue(-1);}
        void add(Mob mob){if(indices.containsKey(mob.getId()))return;indices.put(mob.getId(),entries.size());entries.add(mob);}
        void remove(Mob mob){
            int index=indices.remove(mob.getId());if(index<0)return;
            var last=entries.remove(entries.size()-1);
            if(index<entries.size()){entries.set(index,last);indices.put(last.getId(),index);}
        }
    }
    private HostileTargeting(){}

    /** Called on the client. No targeting requests are made to multiplayer servers. */
    public static boolean observe(Minecraft mc,boolean combatActive){
        var server=mc.getSingleplayerServer();
        if(!combatActive||server==null||mc.level==null||mc.player==null){clear();return false;}
        var query=requested;
        if(query==null||query.server()!=server||!query.dimension().equals(mc.level.dimension())
            ||!query.player().equals(mc.player.getUUID())||query.entityId()!=mc.player.getId()){
            query=new Query(server,mc.level.dimension(),mc.player.getUUID(),mc.player.getId());requested=query;
        }
        var result=observation;return result!=null&&result.query()==query&&result.targeted();
    }
    /** Client disconnect/session reset; server-owned collections are never touched here. */
    public static void clear(){requested=null;}

    public static void register(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(entity instanceof Mob mob&&CombatTargets.classify(mob,null)==Opponent.MOB)levels.computeIfAbsent(level,key->new Mobs()).add(mob);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->{
            if(entity instanceof Mob mob){var mobs=levels.get(level);if(mobs!=null)mobs.remove(mob);}
        });
        ServerWorldEvents.UNLOAD.register((server,level)->levels.remove(level));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{
            levels.keySet().removeIf(level->level.getServer()==server);
            if(active!=null&&active.server()==server){active=null;observation=null;cursor=nextCheck=0;}
        });
        ServerTickEvents.END_SERVER_TICK.register(HostileTargeting::tick);
    }
    private static void tick(MinecraftServer server){
        var query=requested;
        if(query==null||query.server()!=server){
            if(active!=null&&active.server()==server){active=null;observation=null;cursor=nextCheck=0;}return;
        }
        if(active!=query){active=query;cursor=nextCheck=0;observation=null;}
        int tick=server.getTickCount();if(tick<nextCheck)return;
        var player=server.getPlayerList().getPlayer(query.player());
        var level=server.getLevel(query.dimension());
        if(player==null||level==null||player.level()!=level||player.getId()!=query.entityId()||!player.isAlive()){
            observation=new Observation(query,false);cursor=0;nextCheck=tick+5;return;
        }
        var mobs=levels.get(level);int count=mobs==null?0:mobs.entries.size();
        int end=Math.min(count,cursor+256); // A fixed budget per tick, never an all-world/entity scan.
        for(;cursor<end;cursor++){
            var mob=mobs.entries.get(cursor);
            if(mob.isAlive()&&!mob.isRemoved()&&mob.level()==level&&mob.getTarget()==player){
                observation=new Observation(query,true);cursor=0;nextCheck=tick+5;return;
            }
        }
        if(cursor>=count){observation=new Observation(query,false);cursor=0;nextCheck=tick+5;}
    }
}

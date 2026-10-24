package dev.animal.afmr;

import dev.animal.afmr.core.CombatTracker.Opponent;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Server-owned loaded-mob index; the client exchanges only scoped requests and booleans. */
@EventBusSubscriber(modid="afmr",value=Dist.CLIENT)
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
            var last=entries.removeLast();
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

    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof Mob mob
            &&CombatTargets.classify(mob,null)==Opponent.MOB)levels.computeIfAbsent(level,key->new Mobs()).add(mob);
    }
    @SubscribeEvent public static void left(EntityLeaveLevelEvent event){
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof Mob mob){
            var mobs=levels.get(level);if(mobs!=null)mobs.remove(mob);
        }
    }
    @SubscribeEvent public static void unloaded(LevelEvent.Unload event){
        if(event.getLevel() instanceof ServerLevel level)levels.remove(level);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){
        levels.keySet().removeIf(level->level.getServer()==event.getServer());
        if(active!=null&&active.server()==event.getServer()){active=null;observation=null;cursor=nextCheck=0;}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var query=requested;
        if(query==null||query.server()!=event.getServer()){
            if(active!=null&&active.server()==event.getServer()){active=null;observation=null;cursor=nextCheck=0;}return;
        }
        if(active!=query){active=query;cursor=nextCheck=0;observation=null;}
        int tick=event.getServer().getTickCount();if(tick<nextCheck)return;
        var player=event.getServer().getPlayerList().getPlayer(query.player());
        var level=event.getServer().getLevel(query.dimension());
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

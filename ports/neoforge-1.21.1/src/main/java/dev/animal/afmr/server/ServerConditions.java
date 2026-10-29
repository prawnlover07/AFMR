package dev.animal.afmr.server;

import dev.animal.afmr.core.StructureContext;
import dev.animal.afmr.network.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** All state belongs to the server thread. Each connection has at most one pending query. */
public final class ServerConditions {
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final ArrayDeque<Session> work=new ArrayDeque<>();
    private static final ArrayList<Mob> mobs=new ArrayList<>();
    private static final Object2IntOpenHashMap<Mob> indices=new Object2IntOpenHashMap<>();
    private static final Map<ServerLevel,Set<String>> catalogs=new IdentityHashMap<>();
    static {indices.defaultReturnValue(-1);}
    private static final class Session {
        final ServerPlayer player;
        ConditionQuery query;
        boolean pending,targeted;
        int lastRequest,nextReply,cursor,nextTarget,nextStructure;
        StructureContext structures=StructureContext.UNAVAILABLE;
        Session(ServerPlayer player){this.player=player;}
    }
    private ServerConditions(){}
    public static void request(ServerPlayer player,ConditionQuery query){
        if(query.radius() < -1||query.radius()>StructureContext.MAX_RADIUS
            ||query.entityId()!=player.getId()||!query.dimension().equals(player.level().dimension().location().toString()))return;
        var session=sessions.get(player.getUUID());
        if(session==null||session.player!=player){
            if(session!=null)work.remove(session);
            session=new Session(player);sessions.put(player.getUUID(),session);work.addLast(session);
        }
        if(session.query==null||!session.query.dimension().equals(query.dimension())){
            session.structures=StructureContext.UNAVAILABLE;session.targeted=false;session.cursor=0;session.nextStructure=0;
        }
        if(session.query==null||session.query.radius()!=query.radius()){session.structures=StructureContext.UNAVAILABLE;session.nextStructure=0;}
        session.query=query;session.pending=true;session.lastRequest=player.level().getServer().getTickCount();
        if(!query.combat()){session.targeted=false;session.cursor=0;}
        if(query.radius()<0)session.structures=StructureContext.UNAVAILABLE;
    }
    public static void register(){
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e)->{if(e.getLevel() instanceof ServerLevel&&e.getEntity() instanceof Mob mob&&CombatMobs.includes(mob))add(mob);});
        bus.addListener((net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e)->{if(e.getLevel() instanceof ServerLevel&&e.getEntity() instanceof Mob mob)remove(mob);});
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e)->{var s=sessions.remove(e.getEntity().getUUID());if(s!=null)work.remove(s);});
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e)->{sessions.clear();work.clear();mobs.clear();indices.clear();catalogs.clear();});
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e)->{if(e.getLevel() instanceof ServerLevel level)catalogs.remove(level);});
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e)->{tick(e.getServer());});
    }
    private static void add(Mob mob){if(indices.containsKey(mob))return;indices.put(mob,mobs.size());mobs.add(mob);}
    private static void remove(Mob mob){
        int index=indices.removeInt(mob);if(index<0)return;
        var last=mobs.remove(mobs.size()-1);if(index<mobs.size()){mobs.set(index,last);indices.put(last,index);}
    }
    private static void tick(MinecraftServer server){
        int tick=server.getTickCount(),budget=256;
        boolean scannedStructures=false;
        // Up to eight clients/tick, rotating fairly. Total mob visits never exceed 256/tick.
        int count=Math.min(8,work.size());
        for(int i=0;i<count;i++){
            var s=work.removeFirst();var player=s.player;var q=s.query;
            if(player.level().getServer()!=server||player.hasDisconnected()||tick-s.lastRequest>60){sessions.remove(player.getUUID(),s);continue;}
            work.addLast(s);
            if(q.entityId()!=player.getId()||!q.dimension().equals(player.level().dimension().location().toString())||!player.isAlive()){
                s.targeted=false;s.structures=StructureContext.UNAVAILABLE;continue;
            }
            if(q.combat()&&tick>=s.nextTarget&&budget>0){
                int end=Math.min(mobs.size(),s.cursor+Math.min(256/count,budget));
                while(s.cursor<end){
                    var mob=mobs.get(s.cursor++);budget--;
                    if(mob.isAlive()&&!mob.isRemoved()&&mob.level()==player.level()&&mob.getTarget()==player){
                        s.targeted=true;s.cursor=0;s.nextTarget=tick+5;break;
                    }
                }
                if(s.cursor>=mobs.size()){s.targeted=false;s.cursor=0;s.nextTarget=tick+5;}
            }
            if(!s.pending||tick<s.nextReply)continue;
            if(q.radius()>=0&&tick>=s.nextStructure){
                if(scannedStructures)continue;
                var level=(ServerLevel)player.level();var pos=player.blockPosition();
                var result=ServerStructures.scan(level,pos.getX()>>4,pos.getZ()>>4,q.radius(),catalogs.getOrDefault(level,Set.of()));
                catalogs.put(level,result.catalog());s.structures=result.context();s.nextStructure=tick+10;scannedStructures=true;
            }
            var observation=new ConditionObservation(q.token(),q.dimension(),q.entityId(),s.targeted,s.structures,
                q.catalog()?catalogs.getOrDefault((ServerLevel)player.level(),Set.of()):Set.of());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,observation);
            s.pending=false;s.nextReply=tick+5;
        }
        // Rotate the starting client even when all clients fit in one tick; a flood cannot monopolize scans.
        if(!work.isEmpty())work.addLast(work.removeFirst());
    }
}

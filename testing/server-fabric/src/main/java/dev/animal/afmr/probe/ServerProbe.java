package dev.animal.afmr.probe;

import com.mojang.authlib.GameProfile;
import dev.animal.afmr.network.*;
import dev.animal.afmr.server.ServerConditions;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Headless test mod with disposable fake-player packet sinks, never shipped with AFMR. */
public final class ServerProbe implements ModInitializer {
    @Override public void onInitialize(){ServerTickEvents.END_SERVER_TICK.register(ServerProbe::run);}
    private static final List<String> results=new ArrayList<>();
    private static final List<ConditionObservation> first=new ArrayList<>(),second=new ArrayList<>();
    private static ServerPlayer a,b;
    private static Zombie zombie;
    private static int ticks,loaded,checkpoint;
    private static void check(boolean ok,String name){results.add((ok?"PASS ":"FAIL ")+name);if(!ok)throw new AssertionError(name);}
    private static ServerPlayer player(MinecraftServer server,String name,List<ConditionObservation> sink){
        var profile=new GameProfile(UUID.randomUUID(),name);
        var p=new ServerPlayer(server,server.overworld(),profile);
        new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),p){
            @Override public void send(Packet<?> packet){if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.getIdentifier().equals(ConditionObservation.ID))sink.add(ConditionObservation.read(custom.getData()));}
        };
        p.setPos(1,51,1);return p;
    }
    private static void query(ServerPlayer p,long token,int radius,boolean combat){ServerConditions.request(p,new ConditionQuery(token,p.level().dimension().location().toString(),p.getId(),radius,combat,true));}
    private static ConditionObservation latest(List<ConditionObservation> values,long token){return values.stream().filter(v->v.token()==token).reduce((x,y)->y).orElseThrow(()->new AssertionError("Missing response "+token));}
    private static boolean connected;
    private static void socket(MinecraftServer server){
        ticks++;
        if(!connected&&!server.getPlayerList().getPlayers().isEmpty()){
            var p=server.getPlayerList().getPlayers().get(0);var level=server.overworld();var chunk=level.getChunk(0,0);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"forceload add 0 0");
            var type=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new net.minecraft.resources.ResourceLocation("minecraft:mineshaft"));
            var room=new MineshaftPieces.MineShaftRoom(0,RandomSource.create(1),0,0,MineshaftStructure.Type.NORMAL);
            chunk.setStartForStructure(type,new StructureStart(type,new ChunkPos(0,0),0,new PiecesContainer(List.of(room))));
            for(int x=0;x<9;x++)for(int z=0;z<9;z++)for(int y=50;y<56;y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),(y==50?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(),3);
            server.getPlayerList().op(p.getGameProfile());
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamemode survival @a");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp @a 1 51 1");
            zombie=new Zombie(level);zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setPos(3,51,3);level.addFreshEntity(zombie);zombie.setTarget(p);connected=true;
        }
        if(ticks>3600)server.halt(false);
    }
    private static void run(MinecraftServer server){
        if(Boolean.getBoolean("afmr.probe.socket")){socket(server);return;}

        try{
            ticks++;
            if(ticks==1){
                var level=server.overworld();var chunk=level.getChunk(0,0);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"forceload add 0 0");
                var type=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new net.minecraft.resources.ResourceLocation("minecraft:mineshaft"));
                var room=new MineshaftPieces.MineShaftRoom(0,RandomSource.create(1),0,0,MineshaftStructure.Type.NORMAL);
                chunk.setStartForStructure(type,new StructureStart(type,new ChunkPos(0,0),0,new PiecesContainer(List.of(room))));
                a=player(server,"AFMRProbeA",first);b=player(server,"AFMRProbeB",second);
                zombie=new Zombie(level);zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setPos(3,51,3);level.addFreshEntity(zombie);zombie.setTarget(a);
                loaded=level.getChunkSource().getLoadedChunksCount();query(a,1,0,true);query(b,2,0,true);
            }
            if(ticks==15){
                loaded=server.overworld().getChunkSource().getLoadedChunksCount();
                var own=latest(first,1);var other=latest(second,2);
                check(own.targeted(),"loaded hostile target is observed");check(!other.targeted(),"other player does not inherit target");
                check(own.structures().available(),"loaded structure scan is available");
                check(own.structures().matches(Set.of("minecraft:mineshaft"),0,1,51,1),"actual loaded start supplies matching bounds");
                check(own.catalog().contains("minecraft:mineshaft"),"registry catalog supplied");
                check(own.entityId()==a.getId()&&other.entityId()==b.getId(),"observations scoped to each sender");
                checkpoint=first.size();ServerConditions.request(a,new ConditionQuery(99,"minecraft:the_end",a.getId(),0,true,true));
                ServerConditions.request(a,new ConditionQuery(100,a.level().dimension().location().toString(),b.getId(),0,true,true));
                ServerConditions.request(a,new ConditionQuery(101,a.level().dimension().location().toString(),a.getId(),129,true,true));
            }
            if(ticks==25){check(first.size()==checkpoint,"wrong dimension/entity and oversized radius ignored");zombie.setTarget(b);query(a,3,0,true);query(b,4,0,true);}
            if(ticks==35){query(a,3,0,true);query(b,4,0,true);}
            if(ticks==50)query(b,5,0,true);
            if(ticks==65)query(a,6,128,false);
            if(ticks==40){check(zombie.isAlive()&&!zombie.isRemoved()&&zombie.getTarget()==b,"controlled mob remains loaded with its new target");check(!latest(first,3).targeted(),"target loss clears first player");check(latest(second,4).targeted(),"target switch reaches second player");zombie.discard();query(b,5,0,true);}
            if(ticks==55){check(!latest(second,5).targeted(),"unloaded mob cannot retain combat");query(a,6,128,false);}
            if(ticks==70){
                check(latest(first,6).structures().available(),"maximum-radius loaded scan completes");
                check(server.overworld().getChunkSource().getLoadedChunksCount()==loaded,"observation does not load or generate chunks");
                finish(server,null);
            }
        }catch(Throwable failure){finish(server,failure);}
    }
    private static void finish(MinecraftServer server,Throwable failure){
        if(failure!=null){results.add("FAIL "+failure);failure.printStackTrace();}
        try{Files.write(Path.of("server-companion-results.txt"),results);}catch(Exception e){e.printStackTrace();}
        server.halt(false);
    }
}

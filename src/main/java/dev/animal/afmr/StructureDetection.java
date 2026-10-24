package dev.animal.afmr;

import dev.animal.afmr.core.StructureContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Client-owned cache. Only the integrated server thread reads structure/chunk state. */
final class StructureDetection {
    private record Snapshot(Set<String> catalog,StructureContext context) {}
    private IntegratedServer server;
    private ResourceKey<Level> dimension;
    private CompletableFuture<Snapshot> pending;
    private Snapshot snapshot;
    private double nextScan;

    void clear(){server=null;dimension=null;pending=null;snapshot=null;nextScan=0;}
    Set<String> catalog(){return snapshot==null?Set.of():snapshot.catalog();}

    /** Negative radius disables observation. There is at most one outstanding request per session. */
    StructureContext update(Minecraft mc,double now,int radius){
        if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null){clear();return StructureContext.UNAVAILABLE;}
        var currentServer=mc.getSingleplayerServer();var currentDimension=mc.level.dimension();
        if(server!=currentServer||!currentDimension.equals(dimension)){
            clear();server=currentServer;dimension=currentDimension;
        }
        if(radius<0)return StructureContext.UNAVAILABLE;
        radius=Math.clamp(radius,0,StructureContext.MAX_RADIUS);
        if(pending!=null&&pending.isDone()){
            try{snapshot=pending.join();}
            catch(Exception e){snapshot=null;Afmr.LOG.warn("Could not observe singleplayer structures",e);}
            pending=null;
        }
        int chunkX=mc.player.blockPosition().getX()>>4,chunkZ=mc.player.blockPosition().getZ()>>4;
        if(pending==null&&now>=nextScan){
            int requestedRadius=radius;
            var catalog=snapshot==null?Set.<String>of():snapshot.catalog();
            // Capture owned inputs, never the mutable library, player or detector state.
            pending=CompletableFuture.supplyAsync(()->{
                var level=currentServer.getLevel(currentDimension);
                return level==null?null:scan(level,chunkX,chunkZ,requestedRadius,catalog);
            },currentServer);
            nextScan=now+0.5;
        }
        // Known bounds remain valid while crossing chunks; position matching happens on the client.
        // Keep them until the next observation to avoid dropping a village playlist at each chunk edge.
        return snapshot==null?StructureContext.UNAVAILABLE:snapshot.context();
    }

    private static Snapshot scan(ServerLevel level,int chunkX,int chunkZ,int radius,Set<String> catalog){
        var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        if(catalog.isEmpty()){
            var ids=new TreeSet<String>();for(var id:registry.keySet())ids.add(id.toString());catalog=Set.copyOf(ids);
        }
        var starts=Collections.newSetFromMap(new IdentityHashMap<StructureStart,Boolean>());
        var boxes=new ArrayList<StructureContext.Bounds>();
        var chunks=level.getChunkSource();
        int reach=(radius+15)/16;
        // At most 17x17 loaded chunks (radius <= 128); never load or generate a chunk.
        for(int x=chunkX-reach;x<=chunkX+reach;x++)for(int z=chunkZ-reach;z<=chunkZ+reach;z++){
            var chunk=chunks.getChunkNow(x,z);if(chunk==null)continue;
            starts.addAll(chunk.getAllStarts().values());
            for(var entry:chunk.getAllReferences().entrySet()){
                var references=entry.getValue().iterator();
                while(references.hasNext()){
                    var pos=ChunkPos.unpack(references.nextLong());
                    var origin=chunks.getChunkNow(pos.x(),pos.z());if(origin==null)continue;
                    var start=origin.getStartForStructure(entry.getKey());if(start!=null)starts.add(start);
                }
            }
        }
        for(var start:starts)if(start.isValid()){
            var id=registry.getKey(start.getStructure());if(id==null)continue;
            var b=start.getBoundingBox();
            boxes.add(new StructureContext.Bounds(id.toString(),b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ()));
        }
        return new Snapshot(catalog,new StructureContext(true,boxes));
    }
}

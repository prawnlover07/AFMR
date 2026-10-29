package dev.animal.afmr.server;

import dev.animal.afmr.core.StructureContext;
import dev.animal.afmr.network.ConditionObservation;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import java.util.*;

/** Server thread only. Observes loaded chunks without loading or generating terrain. */
final class ServerStructures {
    record Result(Set<String> catalog,StructureContext context) {}
    static Result scan(ServerLevel level,int chunkX,int chunkZ,int radius,Set<String> catalog){
        var registry=level.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
        if(catalog.isEmpty()){
            var ids=new TreeSet<String>();for(var id:registry.keySet())ids.add(id.toString());catalog=Set.copyOf(ids.stream().filter(id->id.length()<=256).limit(ConditionObservation.MAX_CATALOG).toList());
        }
        var starts=Collections.newSetFromMap(new IdentityHashMap<StructureStart,Boolean>());
        var boxes=new ArrayList<StructureContext.Bounds>();
        var chunks=level.getChunkSource();
        int reach=(radius+15)/16,referencesVisited=0;
        // At most 17x17 loaded chunks (radius <= 128); never load or generate a chunk.
        for(int x=chunkX-reach;x<=chunkX+reach;x++)for(int z=chunkZ-reach;z<=chunkZ+reach;z++){
            var chunk=chunks.getChunkNow(x,z);if(chunk==null)continue;
            starts.addAll(chunk.getAllStarts().values());
            if(starts.size()>4096)return new Result(catalog,StructureContext.UNAVAILABLE);
            for(var entry:chunk.getAllReferences().entrySet()){
                var references=entry.getValue().iterator();
                while(references.hasNext()){
                    if(++referencesVisited>4096)return new Result(catalog,StructureContext.UNAVAILABLE);
                    var pos=new ChunkPos(references.nextLong());
                    var origin=chunks.getChunkNow(pos.x,pos.z);if(origin==null)continue;
                    var start=origin.getStartForFeature(entry.getKey());if(start!=null)starts.add(start);
                }
            }
        }
        for(var start:starts)if(start.isValid()){
            var id=registry.getKey(start.getFeature());if(id==null)continue;
            if(id.toString().length()>256)return new Result(catalog,StructureContext.UNAVAILABLE);
            var b=start.getBoundingBox();
            boxes.add(new StructureContext.Bounds(id.toString(),b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ()));
        }
        return new Result(catalog,boxes.size()>ConditionObservation.MAX_BOXES?StructureContext.UNAVAILABLE:new StructureContext(true,boxes));
    }
}

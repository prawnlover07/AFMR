package dev.animal.afmr;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Client-thread-only surface sampling. Water and leaves alone do not count as solid cover. */
final class SurfaceCover {
    private final BlockPos.MutableBlockPos sample=new BlockPos.MutableBlockPos();
    private ChunkAccess sampledChunk;
    private int sampledX,sampledZ,surface;
    private long sampledAt;

    void clear(){sampledChunk=null;}

    int height(ChunkAccess chunk,long tick,int x,int z){
        if(chunk==sampledChunk&&x==sampledX&&z==sampledZ&&tick>=sampledAt&&tick-sampledAt<5)return surface;
        sampledChunk=chunk;sampledX=x;sampledZ=z;sampledAt=tick;
        // In 1.20.1 only MOTION_BLOCKING and WORLD_SURFACE are sent to clients.
        // MOTION_BLOCKING_NO_LEAVES and OCEAN_FLOOR remain empty after a chunk packet.
        int y=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING,x&15,z&15);
        int minY=chunk.getMinBuildHeight();
        // Trim fluids, leaves and intervening air in this single cached column.
        while(y>=minY&&(!chunk.getBlockState(sample.set(x,y,z)).blocksMotion()||chunk.getBlockState(sample).is(BlockTags.LEAVES)))y--;
        surface=y+1;
        return surface;
    }
}

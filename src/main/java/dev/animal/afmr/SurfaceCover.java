package dev.animal.afmr;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;

/** Client-thread-only surface sampling. Water and leaves alone do not count as solid cover. */
final class SurfaceCover {
    private final BlockPos.MutableBlockPos sample=new BlockPos.MutableBlockPos();
    private ClientLevel sampledLevel;
    private int sampledX,sampledZ,surface;
    private long sampledAt;

    void clear(){sampledLevel=null;}

    int height(ClientLevel level,int x,int z){
        long tick=level.getGameTime();
        if(level==sampledLevel&&x==sampledX&&z==sampledZ&&tick>=sampledAt&&tick-sampledAt<5)return surface;
        sampledLevel=level;sampledX=x;sampledZ=z;sampledAt=tick;
        var chunk=level.getChunk(x>>4,z>>4);
        // OCEAN_FLOOR exists on client chunks but is not sent in chunk packets: it stays empty
        // until block updates happen. Start with the synchronized no-leaves map instead.
        int y=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x&15,z&15);
        int minY=level.getMinY();
        // This map includes fluids. Inspect only this column to trim water down to solid terrain.
        while(y>=minY&&!chunk.getBlockState(sample.set(x,y,z)).is(BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP_NO_LEAVES))y--;
        surface=y+1;
        return surface;
    }
}

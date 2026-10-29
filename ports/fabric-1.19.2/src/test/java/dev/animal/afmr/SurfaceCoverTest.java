package dev.animal.afmr;

import com.mojang.serialization.Lifecycle;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.StructureContext;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;


import net.minecraft.data.BuiltinRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Real 1.20.1 block sections with only the heightmaps supplied to a client on chunk load. */
class SurfaceCoverTest {
    private static Registry<Biome> biomes;
    @BeforeAll static void bootstrap(){
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var registry=new MappedRegistry<Biome>(Registry.BIOME_REGISTRY,Lifecycle.stable(),null);
        Registry.register(registry,Biomes.PLAINS,BuiltinRegistries.BIOME.get(Biomes.PLAINS));
        biomes=registry.freeze();
        Registry.BLOCK.bindTags(Map.of(BlockTags.LEAVES,List.of(Registry.BLOCK.getHolder(Registry.BLOCK.getId(Blocks.OAK_LEAVES)).orElseThrow())));
    }
    private ProtoChunk chunk(int chunkX,int chunkZ){
        return new ProtoChunk(new ChunkPos(chunkX,chunkZ),UpgradeData.EMPTY,LevelHeightAccessor.create(-64,384),biomes,null);
    }
    private void block(ProtoChunk chunk,int x,int y,int z,Block block){
        chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x&15,y&15,z&15,block.defaultBlockState());
    }
    private void receivedHeightmaps(ProtoChunk chunk){
        // LevelChunk constructs every FULL map empty before reading the transmitted maps.
        // An absent map would be lazily calculated by getHeight, hiding this client-only bug.
        for(var type:ChunkStatus.FULL.heightmapsAfter()){
            var map=chunk.getOrCreateHeightmapUnprimed(type);
            map.setRawData(chunk,type,new long[map.getRawData().length]);
        }
        Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.MOTION_BLOCKING,Heightmap.Types.WORLD_SURFACE));
    }
    @Test void caveRoofIsDetectedImmediatelyFromClientHeightmaps(){
        assertTrue(Heightmap.Types.MOTION_BLOCKING.sendToClient());
        assertFalse(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.sendToClient());
        var chunk=chunk(0,0);block(chunk,7,90,6,Blocks.STONE);receivedHeightmaps(chunk);
        assertEquals(-65,chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,7,6));
        assertEquals(91,new SurfaceCover().height(chunk,0,7,6));
    }
    @Test void waterAloneDoesNotBecomeARoof(){
        var chunk=chunk(0,0);block(chunk,7,60,6,Blocks.STONE);
        for(int y=61;y<=80;y++)block(chunk,7,y,6,Blocks.WATER);
        receivedHeightmaps(chunk);assertEquals(61,new SurfaceCover().height(chunk,0,7,6));
    }
    @Test void leavesAndTheAirBelowThemDoNotBecomeARoof(){
        var chunk=chunk(0,0);block(chunk,7,60,6,Blocks.STONE);block(chunk,7,100,6,Blocks.OAK_LEAVES);
        receivedHeightmaps(chunk);assertEquals(61,new SurfaceCover().height(chunk,0,7,6));
    }
    @Test void leavesAboveASolidRoofRetainTheSolidRoof(){
        var chunk=chunk(0,0);block(chunk,7,90,6,Blocks.STONE);block(chunk,7,100,6,Blocks.OAK_LEAVES);
        receivedHeightmaps(chunk);assertEquals(91,new SurfaceCover().height(chunk,0,7,6));
    }
    @Test void anEmptyColumnTerminatesAtTheWorldBottom(){
        var chunk=chunk(0,0);receivedHeightmaps(chunk);assertEquals(-64,new SurfaceCover().height(chunk,0,7,6));
    }
    @Test void negativeChunkCoordinatesUseTheCorrectColumn(){
        var chunk=chunk(-2,-3);block(chunk,-17,90,-34,Blocks.STONE);receivedHeightmaps(chunk);
        assertEquals(91,new SurfaceCover().height(chunk,0,-17,-34));
    }
    @Test void cacheRefreshesAfterFiveTicksAndAfterClear(){
        var chunk=chunk(0,0);block(chunk,7,90,6,Blocks.STONE);receivedHeightmaps(chunk);
        var cover=new SurfaceCover();assertEquals(91,cover.height(chunk,10,7,6));
        block(chunk,7,90,6,Blocks.AIR);block(chunk,7,70,6,Blocks.STONE);receivedHeightmaps(chunk);
        assertEquals(91,cover.height(chunk,14,7,6));assertEquals(71,cover.height(chunk,15,7,6));
        block(chunk,7,70,6,Blocks.AIR);receivedHeightmaps(chunk);cover.clear();
        assertEquals(-64,cover.height(chunk,16,7,6));
    }
    @Test void aReplacementChunkAndAChangedColumnCannotReuseOldCover(){
        var first=chunk(0,0);block(first,7,90,6,Blocks.STONE);receivedHeightmaps(first);
        var second=chunk(0,0);receivedHeightmaps(second);var cover=new SurfaceCover();
        assertEquals(91,cover.height(first,10,7,6));assertEquals(-64,cover.height(first,10,8,6));
        assertEquals(91,cover.height(first,10,7,6));assertEquals(-64,cover.height(second,10,7,6));
    }
    private Library.Context context(int surface){
        double depth=surface-62;
        return new Library.Context("minecraft:overworld","minecraft:snowy_plains",false,false,18000,
            depth>1,0,"fixture",7,62,6,depth,0,false,StructureContext.UNAVAILABLE,false,false,
            Library.Precipitation.NONE,10,false,"","",Library.GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    }
    @Test void sampledCaveAtNightSelectsDarkExplorationAheadOfNightOutside(){
        var chunk=chunk(0,0);block(chunk,7,90,6,Blocks.STONE);receivedHeightmaps(chunk);
        var library=Library.defaults();var observed=context(new SurfaceCover().height(chunk,0,7,6));
        assertEquals("dark_exploration",library.select(observed).id);
        assertEquals("night_outside",library.select(context(-64)).id);
    }
    @Test void foliageAloneAtNightStillSelectsNightOutside(){
        var chunk=chunk(0,0);block(chunk,7,60,6,Blocks.STONE);block(chunk,7,90,6,Blocks.OAK_LEAVES);
        receivedHeightmaps(chunk);
        assertEquals("night_outside",Library.defaults().select(context(new SurfaceCover().height(chunk,0,7,6))).id);
    }
}

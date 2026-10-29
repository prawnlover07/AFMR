package dev.animal.afmr.probe;

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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** Real 1.20.1 block sections with only the heightmaps supplied to a client on chunk load. */
public final class SurfaceCoverChecks {
    private static Registry<Biome> biomes;
    static void bootstrap(){
        
        var registry=new MappedRegistry<Biome>(Registry.BIOME_REGISTRY,Lifecycle.stable(),null);
        Registry.register(registry,Biomes.PLAINS,BuiltinRegistries.BIOME.get(Biomes.PLAINS));
        biomes=registry.freeze();
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
    void caveRoofIsDetectedImmediatelyFromClientHeightmaps(){
        assertTrue(Heightmap.Types.MOTION_BLOCKING.sendToClient());
        assertFalse(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.sendToClient());
        var chunk=chunk(0,0);block(chunk,7,90,6,Blocks.STONE);receivedHeightmaps(chunk);
        assertEquals(-65,chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,7,6));
        assertEquals(91,height(cover(),chunk,0,7,6));
    }
    void waterAloneDoesNotBecomeARoof(){
        var chunk=chunk(0,0);block(chunk,7,60,6,Blocks.STONE);
        for(int y=61;y<=80;y++)block(chunk,7,y,6,Blocks.WATER);
        receivedHeightmaps(chunk);assertEquals(61,height(cover(),chunk,0,7,6));
    }
    void leavesAndTheAirBelowThemDoNotBecomeARoof(){
        var chunk=chunk(0,0);block(chunk,7,60,6,Blocks.STONE);block(chunk,7,100,6,Blocks.OAK_LEAVES);
        receivedHeightmaps(chunk);assertEquals(61,height(cover(),chunk,0,7,6));
    }
    void leavesAboveASolidRoofRetainTheSolidRoof(){
        var chunk=chunk(0,0);block(chunk,7,90,6,Blocks.STONE);block(chunk,7,100,6,Blocks.OAK_LEAVES);
        receivedHeightmaps(chunk);assertEquals(91,height(cover(),chunk,0,7,6));
    }
    void anEmptyColumnTerminatesAtTheWorldBottom(){
        var chunk=chunk(0,0);receivedHeightmaps(chunk);assertEquals(-64,height(cover(),chunk,0,7,6));
    }
    void negativeChunkCoordinatesUseTheCorrectColumn(){
        var chunk=chunk(-2,-3);block(chunk,-17,90,-34,Blocks.STONE);receivedHeightmaps(chunk);
        assertEquals(91,height(cover(),chunk,0,-17,-34));
    }
    void cacheRefreshesAfterFiveTicksAndAfterClear(){
        var chunk=chunk(0,0);block(chunk,7,90,6,Blocks.STONE);receivedHeightmaps(chunk);
        var cover=cover();assertEquals(91,height(cover,chunk,10,7,6));
        block(chunk,7,90,6,Blocks.AIR);block(chunk,7,70,6,Blocks.STONE);receivedHeightmaps(chunk);
        assertEquals(91,height(cover,chunk,14,7,6));assertEquals(71,height(cover,chunk,15,7,6));
        block(chunk,7,70,6,Blocks.AIR);receivedHeightmaps(chunk);clear(cover);
        assertEquals(-64,height(cover,chunk,16,7,6));
    }
    void aReplacementChunkAndAChangedColumnCannotReuseOldCover(){
        var first=chunk(0,0);block(first,7,90,6,Blocks.STONE);receivedHeightmaps(first);
        var second=chunk(0,0);receivedHeightmaps(second);var cover=cover();
        assertEquals(91,height(cover,first,10,7,6));assertEquals(-64,height(cover,first,10,8,6));
        assertEquals(91,height(cover,first,10,7,6));assertEquals(-64,height(cover,second,10,7,6));
    }
    private Library.Context context(int surface){
        double depth=surface-62;
        return new Library.Context("minecraft:overworld","minecraft:snowy_plains",false,false,18000,
            depth>1,0,"fixture",7,62,6,depth,0,false,StructureContext.UNAVAILABLE,false,false,
            Library.Precipitation.NONE,10,false,"","",Library.GameMode.SURVIVAL,false,dev.animal.afmr.core.NearbyPlayers.EMPTY,false);
    }
    void sampledCaveAtNightSelectsDarkExplorationAheadOfNightOutside(){
        var chunk=chunk(0,0);block(chunk,7,90,6,Blocks.STONE);receivedHeightmaps(chunk);
        var library=Library.defaults();var observed=context(height(cover(),chunk,0,7,6));
        assertEquals("dark_exploration",library.select(observed).id);
        assertEquals("night_outside",library.select(context(-64)).id);
    }
    void foliageAloneAtNightStillSelectsNightOutside(){
        var chunk=chunk(0,0);block(chunk,7,60,6,Blocks.STONE);block(chunk,7,90,6,Blocks.OAK_LEAVES);
        receivedHeightmaps(chunk);
        assertEquals("night_outside",Library.defaults().select(context(height(cover(),chunk,0,7,6))).id);
    }
    private static void assertTrue(boolean value){if(!value)throw new AssertionError("Expected true");}
    private static void assertFalse(boolean value){if(value)throw new AssertionError("Expected false");}
    private static void assertEquals(Object expected,Object actual){if(!java.util.Objects.equals(expected,actual))throw new AssertionError("Expected "+expected+"; actual "+actual);}
    public static void run() throws Exception {
        bootstrap();var checks=new SurfaceCoverChecks();var results=new java.util.ArrayList<String>();
        checks.caveRoofIsDetectedImmediatelyFromClientHeightmaps();results.add("PASS caveRoofIsDetectedImmediatelyFromClientHeightmaps");
        checks.waterAloneDoesNotBecomeARoof();results.add("PASS waterAloneDoesNotBecomeARoof");
        checks.leavesAndTheAirBelowThemDoNotBecomeARoof();results.add("PASS leavesAndTheAirBelowThemDoNotBecomeARoof");
        checks.leavesAboveASolidRoofRetainTheSolidRoof();results.add("PASS leavesAboveASolidRoofRetainTheSolidRoof");
        checks.anEmptyColumnTerminatesAtTheWorldBottom();results.add("PASS anEmptyColumnTerminatesAtTheWorldBottom");
        checks.negativeChunkCoordinatesUseTheCorrectColumn();results.add("PASS negativeChunkCoordinatesUseTheCorrectColumn");
        checks.cacheRefreshesAfterFiveTicksAndAfterClear();results.add("PASS cacheRefreshesAfterFiveTicksAndAfterClear");
        checks.aReplacementChunkAndAChangedColumnCannotReuseOldCover();results.add("PASS aReplacementChunkAndAChangedColumnCannotReuseOldCover");
        checks.sampledCaveAtNightSelectsDarkExplorationAheadOfNightOutside();results.add("PASS sampledCaveAtNightSelectsDarkExplorationAheadOfNightOutside");
        checks.foliageAloneAtNightStillSelectsNightOutside();results.add("PASS foliageAloneAtNightStillSelectsNightOutside");
        java.nio.file.Files.write(java.nio.file.Path.of("surface-cover-results.txt"),results);
    }

    private static Object cover(){try{var c=Class.forName("dev.animal.afmr.SurfaceCover").getDeclaredConstructor();c.setAccessible(true);return c.newInstance();}catch(Exception e){throw new RuntimeException(e);}}
    private static int height(Object cover,ProtoChunk chunk,int tick,int x,int z){try{var method=cover.getClass().getDeclaredMethod("height",net.minecraft.world.level.chunk.ChunkAccess.class,long.class,int.class,int.class);method.setAccessible(true);return (int)method.invoke(cover,chunk,tick,x,z);}catch(Exception e){throw new RuntimeException(e);}}
    private static void clear(Object cover){try{var method=cover.getClass().getDeclaredMethod("clear");method.setAccessible(true);method.invoke(cover);}catch(Exception e){throw new RuntimeException(e);}}
}

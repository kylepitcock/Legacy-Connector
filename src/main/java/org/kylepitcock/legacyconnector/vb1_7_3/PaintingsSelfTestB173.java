package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.decoration.painting.PaintingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Explicit graphics-free native-region painting proof; never registered or run unless a test calls run. */
public final class PaintingsSelfTestB173 {
    private PaintingsSelfTestB173(){}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    public static String run(ServerLevel level){
        var step=ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);int cx=30_000,cz=30_000;
        var chunks=new java.util.HashMap<ChunkPos,ProtoChunk>();var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        var cache=StaticCache2D.<GenerationChunkHolder>create(cx,cz,step.directDependencies().getRadius(),(x,z)->{
            var pos=new ChunkPos(x,z);var chunk=new ProtoChunk(pos,UpgradeData.EMPTY,level,biomes,null);chunk.setPersistedStatus(ChunkStatus.FEATURES);chunks.put(pos,chunk);
            return new GenerationChunkHolder(pos){@Override public int getTicketLevel(){return 33;}@Override public int getQueueLevel(){return 0;}@Override public net.minecraft.world.level.chunk.ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status){return chunk;}};
        });
        var center=chunks.get(new ChunkPos(cx,cz));var region=new WorldGenRegion(level,cache,step,center);var world=World.mcc$wrap(level);
        try(var scope=World.mcc$generation(region)){
            int x=cx*16+2,z=cz*16+2,y=220;
            var painting=new PaintingEntity(world,x,y,z,2,"Kebab");require(!painting.canStayAttached(),"unsupported original painting rejected");
            world.setBlockWithoutNotifyingNeighbors(x,y,z,1);require(painting.canStayAttached(),"region-native wall supports original painting");
            require(world.spawnEntity(painting),"original painting enters native proto entity storage");
            require(painting.mcc$modern instanceof net.minecraft.world.entity.decoration.Painting nativePainting&&nativePainting.getDirection()==Direction.SOUTH&&nativePainting.getPos().equals(new BlockPos(x,y,z+1)),"Beta2 maps to native SOUTH on front side of attachment block");
            require(painting.canStayAttached(),"spawned original view excludes itself from hanging overlap checks");
            require(!new PaintingEntity(world,x,y,z,2,"Kebab").canStayAttached(),"new original painting cannot overlap earlier generated painting");
            int farX=x+48;world.setBlockWithoutNotifyingNeighbors(farX,y,z,1);
            require(region.getBlockState(new BlockPos(farX,y,z)).isAir(),"deferred wall is not force-written outside native radius");
            var far=new PaintingEntity(world,farX,y,z,2,"Kebab");require(far.canStayAttached()&&world.spawnEntity(far),"pending wall supports painting without a full-level chunk load");
            require(center.getEntities().stream().anyMatch(tag->"minecraft:painting".equals(tag.getString("id"))&&tag.hasUUID("UUID")&&tag.getUUID("UUID").equals(painting.mcc$modern.getUUID())),"native painting ID/UUID serialized in center proto chunk");
            var target=chunks.get(new ChunkPos(cx+3,cz));require(target.getEntities().stream().anyMatch(tag->"minecraft:painting".equals(tag.getString("id"))&&tag.getUUID("UUID").equals(far.mcc$modern.getUUID())),"deferred-wall painting serialized in adjacent proto chunk");
            var targetRegion=new WorldGenRegion(level,cache,step,target);org.kylepitcock.legacyconnector.v1_7_10.bridge.DeferredGenWrites.applyDuringGeneration(targetRegion,cx+3,cz);
            require(target.getBlockState(new BlockPos(farX,y,z)).is(net.minecraft.world.level.block.Blocks.STONE),"queued original painting support materializes through native generation");
        }finally{EntityViewsB173.flush();}
        return "passed: Beta attachment/motive, native painting proto ID/UUID, overlap rejection, deferred support without full-level load";
    }
}

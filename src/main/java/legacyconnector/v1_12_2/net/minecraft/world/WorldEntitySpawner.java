package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;

/**
 * 1.12.2 mob spawning helpers. Natural spawning is the modern game's; {@link #performWorldGenSpawning} (mod chunk generators call it
 * when they populate a chunk) runs the modern chunk-generation spawning for that chunk with the modern biome there.
 */
public final class WorldEntitySpawner {
    public static final int MOB_COUNT_DIV = (int) Math.pow(17.0D, 2.0D);

    /** Natural spawning runs in the modern level; this reports no extra spawns. */
    public int findChunksForSpawning(WorldServer worldServerIn, boolean spawnHostileMobs, boolean spawnPeacefulMobs, boolean spawnOnSetTickRate) {
        return 0;
    }

    /** 1.12's placement test: on solid ground with room above, in water, or in open air. */
    public static boolean canCreatureTypeSpawnAtLocation(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving.SpawnPlacementType spawnPlacementTypeIn, World worldIn, BlockPos pos) {
        if (spawnPlacementTypeIn == null || worldIn == null || pos == null) return false;
        var access = worldIn.mcc$access();
        if (access == null || !access.getWorldBorder().isWithinBounds(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()))) return false;
        var here = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        var state = access.getBlockState(here);
        switch (spawnPlacementTypeIn) {
            case IN_WATER:
                return state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && access.getBlockState(here.below()).getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                        && !access.getBlockState(here.above()).isCollisionShapeFullBlock(access, here.above());
            case IN_AIR:
                return !state.isCollisionShapeFullBlock(access, here) && state.getFluidState().isEmpty();
            default:
                var below = here.below();
                if (!access.getBlockState(below).isFaceSturdy(access, below, net.minecraft.core.Direction.UP)) return false;
                var belowBlock = access.getBlockState(below).getBlock();
                return belowBlock != net.minecraft.world.level.block.Blocks.BEDROCK && belowBlock != net.minecraft.world.level.block.Blocks.BARRIER
                        && !state.isCollisionShapeFullBlock(access, here) && state.getFluidState().isEmpty()
                        && !access.getBlockState(here.above()).isCollisionShapeFullBlock(access, here.above());
        }
    }

    public static boolean isValidEmptySpawnBlock(IBlockState state) {
        if (state == null) return true;
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state);
        return !modern.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO)
                && !modern.isSignalSource() && modern.getFluidState().isEmpty() && !modern.is(net.minecraft.tags.BlockTags.PREVENT_MOB_SPAWNING_INSIDE);
    }

    /**
     * Spawns the creatures a newly generated chunk starts with: the modern chunk-generation spawning (the modern biome's creature
     * list, as 1.12 used the biome's) for the chunk at {@code centerX, centerZ}.
     */
    public static void performWorldGenSpawning(World worldIn, Biome biomeIn, int centerX, int centerZ, int diameterX, int diameterZ, Random randomIn) {
        if (worldIn == null) return;
        var access = worldIn.mcc$access();
        if (!(access instanceof net.minecraft.world.level.ServerLevelAccessor server)) return;
        try {
            var pos = new net.minecraft.core.BlockPos(centerX + 8, worldIn.getSeaLevel(), centerZ + 8);
            var chunk = new net.minecraft.world.level.ChunkPos(pos);
            var holder = server.getBiome(pos);
            net.minecraft.world.level.NaturalSpawner.spawnMobsForChunkGeneration(server, holder, chunk,
                    net.minecraft.util.RandomSource.create(randomIn == null ? 0L : randomIn.nextLong()));
        } catch (RuntimeException e) {
            // generation must not fail for its creatures
        }
    }
}

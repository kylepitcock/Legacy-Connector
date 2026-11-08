package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * Stands in for {@code net.minecraft.world.level.block.grower.AbstractTreeGrower} (1.20.1); {@code TreeGrower} is a final class of keys in 1.21.1. A sapling built
 * from a subclass gets a {@code TreeGrower} for the keys this object answers (see {@link ApiCompat#saplingGrower}).
 */
public abstract class LegacyTreeGrower {
    @Nullable
    protected abstract ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers);

    public boolean growTree(ServerLevel level, ChunkGenerator generator, BlockPos pos, BlockState state, RandomSource random) {
        ResourceKey<ConfiguredFeature<?, ?>> key = getConfiguredFeature(random, hasFlowers(level, pos));
        if (key == null) return false;
        var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(key);
        if (feature.isEmpty()) return false;
        BlockState air = Blocks.AIR.defaultBlockState();
        level.setBlock(pos, air, 4);
        if (feature.get().value().place(level, generator, random, pos)) {
            if (level.getBlockState(pos) == air) level.sendBlockUpdated(pos, state, air, 2);
            return true;
        }
        level.setBlock(pos, state, 4);
        return false;
    }

    private static boolean hasFlowers(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.MutableBlockPos.betweenClosed(pos.below().north(2).west(2), pos.above().south(2).east(2))) {
            if (level.getBlockState(p).is(BlockTags.FLOWERS)) return true;
        }
        return false;
    }
}

package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/** Stands in for 1.20.1's {@code AbstractMegaTreeGrower}: a 2x2 patch of saplings grows the mega feature, anything else the normal one. */
public abstract class LegacyMegaTreeGrower extends LegacyTreeGrower {
    @Nullable
    protected abstract ResourceKey<ConfiguredFeature<?, ?>> getConfiguredMegaFeature(RandomSource random);

    @Override
    public boolean growTree(ServerLevel level, ChunkGenerator generator, BlockPos pos, BlockState state, RandomSource random) {
        for (int dx = 0; dx >= -1; dx--) {
            for (int dz = 0; dz >= -1; dz--) {
                if (isTwoByTwoSapling(state, level, pos, dx, dz) && placeMega(level, generator, pos, state, random, dx, dz)) return true;
            }
        }
        return super.growTree(level, generator, pos, state, random);
    }

    private static boolean isTwoByTwoSapling(BlockState state, BlockGetter level, BlockPos pos, int dx, int dz) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return level.getBlockState(pos.offset(dx, 0, dz)).is(block) && level.getBlockState(pos.offset(dx + 1, 0, dz)).is(block)
                && level.getBlockState(pos.offset(dx, 0, dz + 1)).is(block) && level.getBlockState(pos.offset(dx + 1, 0, dz + 1)).is(block);
    }

    private boolean placeMega(ServerLevel level, ChunkGenerator generator, BlockPos pos, BlockState state, RandomSource random, int dx, int dz) {
        ResourceKey<ConfiguredFeature<?, ?>> key = getConfiguredMegaFeature(random);
        if (key == null) return false;
        var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(key);
        if (feature.isEmpty()) return false;
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) level.setBlock(pos.offset(dx + i, 0, dz + j), air, 4);
        if (feature.get().value().place(level, generator, random, pos.offset(dx, 0, dz))) return true;
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) level.setBlock(pos.offset(dx + i, 0, dz + j), state, 4);
        return false;
    }
}

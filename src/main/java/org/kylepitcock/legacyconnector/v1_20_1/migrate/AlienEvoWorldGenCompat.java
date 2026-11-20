package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;

/**
 * Reads the generator-stored biome at the candidate's exact quart coordinates.
 * Calling getUncachedNoiseBiome bypasses BiomeManager's jittered neighborhood
 * interpolation, which can otherwise sample unavailable adjacent chunks.
 */
public final class AlienEvoWorldGenCompat {
    private AlienEvoWorldGenCompat() {}

    public static Holder<Biome> getBiomeForFeature(WorldGenLevel level, BlockPos pos) {
        return level.getUncachedNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
    }
}

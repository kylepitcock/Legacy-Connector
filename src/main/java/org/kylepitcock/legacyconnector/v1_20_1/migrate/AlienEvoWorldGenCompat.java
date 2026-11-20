package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;

/**
 * Samples the exact quart biome for a feature candidate without BiomeManager's
 * jittered neighborhood interpolation. This keeps generation inside the
 * WorldGenRegion's declared chunk dependencies.
 */
public final class AlienEvoWorldGenCompat {
    private AlienEvoWorldGenCompat() {}

    public static Holder<Biome> getBiomeForFeature(WorldGenLevel level, BlockPos pos) {
        return level.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
    }
}

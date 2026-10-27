package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * Places a modern configured feature for a legacy vanilla generator (big trees, mushrooms, fossils, dungeons...): the modern
 * feature is vanilla's own successor, so a mod calling 1.12's generator gets what the modern game would place there.
 */
public final class ModernFeatures1122 {
    private ModernFeatures1122() {}

    private static int failures;

    public static boolean place(World world, ResourceKey<ConfiguredFeature<?, ?>> key, Random rand, BlockPos pos) {
        if (world == null || key == null || pos == null) return false;
        if (!(world.mcc$access() instanceof WorldGenLevel level) || !(world.mcc$level() instanceof net.minecraft.server.level.ServerLevel server)) return false;
        try {
            var holder = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).getHolder(key);
            if (holder.isEmpty()) return false;
            var random = net.minecraft.util.RandomSource.create(rand == null ? 0L : rand.nextLong());
            return holder.get().value().place(level, server.getChunkSource().getGenerator(), random,
                    new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        } catch (RuntimeException e) {
            if (failures++ < 3) org.slf4j.LoggerFactory.getLogger("LegacyConnector/WorldGen").warn("Feature {} failed at {}: {}", key.location(), pos, e.toString());
            return false;
        }
    }
}

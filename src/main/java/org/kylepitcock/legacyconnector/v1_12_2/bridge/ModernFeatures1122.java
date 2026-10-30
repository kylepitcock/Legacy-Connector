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

    /** The world's generation level (the generation region while a generator runs, else the live level), or null for a detached world. */
    public static WorldGenLevel level(World world) {
        return world != null && world.mcc$access() instanceof WorldGenLevel level ? level : null;
    }

    /** Places a modern feature with an explicit configuration (vanilla generators whose legacy form carried their settings). */
    public static <FC extends net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration> boolean placeFeature(World world,
            net.minecraft.world.level.levelgen.feature.Feature<FC> feature, FC config, Random rand, BlockPos pos) {
        if (world == null || feature == null || pos == null) return false;
        WorldGenLevel level = level(world);
        if (level == null || !(world.mcc$level() instanceof net.minecraft.server.level.ServerLevel server)) return false;
        try {
            var random = net.minecraft.util.RandomSource.create(rand == null ? 0L : rand.nextLong());
            return feature.place(config, level, server.getChunkSource().getGenerator(), random, new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        } catch (RuntimeException e) {
            if (failures++ < 3) org.slf4j.LoggerFactory.getLogger("LegacyConnector/WorldGen").warn("Feature {} failed at {}: {}", feature, pos, e.toString());
            return false;
        }
    }

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

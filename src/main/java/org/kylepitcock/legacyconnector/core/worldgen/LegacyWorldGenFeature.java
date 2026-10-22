package org.kylepitcock.legacyconnector.core.worldgen;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.slf4j.LoggerFactory;

/**
 * One feature placed once per chunk in every biome (via a generated NeoForge biome modifier), which runs every
 * registered legacy chunk decorator. Legacy decorators wrote into the 2x2 chunk area offset by +8; a modern
 * WorldGenRegion allows writes into all neighbours of the decorated chunk, so those writes are legal.
 */
public class LegacyWorldGenFeature extends Feature<NoneFeatureConfiguration> {
    private static final java.util.concurrent.atomic.AtomicInteger FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    /** Implemented by each legacy family's world generator bridge. */
    public interface Decorator {
        void decorate(WorldGenLevel level, int chunkX, int chunkZ, long seed);
    }

    private static final List<Decorator> DECORATORS = new CopyOnWriteArrayList<>();

    public LegacyWorldGenFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public static void addDecorator(Decorator decorator) {
        DECORATORS.add(decorator);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (DECORATORS.isEmpty()) return false;
        WorldGenLevel level = ctx.level();
        int chunkX = ctx.origin().getX() >> 4;
        int chunkZ = ctx.origin().getZ() >> 4;
        for (Decorator d : DECORATORS) {
            try {
                d.decorate(level, chunkX, chunkZ, level.getSeed());
            } catch (RuntimeException | LinkageError e) {
                int n = FAILURES.incrementAndGet();
                if (n <= 3) {
                    LoggerFactory.getLogger("LegacyConnector/WorldGen").error("Legacy world generator failed in chunk {},{}", chunkX, chunkZ, e);
                } else if (n % 200 == 0) {
                    LoggerFactory.getLogger("LegacyConnector/WorldGen").error("Legacy world generators failed {} times so far (last: {})", n, e.toString());
                }
            }
        }
        return true;
    }
}

package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Feature type {@code legacyconnector:legacy_worldgen_deep}: the deep-layer pass of one 1.7.10 {@code IWorldGenerator} in one
 * chunk (see {@link DeepLayer1710}). The placed y is where legacy y=0 lands; the generator writes from there up to modern y=0.
 */
public final class LegacyDeepWorldGenFeature extends Feature<LegacyDeepWorldGenFeature.Config> {
    /** {@code generator}: the generator's deep feature id ({@link DeepLayer1710.Generator#id()}). */
    public record Config(String generator) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("generator").forGetter(Config::generator)).apply(i, Config::new));
    }

    private static final java.util.concurrent.atomic.AtomicInteger FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    public LegacyDeepWorldGenFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        try {
            return DeepLayer1710.place(ctx.level(), ctx.config().generator(), ctx.origin().getX() >> 4, ctx.origin().getZ() >> 4, ctx.origin().getY());
        } catch (RuntimeException | LinkageError e) {
            // world generation must never throw (a generator's own failures are logged by DeepLayer1710)
            int n = FAILURES.incrementAndGet();
            if (n <= 3 || n % 200 == 0) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/WorldGen").error("Deep layer feature {} failed ({} failures so far)", ctx.config().generator(), n, e);
            }
            return false;
        }
    }
}

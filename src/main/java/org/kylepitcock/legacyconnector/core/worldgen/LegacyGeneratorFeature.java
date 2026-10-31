package org.kylepitcock.legacyconnector.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code legacyconnector:legacy_generator}: runs exactly one legacy world generator (named by the config, see {@link LegacyGenerators})
 * for the chunk the feature is placed in, with the legacy per-chunk seeding and the mod's generation lock. The feature's own random is
 * not used: the legacy seed formula makes generation the same as on the legacy version. Never throws: a failing generator is logged
 * (throttled) and the chunk goes on.
 */
public class LegacyGeneratorFeature extends Feature<LegacyGeneratorFeature.Config> {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/WorldGen");
    private static final AtomicInteger FAILURES = new AtomicInteger();
    private static final Set<ResourceLocation> UNKNOWN = ConcurrentHashMap.newKeySet();

    /** The id of the legacy generator to run ({@code <modid>:legacy_worldgen/<name>}). */
    public record Config(ResourceLocation generator) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("generator").forGetter(Config::generator)).apply(i, Config::new));
    }

    public LegacyGeneratorFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        ResourceLocation id = ctx.config().generator();
        LegacyGenerators.Generator g = LegacyGenerators.byId(id);
        if (g == null) {
            if (UNKNOWN.add(id)) LOG.warn("No legacy world generator {} is loaded; its feature does nothing", id);
            return false;
        }
        int chunkX = ctx.origin().getX() >> 4;
        int chunkZ = ctx.origin().getZ() >> 4;
        try {
            g.runner().run(ctx.level(), chunkX, chunkZ);
            return true;
        } catch (RuntimeException | LinkageError e) {
            int n = FAILURES.incrementAndGet();
            if (n <= 3) {
                LOG.error("Legacy world generator {} ({}) failed in chunk {},{}", id, g.generator().getClass().getName(), chunkX, chunkZ, e);
            } else if (n % 200 == 0) {
                LOG.error("Legacy world generators failed {} times so far (last: {}: {})", n, id, e.toString());
            }
            return false;
        }
    }
}

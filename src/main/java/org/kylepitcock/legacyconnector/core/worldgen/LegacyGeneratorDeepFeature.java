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
 * {@code legacyconnector:legacy_generator_deep}: the shifted pass of one legacy world generator (see {@link LegacyGenerators.DeepRunner}).
 * The placed y is where legacy y=0 lands (default: the dimension's bottom, {@code above_bottom 0}); a y at or above 0 (the Nether, the
 * End) runs nothing. Never throws.
 */
public class LegacyGeneratorDeepFeature extends Feature<LegacyGeneratorDeepFeature.Config> {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/WorldGen");
    private static final AtomicInteger FAILURES = new AtomicInteger();
    private static final Set<ResourceLocation> UNKNOWN = ConcurrentHashMap.newKeySet();

    /** The id of the legacy generator ({@code <modid>:legacy_worldgen/<name>}) whose shifted pass this is. */
    public record Config(ResourceLocation generator) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("generator").forGetter(Config::generator)).apply(i, Config::new));
    }

    public LegacyGeneratorDeepFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        ResourceLocation id = ctx.config().generator();
        LegacyGenerators.Generator g = LegacyGenerators.byId(id);
        if (g == null || g.deepRunner() == null) {
            if (UNKNOWN.add(id)) LOG.warn("No legacy world generator {} with a shifted pass is loaded; its deep feature does nothing", id);
            return false;
        }
        int bottom = Math.max(ctx.origin().getY(), ctx.level().getMinBuildHeight());
        if (bottom >= 0 || !LegacyGenerators.shifted(g)) return false;
        try {
            g.deepRunner().run(ctx.level(), ctx.origin().getX() >> 4, ctx.origin().getZ() >> 4, bottom);
            return true;
        } catch (RuntimeException | LinkageError e) {
            int n = FAILURES.incrementAndGet();
            if (n <= 3 || n % 200 == 0) {
                LOG.error("Legacy world generator {} ({}) failed in its shifted pass ({} failures so far)", id, g.generator().getClass().getName(), n, e);
            }
            return false;
        }
    }
}

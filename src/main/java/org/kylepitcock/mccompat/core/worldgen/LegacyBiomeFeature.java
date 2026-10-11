package org.kylepitcock.mccompat.core.worldgen;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.slf4j.LoggerFactory;

/**
 * Feature listed in every generated legacy biome (vegetal decoration step): lets the legacy family apply the legacy
 * biome's surface blocks and run its own decorator for the chunk.
 */
public class LegacyBiomeFeature extends Feature<NoneFeatureConfiguration> {
    public interface Hook {
        boolean decorate(FeaturePlaceContext<NoneFeatureConfiguration> ctx);
    }

    private static final List<Hook> HOOKS = new CopyOnWriteArrayList<>();

    public LegacyBiomeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public static void addHook(Hook hook) {
        HOOKS.add(hook);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        boolean any = false;
        for (Hook h : HOOKS) {
            try {
                any |= h.decorate(ctx);
            } catch (RuntimeException | LinkageError e) {
                LoggerFactory.getLogger("Retrofit/WorldGen").error("Legacy biome decoration failed at {}", ctx.origin(), e);
            }
        }
        return any;
    }
}

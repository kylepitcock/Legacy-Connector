package legacyconnector.v1_20_1.net.minecraftforge.event.level;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Forge 1.20.1's sapling growth event. NeoForge 1.21.1 has no event for it (tree growth is a feature placement now), so this stand-in lets mods
 * register their handlers (the registration is what used to fail); it is not fired.
 */
public class SaplingGrowTreeEvent extends LevelEvent {
    private final BlockPos pos;
    private final RandomSource randomSource;

    public SaplingGrowTreeEvent(LevelAccessor level, RandomSource randomSource, BlockPos pos) {
        super(level);
        this.pos = pos;
        this.randomSource = randomSource;
    }

    public BlockPos getPos() {
        return pos;
    }

    public RandomSource getRandomSource() {
        return randomSource;
    }

    private net.minecraft.core.Holder<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> feature;

    /** The feature the tree would grow with (never fired here, so only what a handler set). */
    public net.minecraft.core.Holder<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> getFeature() {
        return feature;
    }

    public void setFeature(net.minecraft.core.Holder<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> feature) {
        this.feature = feature;
    }

    /** {@code setFeature(key)} of 1.20.1 (the feature as a resource key): looked up in the level's registries; an unknown key leaves the feature unchanged. */
    public void setFeature(net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> key) {
        if (getLevel() instanceof net.minecraft.world.level.Level level) {
            level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).getHolder(key).ifPresent(h -> this.feature = h);
        }
    }
}

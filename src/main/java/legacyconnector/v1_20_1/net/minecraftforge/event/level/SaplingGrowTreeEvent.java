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
}

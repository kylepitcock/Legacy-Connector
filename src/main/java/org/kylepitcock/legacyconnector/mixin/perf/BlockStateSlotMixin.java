package org.kylepitcock.legacyconnector.mixin.perf;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.kylepitcock.legacyconnector.core.perf.LegacyStateSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** perf: the 1.7.10 metadata of a vanilla state (BlockBridge.legacyMeta), remembered on the state; see {@link LegacyStateSlot}. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateSlotMixin implements LegacyStateSlot {
    @Unique
    private int legacyconnector$meta1710;

    @Override
    public int legacyconnector$meta1710() {
        return legacyconnector$meta1710;
    }

    @Override
    public void legacyconnector$setMeta1710(int metaPlusOne) {
        legacyconnector$meta1710 = metaPlusOne;
    }
}

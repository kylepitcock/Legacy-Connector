package org.kylepitcock.legacyconnector.v1_7_10.adapter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The collision context of a legacy {@code World.rayTraceBlocks}. It behaves like {@link CollisionContext#empty()}, but
 * lets a legacy block tell its ray trace apart from the outline the client draws: in 1.7.10 the trace used the block's
 * bounds while the selection box (what is drawn) could be empty (Thaumcraft's aura nodes answer a zero-size selection
 * box, yet the wand finds them through their 0.3-0.7 bounds).
 */
public final class LegacyRayContext implements CollisionContext {
    public static final LegacyRayContext INSTANCE = new LegacyRayContext();

    private LegacyRayContext() {}

    @Override
    public boolean isDescending() {
        return CollisionContext.empty().isDescending();
    }

    @Override
    public boolean isAbove(VoxelShape shape, BlockPos pos, boolean canAscend) {
        return CollisionContext.empty().isAbove(shape, pos, canAscend);
    }

    @Override
    public boolean isHoldingItem(Item item) {
        return false;
    }

    @Override
    public boolean canStandOnFluid(FluidState above, FluidState below) {
        return false;
    }
}

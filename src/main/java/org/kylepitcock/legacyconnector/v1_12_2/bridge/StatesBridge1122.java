package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;

/** Converts between 1.12.2 block states (properties) and modern ones (the META property of a legacy block, or a vanilla state). */
public final class StatesBridge1122 {
    private StatesBridge1122() {}

    /** The legacy block of a modern block (a legacy mod block or a vanilla one), or null. */
    public static Block legacyBlock(BlockState state) {
        return state.getBlock() instanceof Legacy1122Block b ? b.legacy() : Vanilla1122.legacyBlockOf(state.getBlock());
    }

    public static IBlockState toLegacy(BlockState state) {
        if (state.getBlock() instanceof Legacy1122Block b) {
            try {
                return b.legacy().getStateFromMeta(state.getValue(Legacy1122Block.META));
            } catch (RuntimeException | LinkageError e) {
                return b.legacy().getDefaultState();
            }
        }
        Block vanilla = Vanilla1122.legacyBlockOf(state.getBlock());
        return vanilla == null ? null : vanilla.getDefaultState();
    }

    public static BlockState toModern(IBlockState state) {
        if (state == null) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        Block legacy = state.getBlock();
        if (legacy.mcc$modern instanceof Legacy1122Block b) {
            int meta;
            try {
                meta = legacy.getMetaFromState(state) & 15;
            } catch (RuntimeException | LinkageError e) {
                meta = 0;
            }
            return b.defaultBlockState().setValue(Legacy1122Block.META, meta);
        }
        if (legacy.mcc$modern instanceof net.minecraft.world.level.block.Block vanilla) return vanilla.defaultBlockState();
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }
}

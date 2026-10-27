package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;

/**
 * The face answers of vanilla blocks for 1.12.2's {@code Block.isSideSolid} and {@code Block.getBlockFaceShape} (both real shim
 * methods now that {@code IBlockAccess} and {@code BlockFaceShape} are): a vanilla block answers from its modern state's sturdy
 * faces; a mod block gets Forge's defaults.
 */
public final class BlockFaces1122 {
    private BlockFaces1122() {}

    /** Kept for the platform's start-up call; the face queries are plain shim methods now. */
    public static void register() {
    }

    /** Whether a vanilla block's face is a full sturdy face (in the world when it is a modern level's view), or null for a mod block. */
    public static Boolean vanillaSturdy(IBlockState state, Object world, BlockPos pos, EnumFacing side) {
        if (state == null || state.getBlock() == null) return null;
        Object modern = state.getBlock().mcc$modern;
        if (!(modern instanceof net.minecraft.world.level.block.Block) || modern instanceof Legacy1122Block) return null;
        try {
            var s = StatesBridge1122.toModern(state);
            var p = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
            Direction d = Direction.byName(side.getName());
            if (d == null) return false;
            if (world instanceof World w && w.mcc$level() != null) return s.isFaceSturdy(w.mcc$level(), p, d);
            return s.isFaceSturdy(EmptyBlockGetter.INSTANCE, p, d);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}

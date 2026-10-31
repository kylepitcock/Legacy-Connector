package org.kylepitcock.legacyconnector.api;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.api.internal.Convert;

/**
 * Block, block state and tile entity conversion. Legacy objects are opaque {@code Object}s (see {@link LegacyAccess}).
 *
 * <p>Legacy blocks keep their state as a metadata number 0-15; Legacy Connector represents it as one modern block with a
 * {@code meta} property (1.7.10: {@code LegacyBlockAdapter.META}; 1.12.2: mapped through the block's own
 * {@code getStateFromMeta}/{@code getMetaFromState}).
 */
public final class LegacyBlocks {
    private LegacyBlocks() {}

    /** The version of the legacy mod a block state belongs to (empty for vanilla and modern blocks). */
    public static Optional<LegacyVersion> versionOf(BlockState state) {
        return Convert.versionOf(state);
    }

    /**
     * The legacy {@code Block} of a modern state: the mod's own block for a legacy mod block, a stand-in for vanilla and modern
     * blocks (the ones the legacy version had are the real vanilla wrappers, so {@code Blocks.stone}-style comparisons work).
     */
    public static Optional<Object> legacyBlock(BlockState state, LegacyVersion version) {
        return Convert.blockToLegacy(state, version);
    }

    /** The legacy metadata value of a state (0-15). */
    public static OptionalInt legacyMeta(BlockState state, LegacyVersion version) {
        return Convert.metaToLegacy(state, version);
    }

    /** 1.12.2 only: the legacy {@code IBlockState} of a modern state. */
    public static Optional<Object> legacyState(BlockState state) {
        return Convert.stateToLegacy(state);
    }

    /** The modern state of a legacy block and metadata (1.7.10 blocks; for 1.12.2 pass a legacy {@code IBlockState} to {@link #fromLegacyState}). */
    public static Optional<BlockState> fromLegacy(Object legacyBlock, int meta) {
        return Convert.blockFromLegacy(legacyBlock, meta);
    }

    /** The modern state of a 1.12.2 {@code IBlockState}. */
    public static Optional<BlockState> fromLegacyState(Object legacyState) {
        return Convert.stateFromLegacy(legacyState);
    }

    /**
     * The legacy tile entity a block entity hosts: the mod's own {@code TileEntity} instance. Empty for block entities that are not
     * hosts of a legacy tile entity (vanilla or modern ones).
     */
    public static Optional<Object> legacyTile(BlockEntity blockEntity) {
        return Convert.tileOf(blockEntity);
    }

    /** As {@link #legacyTile(BlockEntity)} for the block entity at a position. */
    public static Optional<Object> legacyTile(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? Optional.empty() : legacyTile(be);
    }

    /** The version of the legacy tile entity a block entity hosts. */
    public static Optional<LegacyVersion> tileVersion(BlockEntity blockEntity) {
        return Convert.tileVersion(blockEntity);
    }
}

package org.kylepitcock.legacyconnector.v1_7_10.adapter;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.TileEntityBridge;

/** Adapter for legacy blocks that host tile entities. */
public class LegacyEntityBlockAdapter extends LegacyBlockAdapter implements EntityBlock {
    LegacyEntityBlockAdapter(Block legacy, BlockBehaviour.Properties properties) {
        super(legacy, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Every metadata value gets a host: the modern block is an EntityBlock as a whole, and vanilla warns about (and
        // re-tries to load) block entities of such blocks that were never created. Metadata without a tile entity
        // leaves the host empty (see LegacyBlockEntity.legacy) and unticked.
        return new LegacyBlockEntity(pos, state);
    }

    /** 1.7.10 ticked every tile entity whose {@code canUpdate()} returned true, on both sides. */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != TileEntityBridge.type() || !((Block) legacy()).hasTileEntity(state.getValue(META))) return null;
        return (lvl, pos, st, be) -> ((LegacyBlockEntity) be).tick();
    }
}

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
        // The modern block is an EntityBlock as a whole, but only the metadata values whose legacy hasTileEntity is true
        // get a host. An empty host for every other placement (a mod's ore or plant block sharing its class with a machine)
        // was created for every such block world generation placed: hundreds of thousands per few minutes of play.
        try {
            if (!((Block) legacy()).hasTileEntity(state.getValue(META))) return null;
        } catch (RuntimeException | LinkageError e) {
            // unknown: keep the host
        }
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

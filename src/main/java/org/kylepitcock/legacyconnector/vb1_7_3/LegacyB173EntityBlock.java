package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The modern block of a b1.7.3 {@code BlockContainer}: its tile entity lives in a {@link LegacyB173BlockEntity}, ticked on the server. */
public class LegacyB173EntityBlock extends LegacyB173Block implements EntityBlock {
    public LegacyB173EntityBlock(Block legacy, Properties properties) {
        super(legacy, properties);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LegacyB173BlockEntity(pos, state);
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != TileEntitiesB173.type()) return null;
        return (lvl, pos, st, be) -> ((LegacyB173BlockEntity) be).tickServer();
    }
}

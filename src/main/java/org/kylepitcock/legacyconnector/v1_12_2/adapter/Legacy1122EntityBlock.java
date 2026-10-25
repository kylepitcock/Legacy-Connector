package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122;

/** A 1.12.2 block that has a tile entity. */
public class Legacy1122EntityBlock extends Legacy1122Block implements EntityBlock {
    public Legacy1122EntityBlock(Block legacy, Properties properties) {
        super(legacy, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Legacy1122BlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != TileEntityBridge1122.type()) return null;
        return (lvl, pos, st, be) -> ((Legacy1122BlockEntity) be).tick();
    }
}

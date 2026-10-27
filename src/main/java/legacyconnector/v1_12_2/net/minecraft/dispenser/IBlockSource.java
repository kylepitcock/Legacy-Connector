package legacyconnector.v1_12_2.net.minecraft.dispenser;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** The dispenser (or dropper) that is dispensing: its centre, position, state and tile entity. */
public interface IBlockSource extends ILocatableSource {
    @Override
    double getX();

    @Override
    double getY();

    @Override
    double getZ();

    BlockPos getBlockPos();

    IBlockState getBlockState();

    <T extends TileEntity> T getBlockTileEntity();
}

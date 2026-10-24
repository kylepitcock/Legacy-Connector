package legacyconnector.v1_7_10.net.minecraft.dispenser;

import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The block a dispenser behaviour acts for: its position (the block's centre), metadata and tile entity. */
public interface IBlockSource extends IPosition {
    @Override
    double getX();

    @Override
    double getY();

    @Override
    double getZ();

    int getXInt();

    int getYInt();

    int getZInt();

    int getBlockMetadata();

    TileEntity getBlockTileEntity();

    World getWorld();
}

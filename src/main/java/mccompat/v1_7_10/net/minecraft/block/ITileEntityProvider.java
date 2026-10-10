package mccompat.v1_7_10.net.minecraft.block;

import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.world.World;

public interface ITileEntityProvider {
    TileEntity createNewTileEntity(World world, int meta);
}

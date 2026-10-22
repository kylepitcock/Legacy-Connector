package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public interface ITileEntityProvider {
    TileEntity createNewTileEntity(World world, int meta);
}

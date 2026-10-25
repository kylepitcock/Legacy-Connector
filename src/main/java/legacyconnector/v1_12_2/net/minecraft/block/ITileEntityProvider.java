package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

public interface ITileEntityProvider {
    TileEntity createNewTileEntity(World world, int meta);
}

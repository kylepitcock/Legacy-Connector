package legacyconnector.v1_7_10.net.minecraft.world;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

public interface IBlockAccess {
    Block getBlock(int x, int y, int z);

    TileEntity getTileEntity(int x, int y, int z);

    int getLightBrightnessForSkyBlocks(int x, int y, int z, int min);

    int getBlockMetadata(int x, int y, int z);

    int isBlockProvidingPowerTo(int x, int y, int z, int side);

    boolean isAirBlock(int x, int y, int z);

    int getHeight();

    boolean extendedLevelsInChunkCache();

    boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue);
}

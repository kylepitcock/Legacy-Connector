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

    legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoords(int x, int z);

    boolean extendedLevelsInChunkCache();

    boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue);

    /** The biome-blended tint of the modern world at a position: kind 0 grass, 1 foliage; white where unknown. */
    default int mcc$blockTint(int x, int y, int z, int kind) {
        return 0xFFFFFF;
    }
}

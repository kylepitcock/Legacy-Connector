package legacyconnector.v1_12_2.net.minecraft.world;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;

/**
 * 1.12.2 read access to blocks: implemented by {@link World} (and its generation-region views) and by mods' own block views
 * (fake worlds, render caches). Forge's {@code isSideSolid} is part of it.
 */
public interface IBlockAccess {
    TileEntity getTileEntity(BlockPos pos);

    /** Packed light: sky light {@code << 20 | block light << 4}, block light at least {@code lightValue}. */
    int getCombinedLight(BlockPos pos, int lightValue);

    IBlockState getBlockState(BlockPos pos);

    boolean isAirBlock(BlockPos pos);

    Biome getBiome(BlockPos pos);

    int getStrongPower(BlockPos pos, EnumFacing direction);

    WorldType getWorldType();

    boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default);
}

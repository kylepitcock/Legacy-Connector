package legacyconnector.v1_12_2.net.minecraft.world;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2 read access to blocks (worlds, chunk caches, mods' fake worlds), with Forge's {@code isSideSolid}. */
public interface IBlockAccess {
    TileEntity getTileEntity(BlockPos pos);

    int getCombinedLight(BlockPos pos, int lightValue);

    IBlockState getBlockState(BlockPos pos);

    boolean isAirBlock(BlockPos pos);

    legacyconnector.v1_12_2.net.minecraft.world.biome.Biome getBiome(BlockPos pos);

    int getStrongPower(BlockPos pos, EnumFacing direction);

    WorldType getWorldType();

    boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default);
}

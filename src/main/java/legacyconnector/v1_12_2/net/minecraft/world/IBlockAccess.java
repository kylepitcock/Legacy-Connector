package legacyconnector.v1_12_2.net.minecraft.world;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/**
 * 1.12.2 read access to blocks. Area R needs only the type (block colours and block model rendering take one); area W owns the full
 * interface: on a merge conflict take W's version (a superset of this one).
 */
public interface IBlockAccess {
    TileEntity getTileEntity(BlockPos pos);

    /** Packed light: sky light {@code << 20 | block light << 4}, block light at least {@code lightValue}. */
    int getCombinedLight(BlockPos pos, int lightValue);

    IBlockState getBlockState(BlockPos pos);

    boolean isAirBlock(BlockPos pos);
}

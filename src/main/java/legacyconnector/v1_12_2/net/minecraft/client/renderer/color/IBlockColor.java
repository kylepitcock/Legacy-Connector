package legacyconnector.v1_12_2.net.minecraft.client.renderer.color;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** The tint of a block's quads with a tint index (world and position are null when drawn as an item). */
public interface IBlockColor {
    int colorMultiplier(IBlockState state, IBlockAccess worldIn, BlockPos pos, int tintIndex);
}

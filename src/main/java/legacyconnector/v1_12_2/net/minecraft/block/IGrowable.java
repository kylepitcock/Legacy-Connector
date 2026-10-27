package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A block bone meal grows (crops, saplings, grass); the modern block forwards bone meal to it. */
public interface IGrowable {
    boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient);

    boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state);

    void grow(World world, Random rand, BlockPos pos, IBlockState state);
}

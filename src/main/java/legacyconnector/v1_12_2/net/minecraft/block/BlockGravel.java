package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** 1.12.2 gravel: falls, and drops flint one time in ten (more often with fortune). */
public class BlockGravel extends BlockFalling {
    public BlockGravel() {
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        if (fortune > 3) fortune = 3;
        return rand.nextInt(10 - fortune * 3) == 0 ? legacyconnector.v1_12_2.net.minecraft.init.Items.FLINT : super.getItemDropped(state, rand, fortune);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return MapColor.STONE;
    }
}

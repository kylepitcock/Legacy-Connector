package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 frosted ice (frost walker): ages in light and melts to water after four stages, faster with few frosted neighbours. */
public class BlockFrostedIce extends BlockIce {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 3);

    public BlockFrostedIce() {
        setDefaultState(blockState.getBaseState().withProperty(AGE, 0));
        setCreativeTab(null);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, MathHelper.clamp(meta, 0, 3));
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if ((rand.nextInt(3) == 0 || countNeighbors(worldIn, pos) < 4)
                && worldIn.getLightFromNeighbors(pos) > 11 - state.getValue(AGE) - state.getLightOpacity()) {
            slightlyMelt(worldIn, pos, state, rand, true);
        } else {
            worldIn.scheduleUpdate(pos, this, MathHelper.getInt(rand, 20, 40));
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (blockIn == this && countNeighbors(worldIn, pos) < 2) turnIntoWater(worldIn, pos);
    }

    private int countNeighbors(World worldIn, BlockPos pos) {
        int i = 0;
        for (EnumFacing facing : EnumFacing.values()) {
            if (worldIn.getBlockState(pos.offset(facing)).getBlock() == this) {
                ++i;
                if (i >= 4) return i;
            }
        }
        return i;
    }

    /** Ages the ice one stage, or melts it (and, when asked, ages the frosted ice around it). */
    protected void slightlyMelt(World worldIn, BlockPos pos, IBlockState state, Random rand, boolean meltNeighbors) {
        int age = state.getValue(AGE);
        if (age < 3) {
            worldIn.setBlockState(pos, state.withProperty(AGE, age + 1), 2);
            worldIn.scheduleUpdate(pos, this, MathHelper.getInt(rand, 20, 40));
        } else {
            turnIntoWater(worldIn, pos);
            if (meltNeighbors) {
                for (EnumFacing facing : EnumFacing.values()) {
                    BlockPos p = pos.offset(facing);
                    IBlockState s = worldIn.getBlockState(p);
                    if (s.getBlock() == this) slightlyMelt(worldIn, p, s, rand, false);
                }
            }
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return ItemStack.EMPTY;
    }
}

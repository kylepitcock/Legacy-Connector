package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 beetroots: a crop with four ages that grows a third as fast and only one age per bone meal. */
public class BlockBeetroot extends BlockCrops {
    public static final PropertyInteger BEETROOT_AGE = PropertyInteger.create("age", 0, 3);
    private static final AxisAlignedBB[] BEETROOT_AABB = {new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D), new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.375D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D)};

    @Override
    protected PropertyInteger getAgeProperty() {
        return BEETROOT_AGE;
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    protected Item getSeed() {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.BEETROOT_SEEDS;
    }

    @Override
    protected Item getCrop() {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.BEETROOT;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (rand.nextInt(3) == 0) checkAndDropBlock(worldIn, pos, state);
        else super.updateTick(worldIn, pos, state, rand);
    }

    @Override
    protected int getBonemealAgeIncrease(World worldIn) {
        return super.getBonemealAgeIncrease(worldIn) / 3;
    }

    @Override
    public AxisAlignedBB mcc$staticBounds(IBlockState state) {
        return BEETROOT_AABB[Math.max(0, Math.min(3, getAge(state)))];
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BEETROOT_AGE);
    }
}

package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 cocoa pods: hang on jungle logs (the facing in bits 0-1), age 0-2 (bits 2-3); ripe they drop three beans. */
public class BlockCocoa extends BlockHorizontal implements IGrowable {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 2);

    public BlockCocoa() {
        super(Material.PLANTS);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(AGE, 0));
        setTickRandomly(true);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!canBlockStay(worldIn, pos, state)) {
            dropBlock(worldIn, pos, state);
        } else if (rand.nextInt(5) == 0) {
            int age = state.getValue(AGE);
            if (age < 2) worldIn.setBlockState(pos, state.withProperty(AGE, age + 1), 2);
        }
    }

    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state) {
        IBlockState log = worldIn.getBlockState(pos.offset(state.getValue(FACING)));
        return log.getBlock() == legacyconnector.v1_12_2.net.minecraft.init.Blocks.LOG && log.getValue(BlockOldLog.VARIANT) == BlockPlanks.EnumType.JUNGLE;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        if (!facing.getAxis().isHorizontal()) facing = EnumFacing.NORTH;
        return getDefaultState().withProperty(FACING, facing.getOpposite()).withProperty(AGE, 0);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!canBlockStay(worldIn, pos, state)) dropBlock(worldIn, pos, state);
    }

    private void dropBlock(World worldIn, BlockPos pos, IBlockState state) {
        worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR.getDefaultState(), 3);
        dropBlockAsItem(worldIn, pos, state, 0);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        int count = state.getValue(AGE) >= 2 ? 3 : 1;
        for (int i = 0; i < count; ++i) drops.add(new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.DYE, 1, EnumDyeColor.BROWN.getDyeDamage()));
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.DYE, 1, EnumDyeColor.BROWN.getDyeDamage());
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return state.getValue(AGE) < 2;
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        worldIn.setBlockState(pos, state.withProperty(AGE, state.getValue(AGE) + 1), 2);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta)).withProperty(AGE, (meta & 15) >> 2);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(AGE) << 2;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, AGE);
    }
}

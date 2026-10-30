package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 pumpkin and melon stems: age 0-7 (metadata); grown, they set their crop beside them and face it. */
public class BlockStem extends BlockBush implements IGrowable {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 7);
    public static final PropertyDirection FACING = BlockTorch.FACING;
    private final Block crop;

    protected BlockStem(Block crop) {
        setDefaultState(blockState.getBaseState().withProperty(AGE, 0).withProperty(FACING, EnumFacing.UP));
        this.crop = crop;
        setTickRandomly(true);
        setCreativeTab(null);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        int age = state.getValue(AGE);
        state = state.withProperty(FACING, EnumFacing.UP);
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            if (worldIn.getBlockState(pos.offset(f)).getBlock() == crop && age == 7) return state.withProperty(FACING, f);
        }
        return state;
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return Block.mcc$named(state.getBlock(), "farmland");
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        super.updateTick(worldIn, pos, state, rand);
        if (!worldIn.isAreaLoaded(pos, 1) || worldIn.getLightFromNeighbors(pos.up()) < 9 || rand.nextInt(5) != 0) return;
        int age = state.getValue(AGE);
        if (age < 7) {
            worldIn.setBlockState(pos, state.withProperty(AGE, age + 1), 2);
            return;
        }
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) if (worldIn.getBlockState(pos.offset(f)).getBlock() == crop) return;
        BlockPos target = pos.offset(EnumFacing.Plane.HORIZONTAL.random(rand));
        IBlockState soil = worldIn.getBlockState(target.down());
        Block below = soil.getBlock();
        if (worldIn.isAirBlock(target) && (below.canSustainPlant(soil, worldIn, target.down(), EnumFacing.UP, this) || Block.mcc$named(below, "dirt", "grass"))) {
            worldIn.setBlockState(target, crop.getDefaultState());
        }
    }

    public void growStem(World worldIn, BlockPos pos, IBlockState state) {
        int age = state.getValue(AGE) + 2 + worldIn.rand.nextInt(4);
        worldIn.setBlockState(pos, state.withProperty(AGE, Math.min(7, age)), 2);
    }

    protected Item getSeedItem() {
        if (Block.mcc$named(crop, "pumpkin")) return legacyconnector.v1_12_2.net.minecraft.init.Items.PUMPKIN_SEEDS;
        if (Block.mcc$named(crop, "melon_block")) return legacyconnector.v1_12_2.net.minecraft.init.Items.MELON_SEEDS;
        return null;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Item seed = getSeedItem();
        if (seed == null) return;
        int age = state.getValue(AGE);
        Random rand = world instanceof World w ? w.rand : RANDOM;
        for (int i = 0; i < 3; ++i) if (rand.nextInt(15) <= age) drops.add(new ItemStack(seed));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        Item seed = getSeedItem();
        return seed == null ? ItemStack.EMPTY : new ItemStack(seed);
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return state.getValue(AGE) != 7;
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        growStem(worldIn, pos, state);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, meta & 7);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE, FACING);
    }
}

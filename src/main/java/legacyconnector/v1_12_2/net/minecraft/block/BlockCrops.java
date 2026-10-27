package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.EnumPlantType;

/** 1.12.2 crops (wheat and mods' crops): an age 0-7 growing on farmland with light, bone meal, seeds and the crop when ripe. */
public class BlockCrops extends BlockBush implements IGrowable {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 7);
    private static final AxisAlignedBB[] CROPS_AABB = {new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D), new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.375D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D), new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.625D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D), new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.875D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)};

    protected BlockCrops() {
        setDefaultState(blockState.getBaseState().withProperty(getAgeProperty(), 0));
        setTickRandomly(true);
        setCreativeTab(null);
        setHardness(0.0F);
        setSoundType(SoundType.PLANT);
        disableStats();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return mcc$staticBounds(state);
    }

    @Override
    public AxisAlignedBB mcc$staticBounds(IBlockState state) {
        int age = getAge(state);
        return CROPS_AABB[Math.max(0, Math.min(7, age * 7 / Math.max(1, getMaxAge())))];
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return Block.mcc$named(state.getBlock(), "farmland");
    }

    protected PropertyInteger getAgeProperty() {
        return AGE;
    }

    public int getMaxAge() {
        return 7;
    }

    protected int getAge(IBlockState state) {
        return state.getValue(getAgeProperty());
    }

    public IBlockState withAge(int age) {
        return getDefaultState().withProperty(getAgeProperty(), age);
    }

    public boolean isMaxAge(IBlockState state) {
        return state.getValue(getAgeProperty()) >= getMaxAge();
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        super.updateTick(worldIn, pos, state, rand);
        if (!worldIn.isAreaLoaded(pos, 1)) return;
        if (worldIn.getLightFromNeighbors(pos.up()) >= 9) {
            int i = getAge(state);
            if (i < getMaxAge()) {
                float f = getGrowthChance(this, worldIn, pos);
                if (legacyconnector.v1_12_2.net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn, pos, state, rand.nextInt((int) (25.0F / f) + 1) == 0)) {
                    worldIn.setBlockState(pos, withAge(i + 1), 2);
                    legacyconnector.v1_12_2.net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, pos, state, worldIn.getBlockState(pos));
                }
            }
        }
    }

    public void grow(World worldIn, BlockPos pos, IBlockState state) {
        int i = getAge(state) + getBonemealAgeIncrease(worldIn);
        int j = getMaxAge();
        if (i > j) i = j;
        worldIn.setBlockState(pos, withAge(i), 2);
    }

    protected int getBonemealAgeIncrease(World worldIn) {
        return MathHelper.getInt(worldIn.rand, 2, 5);
    }

    /** The growth odds: wetter and more farmland around helps; the same crop in rows around hinders. */
    protected static float getGrowthChance(Block blockIn, World worldIn, BlockPos pos) {
        float f = 1.0F;
        BlockPos blockpos = pos.down();
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                float f1 = 0.0F;
                IBlockState iblockstate = worldIn.getBlockState(blockpos.add(i, 0, j));
                if (blockIn instanceof legacyconnector.v1_12_2.net.minecraftforge.common.IPlantable plantable
                        && iblockstate.getBlock().canSustainPlant(iblockstate, worldIn, blockpos.add(i, 0, j), EnumFacing.UP, plantable)) {
                    f1 = 1.0F;
                    if (iblockstate.getBlock().isFertile(worldIn, blockpos.add(i, 0, j))) f1 = 3.0F;
                }
                if (i != 0 || j != 0) f1 /= 4.0F;
                f += f1;
            }
        }
        BlockPos north = pos.north(), south = pos.south(), west = pos.west(), east = pos.east();
        boolean flag = blockIn == worldIn.getBlockState(west).getBlock() || blockIn == worldIn.getBlockState(east).getBlock();
        boolean flag1 = blockIn == worldIn.getBlockState(north).getBlock() || blockIn == worldIn.getBlockState(south).getBlock();
        if (flag && flag1) {
            f /= 2.0F;
        } else {
            boolean flag2 = blockIn == worldIn.getBlockState(west.north()).getBlock() || blockIn == worldIn.getBlockState(east.north()).getBlock()
                    || blockIn == worldIn.getBlockState(east.south()).getBlock() || blockIn == worldIn.getBlockState(west.south()).getBlock();
            if (flag2) f /= 2.0F;
        }
        return f;
    }

    @Override
    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state) {
        IBlockState soil = worldIn.getBlockState(pos.down());
        return (worldIn.getLight(pos) >= 8 || worldIn.canSeeSky(pos)) && soil.getBlock().canSustainPlant(soil, worldIn, pos.down(), EnumFacing.UP, this);
    }

    protected Item getSeed() {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.WHEAT_SEEDS;
    }

    protected Item getCrop() {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.WHEAT;
    }

    /** Forge: a ripe crop drops extra seeds (3 + fortune tries). */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        super.getDrops(drops, world, pos, state, fortune);
        int age = getAge(state);
        Random rand = world instanceof World w ? w.rand : new Random();
        if (age >= getMaxAge()) {
            for (int i = 0; i < 3 + fortune; ++i) {
                if (rand.nextInt(2 * getMaxAge()) <= age) drops.add(new ItemStack(getSeed(), 1, 0));
            }
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return isMaxAge(state) ? getCrop() : getSeed();
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(getSeed());
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return !isMaxAge(state);
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        grow(worldIn, pos, state);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return withAge(meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return getAge(state);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
        return EnumPlantType.Crop;
    }
}

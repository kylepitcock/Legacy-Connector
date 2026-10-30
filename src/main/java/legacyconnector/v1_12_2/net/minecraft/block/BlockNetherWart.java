package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 nether wart: ages 0-3 (metadata) on soul sand; ripe it drops 2-4 (more with fortune). */
public class BlockNetherWart extends BlockBush {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 3);

    protected BlockNetherWart() {
        super(Material.PLANTS, MapColor.RED);
        setDefaultState(blockState.getBaseState().withProperty(AGE, 0));
        setTickRandomly(true);
        setCreativeTab(null);
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return Block.mcc$named(state.getBlock(), "soul_sand");
    }

    @Override
    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state) {
        return canSustainBush(worldIn.getBlockState(pos.down()));
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        int age = state.getValue(AGE);
        if (age < 3 && rand.nextInt(10) == 0) worldIn.setBlockState(pos, state.withProperty(AGE, age + 1), 2);
        super.updateTick(worldIn, pos, state, rand);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        int count = 1;
        if (state.getValue(AGE) >= 3) {
            Random rand = world instanceof World w ? w.rand : RANDOM;
            count = 2 + rand.nextInt(3);
            if (fortune > 0) count += rand.nextInt(fortune + 1);
        }
        for (int i = 0; i < count; ++i) drops.add(new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.NETHER_WART));
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.NETHER_WART);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, meta & 3);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }
}

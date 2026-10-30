package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 cake: seven slices, eaten one bite (metadata) at a time by a hungry player. */
public class BlockCake extends Block {
    public static final PropertyInteger BITES = PropertyInteger.create("bites", 0, 6);
    protected static final AxisAlignedBB[] CAKE_AABB = new AxisAlignedBB[7];

    static {
        for (int i = 0; i < 7; i++) CAKE_AABB[i] = new AxisAlignedBB((1 + i * 2) / 16.0D, 0.0D, 0.0625D, 0.9375D, 0.5D, 0.9375D);
    }

    protected BlockCake() {
        super(Material.CAKE);
        setDefaultState(blockState.getBaseState().withProperty(BITES, 0));
        setTickRandomly(true);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return CAKE_AABB[state.getValue(BITES)];
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
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (!worldIn.isRemote) return eatCake(worldIn, pos, state, playerIn);
        return true;
    }

    private boolean eatCake(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (!player.canEat(false)) return false;
        player.getFoodStats().addStats(2, 0.1F);
        int bites = state.getValue(BITES);
        if (bites < 6) worldIn.setBlockState(pos, state.withProperty(BITES, bites + 1), 3);
        else worldIn.setBlockToAir(pos);
        return true;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return super.canPlaceBlockAt(worldIn, pos) && canBlockStay(worldIn, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!canBlockStay(worldIn, pos)) worldIn.setBlockToAir(pos);
    }

    private boolean canBlockStay(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos.down()).getMaterial().isSolid();
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
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.CAKE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(BITES, Math.min(6, meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BITES);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BITES);
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return (7 - blockState.getValue(BITES)) * 2;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }
}

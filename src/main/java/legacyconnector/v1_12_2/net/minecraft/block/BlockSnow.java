package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.EnumSkyBlock;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 snow layers: 1-8 layers (metadata + 1), melt in block light above 11, drop snowballs. */
public class BlockSnow extends Block {
    public static final PropertyInteger LAYERS = PropertyInteger.create("layers", 1, 8);
    protected static final AxisAlignedBB[] SNOW_AABB = new AxisAlignedBB[9];

    static {
        for (int i = 0; i < 9; i++) SNOW_AABB[i] = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, i * 0.125D, 1.0D);
    }

    protected BlockSnow() {
        super(Material.SNOW);
        setDefaultState(blockState.getBaseState().withProperty(LAYERS, 1));
        setTickRandomly(true);
        setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SNOW_AABB[state.getValue(LAYERS)];
    }

    @Override
    public boolean isTopSolid(IBlockState state) {
        return state.getValue(LAYERS) == 8;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        int i = blockState.getValue(LAYERS) - 1;
        AxisAlignedBB box = blockState.getBoundingBox(worldIn, pos);
        return new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX, i * 0.125F, box.maxZ);
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).getValue(LAYERS) < 5;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** Forge: light passes a single layer. */
    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(LAYERS) == 1 ? 0 : super.getLightOpacity(state, world, pos);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.SNOWBALL;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    /** Melts in strong block light. */
    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.getLightFor(EnumSkyBlock.BLOCK, pos) > 11) {
            dropBlockAsItem(worldIn, pos, worldIn.getBlockState(pos), 0);
            worldIn.setBlockToAir(pos);
        }
    }

    @Override
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random) {
        updateTick(worldIn, pos, state, random);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LAYERS, (meta & 7) + 1);
    }

    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).getValue(LAYERS) == 1;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LAYERS) - 1;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LAYERS);
    }
}

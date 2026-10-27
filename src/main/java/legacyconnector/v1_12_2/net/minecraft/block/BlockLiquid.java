package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 water and lava blocks (vanilla's BlockStaticLiquid/BlockDynamicLiquid extend it). The flowing itself is the modern fluid's; this
 * keeps the level property and the height maths mods use. A vanilla liquid's level is read from the modern fluid in the world.
 */
public abstract class BlockLiquid extends Block {
    public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 15);

    protected BlockLiquid(Material materialIn) {
        super(materialIn);
        setDefaultState(blockState.getBaseState().withProperty(LEVEL, 0));
        setTickRandomly(true);
    }

    /** How much of the block a level leaves empty (0 is a source: 1/9). */
    public static float getLiquidHeightPercent(int meta) {
        if (meta >= 8) meta = 0;
        return (float) (meta + 1) / 9.0F;
    }

    /** The liquid's surface height in the block (full under more of the same liquid). */
    public static float getBlockLiquidHeight(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        int i = mcc$level(state, worldIn, pos);
        return (i & 7) == 0 && worldIn.getBlockState(pos.up()).getMaterial() == state.getMaterial() ? 1.0F : 1.0F - getLiquidHeightPercent(i);
    }

    /** The level of the liquid at a position: the state's LEVEL, else the modern fluid's (8 - amount, 8 and up when falling). */
    private static int mcc$level(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        boolean vanilla = state.getBlock().mcc$modern instanceof net.minecraft.world.level.block.Block b
                && !(b instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block);
        if (!vanilla && state.getPropertyKeys().contains(LEVEL)) return state.getValue(LEVEL);
        if (worldIn instanceof World w && w.mcc$level() != null) {
            var fluid = w.mcc$level().getFluidState(org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos));
            if (fluid.isEmpty() || fluid.isSource()) return 0;
            return (fluid.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING) ? 8 : 0) | Math.max(0, 8 - fluid.getAmount()) & 7;
        }
        return 0;
    }

    public static float getLiquidHeight(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return (float) pos.getY() + getBlockLiquidHeight(state, worldIn, pos);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return getMaterialLegacy() != Material.LAVA;
    }

    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LEVEL, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEVEL);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LEVEL);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}

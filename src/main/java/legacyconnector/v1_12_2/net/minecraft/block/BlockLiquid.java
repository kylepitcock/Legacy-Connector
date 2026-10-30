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

    @Override
    public int tickRate(World worldIn) {
        if (material == Material.WATER) return 5;
        if (material == Material.LAVA) return worldIn != null && World.mcc$get$provider(worldIn) != null && World.mcc$get$provider(worldIn).isNether() ? 10 : 30;
        return 0;
    }

    /** The level of the same liquid at a position, -1 for anything else. */
    protected int getDepth(IBlockState state) {
        return state.getMaterial() == material ? state.getValue(LEVEL) : -1;
    }

    /** Whether the flow is pushed down at this side (a solid face that is not ice, or the same liquid flowing). */
    public boolean causesDownwardCurrent(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        IBlockState state = worldIn.getBlockState(pos);
        Material m = state.getMaterial();
        if (m == material) return false;
        if (side == EnumFacing.UP) return true;
        if (m == Material.ICE) return false;
        return state.getBlockFaceShape(worldIn, pos, side) == BlockFaceShape.SOLID;
    }

    /** Whether the side of the liquid is visible: no more of it above and the block there is not opaque. */
    public boolean shouldRenderSides(IBlockAccess blockAccess, BlockPos pos) {
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                IBlockState state = blockAccess.getBlockState(pos.add(i, 0, j));
                if (state.getMaterial() != material && !state.isFullBlock()) return true;
            }
        }
        return false;
    }

    /** The direction of the flow at a position (the modern fluid's flow vector there). */
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d mcc$flow(IBlockAccess worldIn, BlockPos pos) {
        if (worldIn instanceof World w && w.mcc$level() != null) {
            var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos);
            var v = w.mcc$level().getFluidState(mp).getFlow(w.mcc$level(), mp);
            return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(v.x, v.y, v.z);
        }
        return legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d.ZERO;
    }

    /** The angle of the flow (for boats and the flowing texture), -1000 where the liquid stands still. */
    public static float getSlopeAngle(IBlockAccess worldIn, BlockPos pos, Material materialIn, IBlockState state) {
        legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d v = state.getBlock() instanceof BlockLiquid l ? l.mcc$flow(worldIn, pos)
                : legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d.ZERO;
        return v.x == 0.0D && v.z == 0.0D ? -1000.0F : (float) Math.atan2(v.z, v.x) - (float) (Math.PI / 2D);
    }

    /** Lava touching water: a source turns to obsidian, flowing lava to cobblestone (true when that happened). */
    public boolean checkForMixing(World worldIn, BlockPos pos, IBlockState state) {
        if (material != Material.LAVA) return false;
        boolean water = false;
        for (EnumFacing f : EnumFacing.values()) {
            if (f != EnumFacing.DOWN && worldIn.getBlockState(pos.offset(f)).getMaterial() == Material.WATER) {
                water = true;
                break;
            }
        }
        if (!water) return false;
        int level = state.getValue(LEVEL);
        if (level == 0) {
            worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.OBSIDIAN.getDefaultState());
            triggerMixEffects(worldIn, pos);
            return true;
        }
        if (level <= 4) {
            worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.COBBLESTONE.getDefaultState());
            triggerMixEffects(worldIn, pos);
            return true;
        }
        return false;
    }

    /** The fizz and smoke of lava meeting water. */
    protected void triggerMixEffects(World worldIn, BlockPos pos) {
        var level = worldIn.mcc$level();
        if (level != null) level.levelEvent(1501, org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos), 0);
    }

    /** The flowing block of a liquid material (vanilla water or lava). */
    public static BlockDynamicLiquid getFlowingBlock(Material materialIn) {
        if (materialIn == Material.WATER) return legacyconnector.v1_12_2.net.minecraft.init.Blocks.FLOWING_WATER;
        if (materialIn == Material.LAVA) return legacyconnector.v1_12_2.net.minecraft.init.Blocks.FLOWING_LAVA;
        throw new IllegalArgumentException("Invalid material");
    }

    /** The still block of a liquid material (vanilla water or lava). */
    public static BlockStaticLiquid getStaticBlock(Material materialIn) {
        if (materialIn == Material.WATER) return legacyconnector.v1_12_2.net.minecraft.init.Blocks.WATER;
        if (materialIn == Material.LAVA) return legacyconnector.v1_12_2.net.minecraft.init.Blocks.LAVA;
        throw new IllegalArgumentException("Invalid material");
    }
}

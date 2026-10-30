package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 fire. Burning itself is the modern fire's; the flammability table mods fill through {@link #setFireInfo} is the modern fire's
 * table, so their blocks burn in game. Its state keeps the age (metadata) and the faces it burns on.
 */
public class BlockFire extends Block {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 15);
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool UPPER = PropertyBool.create("up");
    private final java.util.Map<Block, Integer> encouragements = new java.util.IdentityHashMap<>();
    private final java.util.Map<Block, Integer> flammabilities = new java.util.IdentityHashMap<>();

    protected BlockFire() {
        super(Material.FIRE);
        setDefaultState(blockState.getBaseState().withProperty(AGE, 0).withProperty(NORTH, false).withProperty(EAST, false)
                .withProperty(SOUTH, false).withProperty(WEST, false).withProperty(UPPER, false));
        setTickRandomly(true);
    }

    /** Vanilla's fire table (the modern fire already has it). */
    public static void init() {
    }

    /** How eagerly fire spreads to the block and how fast it burns away; the modern fire learns it too. */
    public void setFireInfo(Block blockIn, int encouragement, int flammability) {
        if (blockIn == legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR) throw new IllegalArgumentException("Tried to set air on fire... This is bad.");
        encouragements.put(blockIn, encouragement);
        flammabilities.put(blockIn, flammability);
        Object modern = blockIn.mcc$modern;
        if (modern instanceof net.minecraft.world.level.block.Block m) {
            ((net.minecraft.world.level.block.FireBlock) net.minecraft.world.level.block.Blocks.FIRE).setFlammable(m, encouragement, flammability);
        }
    }

    /** The fire spread chance recorded for a block (0 when none). */
    public int mcc$encouragement(Block block) {
        Integer i = encouragements.get(block);
        return i == null ? 0 : i;
    }

    /** The burn chance recorded for a block (0 when none). */
    public int mcc$flammability(Block block) {
        Integer i = flammabilities.get(block);
        return i == null ? 0 : i;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        if (!worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP) && !canCatchFire(worldIn, pos.down(), EnumFacing.UP)) {
            return state.withProperty(NORTH, canCatchFire(worldIn, pos.north(), EnumFacing.SOUTH))
                    .withProperty(EAST, canCatchFire(worldIn, pos.east(), EnumFacing.WEST))
                    .withProperty(SOUTH, canCatchFire(worldIn, pos.south(), EnumFacing.NORTH))
                    .withProperty(WEST, canCatchFire(worldIn, pos.west(), EnumFacing.EAST))
                    .withProperty(UPPER, canCatchFire(worldIn, pos.up(), EnumFacing.DOWN));
        }
        return getDefaultState();
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
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    @Override
    public int tickRate(World worldIn) {
        return 30;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    /** Forge: whether the block at a position burns from that side. */
    public boolean canCatchFire(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return world.getBlockState(pos).getBlock().isFlammable(world, pos, face);
    }

    /** Fire stays on a solid top face or next to something that burns. */
    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos.down()).isTopSolid() || canNeighborCatchFire(worldIn, pos);
    }

    private boolean canNeighborCatchFire(World worldIn, BlockPos pos) {
        for (EnumFacing f : EnumFacing.values()) if (canCatchFire(worldIn, pos.offset(f), f.getOpposite())) return true;
        return false;
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!canPlaceBlockAt(worldIn, pos)) worldIn.setBlockToAir(pos);
        else worldIn.scheduleUpdate(pos, this, tickRate(worldIn) + worldIn.rand.nextInt(10));
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return MapColor.TNT;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE, NORTH, EAST, SOUTH, WEST, UPPER);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}

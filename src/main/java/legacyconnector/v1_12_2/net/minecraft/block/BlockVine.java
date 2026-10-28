package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 vines: hang on the solid faces around them (and under a solid block above), fall off when unsupported, climbable, dropped only
 * by shears. Spreading is not reproduced (mods' vines spread through their own updateTick).
 */
public class BlockVine extends Block {
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool[] ALL_FACES = {UP, NORTH, SOUTH, WEST, EAST};
    protected static final AxisAlignedBB UP_AABB = new AxisAlignedBB(0.0D, 0.9375D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB WEST_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0625D, 1.0D, 1.0D);
    protected static final AxisAlignedBB EAST_AABB = new AxisAlignedBB(0.9375D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB NORTH_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.0625D);
    protected static final AxisAlignedBB SOUTH_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.9375D, 1.0D, 1.0D, 1.0D);

    public BlockVine() {
        super(Material.VINE);
        setDefaultState(blockState.getBaseState().withProperty(UP, false).withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false)
                .withProperty(WEST, false));
        setTickRandomly(true);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        state = state.getActualState(source, pos);
        int i = 0;
        AxisAlignedBB bb = FULL_BLOCK_AABB;
        if (state.getValue(UP)) {
            bb = UP_AABB;
            ++i;
        }
        if (state.getValue(NORTH)) {
            bb = NORTH_AABB;
            ++i;
        }
        if (state.getValue(EAST)) {
            bb = EAST_AABB;
            ++i;
        }
        if (state.getValue(SOUTH)) {
            bb = SOUTH_AABB;
            ++i;
        }
        if (state.getValue(WEST)) {
            bb = WEST_AABB;
            ++i;
        }
        return i == 1 ? bb : FULL_BLOCK_AABB;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        BlockPos up = pos.up();
        return state.withProperty(UP, worldIn.getBlockState(up).getBlockFaceShape(worldIn, up, EnumFacing.DOWN) == BlockFaceShape.SOLID);
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
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side) {
        return side != EnumFacing.DOWN && side != EnumFacing.UP && canAttachTo(worldIn, pos, side);
    }

    public boolean canAttachTo(World world, BlockPos pos, EnumFacing side) {
        Block block = world.getBlockState(pos.up()).getBlock();
        return isAcceptableNeighbor(world, pos.offset(side.getOpposite()), side)
                && (block == legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR || block instanceof BlockVine || Block.mcc$named(block, "vine")
                        || isAcceptableNeighbor(world, pos.up(), EnumFacing.DOWN));
    }

    private boolean isAcceptableNeighbor(World world, BlockPos pos, EnumFacing side) {
        IBlockState iblockstate = world.getBlockState(pos);
        return iblockstate.getBlockFaceShape(world, pos, side) == BlockFaceShape.SOLID && !isExceptBlockForAttaching(iblockstate.getBlock());
    }

    protected static boolean isExceptBlockForAttaching(Block block) {
        var name = block == null ? null : block.getRegistryName();
        return name != null && "minecraft".equals(name.getNamespace()) && name.getPath().endsWith("shulker_box")
                || Block.mcc$named(block, "beacon", "cauldron", "glass", "stained_glass", "piston", "sticky_piston", "piston_head", "trapdoor");
    }

    /** Drops the sides that lost their support; false when no side is left. */
    private boolean recheckGrownSides(World worldIn, BlockPos pos, IBlockState state) {
        IBlockState original = state;
        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) {
            PropertyBool propertybool = getPropertyFor(enumfacing);
            if (state.getValue(propertybool) && !canAttachTo(worldIn, pos, enumfacing.getOpposite())) {
                IBlockState up = worldIn.getBlockState(pos.up());
                if (up.getBlock() != this || !up.getValue(propertybool)) state = state.withProperty(propertybool, false);
            }
        }
        if (getNumGrownFaces(state) == 0) return false;
        if (original != state) worldIn.setBlockState(pos, state, 2);
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!worldIn.isRemote && !recheckGrownSides(worldIn, pos, state)) {
            dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        }
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        IBlockState iblockstate = getDefaultState().withProperty(UP, false).withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false)
                .withProperty(WEST, false);
        return facing.getAxis().isHorizontal() ? iblockstate.withProperty(getPropertyFor(facing.getOpposite()), true) : iblockstate;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.AIR;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(SOUTH, (meta & 1) > 0).withProperty(WEST, (meta & 2) > 0).withProperty(NORTH, (meta & 4) > 0)
                .withProperty(EAST, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        if (state.getValue(SOUTH)) i |= 1;
        if (state.getValue(WEST)) i |= 2;
        if (state.getValue(NORTH)) i |= 4;
        if (state.getValue(EAST)) i |= 8;
        return i;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, UP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return switch (rot) {
            case CLOCKWISE_180 -> state.withProperty(NORTH, state.getValue(SOUTH)).withProperty(EAST, state.getValue(WEST)).withProperty(SOUTH, state.getValue(NORTH))
                    .withProperty(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state.withProperty(NORTH, state.getValue(EAST)).withProperty(EAST, state.getValue(SOUTH))
                    .withProperty(SOUTH, state.getValue(WEST)).withProperty(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state.withProperty(NORTH, state.getValue(WEST)).withProperty(EAST, state.getValue(NORTH)).withProperty(SOUTH, state.getValue(EAST))
                    .withProperty(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return switch (mirrorIn) {
            case LEFT_RIGHT -> state.withProperty(NORTH, state.getValue(SOUTH)).withProperty(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state.withProperty(EAST, state.getValue(WEST)).withProperty(WEST, state.getValue(EAST));
            default -> state;
        };
    }

    public static PropertyBool getPropertyFor(EnumFacing side) {
        return switch (side) {
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> throw new IllegalArgumentException(side + " is an invalid choice");
        };
    }

    public static int getNumGrownFaces(IBlockState state) {
        int i = 0;
        for (PropertyBool p : ALL_FACES) if (state.getValue(p)) ++i;
        return i;
    }

    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return true;
    }

    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return true;
    }

    public java.util.List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return java.util.Arrays.asList(new ItemStack(this, 1));
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}

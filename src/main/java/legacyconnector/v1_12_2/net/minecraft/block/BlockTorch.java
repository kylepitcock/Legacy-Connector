package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A torch: standing on a block or attached to the side of one (FACING away from it); it pops off when that block goes. */
public class BlockTorch extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", f -> f != EnumFacing.DOWN);
    protected static final AxisAlignedBB STANDING_AABB = new AxisAlignedBB(0.4D, 0.0D, 0.4D, 0.6D, 0.6D, 0.6D);
    protected static final AxisAlignedBB TORCH_NORTH_AABB = new AxisAlignedBB(0.35D, 0.2D, 0.7D, 0.65D, 0.8D, 1.0D);
    protected static final AxisAlignedBB TORCH_SOUTH_AABB = new AxisAlignedBB(0.35D, 0.2D, 0.0D, 0.65D, 0.8D, 0.3D);
    protected static final AxisAlignedBB TORCH_WEST_AABB = new AxisAlignedBB(0.7D, 0.2D, 0.35D, 1.0D, 0.8D, 0.65D);
    protected static final AxisAlignedBB TORCH_EAST_AABB = new AxisAlignedBB(0.0D, 0.2D, 0.35D, 0.3D, 0.8D, 0.65D);

    protected BlockTorch() {
        super(Material.CIRCUITS);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
        setTickRandomly(true);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public AxisAlignedBB mcc$staticBounds(IBlockState state) {
        return switch (state.getValue(FACING)) {
            case EAST -> TORCH_EAST_AABB;
            case WEST -> TORCH_WEST_AABB;
            case SOUTH -> TORCH_SOUTH_AABB;
            case NORTH -> TORCH_NORTH_AABB;
            default -> STANDING_AABB;
        };
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** Whether the block on that side of the position holds a torch (its facing side is sturdy; fences and walls hold standing ones). */
    private static boolean canPlaceOn(World world, BlockPos pos, EnumFacing facing) {
        var level = world.mcc$level();
        var support = pos.offset(facing.getOpposite()).mcc$modern();
        var state = level.getBlockState(support);
        if (facing == EnumFacing.UP) return net.minecraft.world.level.block.Block.canSupportCenter(level, support, net.minecraft.core.Direction.UP);
        return state.isFaceSturdy(level, support, facing.mcc$modern());
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        for (EnumFacing f : FACING.getAllowedValues()) if (canPlaceOn(world, pos, f)) return true;
        return false;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        if (canPlaceOn(world, pos, facing)) return getDefaultState().withProperty(FACING, facing);
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            if (canPlaceOn(world, pos, f)) return getDefaultState().withProperty(FACING, f);
        }
        return getDefaultState();
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        checkForDrop(world, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        onNeighborChangeInternal(world, pos, state);
    }

    protected boolean onNeighborChangeInternal(World world, BlockPos pos, IBlockState state) {
        if (!checkForDrop(world, pos, state)) return true;
        if (!canPlaceOn(world, pos, state.getValue(FACING))) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
            return true;
        }
        return false;
    }

    protected boolean checkForDrop(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() == this && canPlaceOn(world, pos, state.getValue(FACING))) return true;
        if (world.getBlockState(pos).getBlock() == this) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
        return false;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState state = getDefaultState();
        return switch (meta) {
            case 1 -> state.withProperty(FACING, EnumFacing.EAST);
            case 2 -> state.withProperty(FACING, EnumFacing.WEST);
            case 3 -> state.withProperty(FACING, EnumFacing.SOUTH);
            case 4 -> state.withProperty(FACING, EnumFacing.NORTH);
            default -> state.withProperty(FACING, EnumFacing.UP);
        };
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return switch (state.getValue(FACING)) {
            case EAST -> 1;
            case WEST -> 2;
            case SOUTH -> 3;
            case NORTH -> 4;
            default -> 5;
        };
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }
}

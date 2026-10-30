package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockPistonStructureHelper;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 pistons: facing bits 0-2, extended bit 3. Vanilla pistons are the modern ones; for mods' pistons built on this class a redstone
 * signal pushes the blocks the {@link BlockPistonStructureHelper} finds one block along at once (no animation) and the sticky one
 * pulls one back.
 */
public class BlockPistonBase extends BlockDirectional {
    public static final PropertyBool EXTENDED = PropertyBool.create("extended");
    protected static final AxisAlignedBB PISTON_BASE_EAST_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.75D, 1.0D, 1.0D);
    protected static final AxisAlignedBB PISTON_BASE_WEST_AABB = new AxisAlignedBB(0.25D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB PISTON_BASE_SOUTH_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.75D);
    protected static final AxisAlignedBB PISTON_BASE_NORTH_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.25D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB PISTON_BASE_UP_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D);
    protected static final AxisAlignedBB PISTON_BASE_DOWN_AABB = new AxisAlignedBB(0.0D, 0.25D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected final boolean isSticky;

    public BlockPistonBase(boolean isSticky) {
        super(Material.PISTON);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(EXTENDED, false));
        setSoundType(SoundType.STONE);
        setHardness(0.5F);
        setCreativeTab(CreativeTabs.REDSTONE);
        this.isSticky = isSticky;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        if (!state.getValue(EXTENDED)) return FULL_BLOCK_AABB;
        return switch (state.getValue(FACING)) {
            case DOWN -> PISTON_BASE_DOWN_AABB;
            case UP -> PISTON_BASE_UP_AABB;
            case NORTH -> PISTON_BASE_NORTH_AABB;
            case SOUTH -> PISTON_BASE_SOUTH_AABB;
            case WEST -> PISTON_BASE_WEST_AABB;
            default -> PISTON_BASE_EAST_AABB;
        };
    }

    @Override
    public boolean isTopSolid(IBlockState state) {
        return !state.getValue(EXTENDED) || state.getValue(FACING) == EnumFacing.DOWN;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos, state.withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer)), 2);
        if (!worldIn.isRemote) checkForMove(worldIn, pos, worldIn.getBlockState(pos));
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!worldIn.isRemote) checkForMove(worldIn, pos, state);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote && worldIn.getTileEntity(pos) == null) checkForMove(worldIn, pos, state);
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer)).withProperty(EXTENDED, false);
    }

    private void checkForMove(World worldIn, BlockPos pos, IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        boolean powered = shouldBeExtended(worldIn, pos, facing);
        if (powered && !state.getValue(EXTENDED)) {
            if (new BlockPistonStructureHelper(worldIn, pos, facing, true).canMove() && doMove(worldIn, pos, facing, true)) {
                worldIn.setBlockState(pos, state.withProperty(EXTENDED, true), 2);
                worldIn.setBlockState(pos.offset(facing), legacyconnector.v1_12_2.net.minecraft.init.Blocks.PISTON_HEAD.getDefaultState()
                        .withProperty(BlockPistonExtension.FACING, facing)
                        .withProperty(BlockPistonExtension.TYPE, isSticky ? BlockPistonExtension.EnumPistonType.STICKY : BlockPistonExtension.EnumPistonType.DEFAULT), 3);
                worldIn.playEvent(1048, pos, 0);
            }
        } else if (!powered && state.getValue(EXTENDED)) {
            worldIn.setBlockToAir(pos.offset(facing));
            if (isSticky) {
                BlockPos far = pos.offset(facing, 2);
                IBlockState pulled = worldIn.getBlockState(far);
                if (!pulled.getBlock().isAir(pulled, worldIn, far) && canPush(pulled, worldIn, far, facing.getOpposite(), false, facing)
                        && (pulled.getPushReaction() == EnumPushReaction.NORMAL || pulled.getBlock() instanceof BlockPistonBase)) {
                    doMove(worldIn, pos, facing, false);
                }
            }
            worldIn.setBlockState(pos, state.withProperty(EXTENDED, false), 2);
            worldIn.playEvent(1049, pos, 0);
        }
    }

    /** Powered from any side but the front (or from above the piston, quasi-connectivity). */
    private boolean shouldBeExtended(World worldIn, BlockPos pos, EnumFacing facing) {
        for (EnumFacing f : EnumFacing.values()) {
            if (f != facing && worldIn.isSidePowered(pos.offset(f), f)) return true;
        }
        if (worldIn.isSidePowered(pos, EnumFacing.DOWN)) return true;
        BlockPos up = pos.up();
        for (EnumFacing f : EnumFacing.values()) {
            if (f != EnumFacing.DOWN && worldIn.isSidePowered(up.offset(f), f)) return true;
        }
        return false;
    }

    /** Whether a piston can move the block there in a direction (breaking it when allowed and it breaks on push). */
    public static boolean canPush(IBlockState blockStateIn, World worldIn, BlockPos pos, EnumFacing facing, boolean destroyBlocks, EnumFacing p_185646_5_) {
        Block block = blockStateIn.getBlock();
        if (Block.mcc$named(block, "obsidian")) return false;
        if (pos.getY() < 0 || facing == EnumFacing.DOWN && pos.getY() == 0) return false;
        int top = worldIn.getHeight() - 1;
        if (pos.getY() > top || facing == EnumFacing.UP && pos.getY() == top) return false;
        if (!(block instanceof BlockPistonBase) && !Block.mcc$named(block, "piston", "sticky_piston")) {
            if (blockStateIn.getBlockHardness(worldIn, pos) == -1.0F) return false;
            switch (blockStateIn.getPushReaction()) {
                case BLOCK:
                    return false;
                case DESTROY:
                    return destroyBlocks;
                case PUSH_ONLY:
                    return facing == p_185646_5_;
                default:
                    break;
            }
        } else if (blockStateIn.getPropertyKeys().contains(EXTENDED) && blockStateIn.getValue(EXTENDED)) {
            return false;
        }
        return !block.hasTileEntity(blockStateIn);
    }

    /** Moves (extending) or pulls (retracting) the blocks in front one block; false when they cannot move. */
    protected boolean doMove(World worldIn, BlockPos pos, EnumFacing direction, boolean extending) {
        if (!extending) worldIn.setBlockToAir(pos.offset(direction));
        BlockPistonStructureHelper helper = new BlockPistonStructureHelper(worldIn, pos, direction, extending);
        if (!helper.canMove()) return false;
        List<BlockPos> move = helper.getBlocksToMove();
        List<IBlockState> states = new ArrayList<>();
        for (BlockPos p : move) states.add(worldIn.getBlockState(p).getActualState(worldIn, p));
        for (BlockPos p : helper.getBlocksToDestroy()) {
            IBlockState s = worldIn.getBlockState(p);
            s.getBlock().dropBlockAsItem(worldIn, p, s, 0);
            worldIn.setBlockToAir(p);
        }
        EnumFacing step = extending ? direction : direction.getOpposite();
        for (int i = move.size() - 1; i >= 0; --i) worldIn.setBlockState(move.get(i), legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR.getDefaultState(), 2);
        for (int i = move.size() - 1; i >= 0; --i) worldIn.setBlockState(move.get(i).offset(step), states.get(i), 3);
        for (BlockPos p : move) worldIn.notifyNeighborsOfStateChange(p, states.get(move.indexOf(p)).getBlock(), false);
        return true;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, getFacing(meta)).withProperty(EXTENDED, (meta & 8) > 0);
    }

    public static EnumFacing getFacing(int meta) {
        int i = meta & 7;
        return i > 5 ? null : EnumFacing.byIndex(i);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(FACING).getIndex();
        if (state.getValue(EXTENDED)) i |= 8;
        return i;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, EXTENDED);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        state = getActualState(state, worldIn, pos);
        return state.getValue(FACING) != face.getOpposite() && state.getValue(EXTENDED) ? BlockFaceShape.UNDEFINED : BlockFaceShape.SOLID;
    }
}

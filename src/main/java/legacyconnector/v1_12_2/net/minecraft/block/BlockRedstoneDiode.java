package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2's base of repeaters and comparators: a slab-high block facing away from its input (the facing in bits 0-1), powered as a
 * separate block (1.12 swaps the powered and unpowered blocks). Mods' diodes get the input reading and the delayed output switch.
 */
public abstract class BlockRedstoneDiode extends BlockHorizontal {
    protected static final AxisAlignedBB REDSTONE_DIODE_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
    protected final boolean isRepeaterPowered;

    protected BlockRedstoneDiode(boolean powered) {
        super(Material.CIRCUITS);
        this.isRepeaterPowered = powered;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return REDSTONE_DIODE_AABB;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        IBlockState below = worldIn.getBlockState(pos.down());
        return (below.isTopSolid() || below.getBlockFaceShape(worldIn, pos.down(), EnumFacing.UP) == BlockFaceShape.SOLID) && super.canPlaceBlockAt(worldIn, pos);
    }

    public boolean canBlockStay(World worldIn, BlockPos pos) {
        IBlockState below = worldIn.getBlockState(pos.down());
        return below.isTopSolid() || below.getBlockFaceShape(worldIn, pos.down(), EnumFacing.UP) == BlockFaceShape.SOLID;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (isLocked(worldIn, pos, state)) return;
        boolean should = shouldBePowered(worldIn, pos, state);
        if (isRepeaterPowered && !should) worldIn.setBlockState(pos, getUnpoweredState(state), 2);
        else if (!isRepeaterPowered) {
            worldIn.setBlockState(pos, getPoweredState(state), 2);
            if (!should) worldIn.scheduleUpdate(pos, getPoweredState(state).getBlock(), getTickDelay(state));
        }
    }

    @Override
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return side.getAxis() != EnumFacing.Axis.Y;
    }

    protected boolean isPowered(IBlockState state) {
        return isRepeaterPowered;
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return blockState.getWeakPower(blockAccess, pos, side);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (!isPowered(blockState)) return 0;
        return blockState.getValue(FACING) == side ? getActiveSignal(blockAccess, pos, blockState) : 0;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (canBlockStay(worldIn, pos)) {
            updateState(worldIn, pos, state);
        } else {
            dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
            for (EnumFacing f : EnumFacing.values()) worldIn.notifyNeighborsOfStateChange(pos.offset(f), this, false);
        }
    }

    protected void updateState(World worldIn, BlockPos pos, IBlockState state) {
        if (isLocked(worldIn, pos, state)) return;
        boolean should = shouldBePowered(worldIn, pos, state);
        if (isRepeaterPowered != should && !worldIn.isBlockTickPending(pos, this)) {
            worldIn.scheduleUpdate(pos, this, getDelay(state));
        }
    }

    public boolean isLocked(IBlockAccess worldIn, BlockPos pos, IBlockState state) {
        return false;
    }

    protected boolean shouldBePowered(World worldIn, BlockPos pos, IBlockState state) {
        return calculateInputStrength(worldIn, pos, state) > 0;
    }

    /** The signal coming into the back. */
    protected int calculateInputStrength(World worldIn, BlockPos pos, IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        BlockPos back = pos.offset(facing);
        int i = worldIn.getRedstonePower(back, facing);
        if (i >= 15) return i;
        IBlockState backState = worldIn.getBlockState(back);
        return Math.max(i, backState.getBlock() instanceof BlockRedstoneWire || Block.mcc$named(backState.getBlock(), "redstone_wire")
                ? backState.getValue(BlockRedstoneWire.POWER) : 0);
    }

    /** The strongest signal from the two sides (comparators subtract it, repeaters lock on it). */
    protected int getPowerOnSides(IBlockAccess worldIn, BlockPos pos, IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        EnumFacing left = facing.rotateY();
        EnumFacing right = facing.rotateYCCW();
        return Math.max(getPowerOnSide(worldIn, pos.offset(left), left), getPowerOnSide(worldIn, pos.offset(right), right));
    }

    protected int getPowerOnSide(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        IBlockState state = worldIn.getBlockState(pos);
        Block block = state.getBlock();
        if (!isAlternateInput(state)) return 0;
        if (Block.mcc$named(block, "redstone_block")) return 15;
        return block instanceof BlockRedstoneWire ? state.getValue(BlockRedstoneWire.POWER) : worldIn.getStrongPower(pos, side);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (shouldBePowered(worldIn, pos, state)) worldIn.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        notifyNeighbors(worldIn, pos, state);
    }

    protected void notifyNeighbors(World worldIn, BlockPos pos, IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        BlockPos front = pos.offset(facing.getOpposite());
        worldIn.neighborChanged(front, this, pos);
        worldIn.notifyNeighborsOfStateExcept(front, this, facing);
    }

    @Override
    public void onPlayerDestroy(World worldIn, BlockPos pos, IBlockState state) {
        if (isRepeaterPowered) for (EnumFacing f : EnumFacing.values()) worldIn.notifyNeighborsOfStateChange(pos.offset(f), this, false);
        super.onPlayerDestroy(worldIn, pos, state);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    protected boolean isAlternateInput(IBlockState state) {
        return state.canProvidePower();
    }

    protected int getActiveSignal(IBlockAccess worldIn, BlockPos pos, IBlockState state) {
        return 15;
    }

    /** Whether the state is of a repeater or comparator. */
    public static boolean isDiode(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockRedstoneDiode || Block.mcc$named(block, "unpowered_repeater", "powered_repeater", "unpowered_comparator", "powered_comparator");
    }

    public boolean isSameDiode(IBlockState state) {
        Block block = state.getBlock();
        return block == getPoweredState(getDefaultState()).getBlock() || block == getUnpoweredState(getDefaultState()).getBlock();
    }

    public boolean isFacingTowardsRepeater(World worldIn, BlockPos pos, IBlockState state) {
        EnumFacing f = state.getValue(FACING).getOpposite();
        BlockPos front = pos.offset(f);
        return isDiode(worldIn.getBlockState(front)) && worldIn.getBlockState(front).getValue(FACING) != f;
    }

    protected int getTickDelay(IBlockState state) {
        return getDelay(state);
    }

    protected abstract int getDelay(IBlockState state);

    protected abstract IBlockState getPoweredState(IBlockState unpoweredState);

    protected abstract IBlockState getUnpoweredState(IBlockState poweredState);

    @Override
    public boolean isAssociatedBlock(Block other) {
        return isSameDiode(other.getDefaultState());
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }
}

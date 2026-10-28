package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 pressure plate base: entities on it give a redstone strength (checked each second while pressed), to the plate's neighbours and strongly downward. */
public abstract class BlockBasePressurePlate extends Block {
    protected static final AxisAlignedBB PRESSED_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.03125D, 0.9375D);
    protected static final AxisAlignedBB UNPRESSED_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.0625D, 0.9375D);
    protected static final AxisAlignedBB PRESSURE_AABB = new AxisAlignedBB(0.125D, 0.0D, 0.125D, 0.875D, 0.25D, 0.875D);

    protected BlockBasePressurePlate(Material materialIn) {
        this(materialIn, materialIn.getMaterialMapColor());
    }

    protected BlockBasePressurePlate(Material materialIn, MapColor mapColorIn) {
        super(materialIn, mapColorIn);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.REDSTONE);
        setTickRandomly(true);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return mcc$staticBounds(state);
    }

    @Override
    public AxisAlignedBB mcc$staticBounds(IBlockState state) {
        return getRedstoneStrength(state) > 0 ? PRESSED_AABB : UNPRESSED_AABB;
    }

    @Override
    public int tickRate(World worldIn) {
        return 20;
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
        return true;
    }

    public boolean canSpawnInBlock() {
        return true;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return canBePlacedOn(worldIn, pos.down());
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!canBePlacedOn(worldIn, pos.down())) {
            dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        }
    }

    private boolean canBePlacedOn(World worldIn, BlockPos pos) {
        IBlockState s = worldIn.getBlockState(pos);
        return s.isTopSolid() || s.getBlock() instanceof BlockFence || Block.mcc$named(s.getBlock(), "fence", "nether_brick_fence");
    }

    @Override
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random) {
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.isRemote) return;
        int i = getRedstoneStrength(state);
        if (i > 0) updateState(worldIn, pos, state, i);
    }

    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (worldIn.isRemote) return;
        int i = getRedstoneStrength(state);
        if (i == 0) updateState(worldIn, pos, state, i);
    }

    protected void updateState(World worldIn, BlockPos pos, IBlockState state, int oldRedstoneStrength) {
        int i = computeRedstoneStrength(worldIn, pos);
        boolean was = oldRedstoneStrength > 0;
        boolean now = i > 0;
        if (oldRedstoneStrength != i) {
            state = setRedstoneStrength(state, i);
            worldIn.setBlockState(pos, state, 2);
            updateNeighbors(worldIn, pos);
            worldIn.markBlockRangeForRenderUpdate(pos, pos);
        }
        if (!now && was) playClickOffSound(worldIn, pos);
        else if (now && !was) playClickOnSound(worldIn, pos);
        if (now) worldIn.scheduleUpdate(new BlockPos(pos), this, tickRate(worldIn));
    }

    protected abstract void playClickOnSound(World worldIn, BlockPos color);

    protected abstract void playClickOffSound(World worldIn, BlockPos pos);

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (getRedstoneStrength(state) > 0) updateNeighbors(worldIn, pos);
        super.breakBlock(worldIn, pos, state);
    }

    protected void updateNeighbors(World worldIn, BlockPos pos) {
        worldIn.notifyNeighborsOfStateChange(pos, this, false);
        worldIn.notifyNeighborsOfStateChange(pos.down(), this, false);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return getRedstoneStrength(blockState);
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.UP ? getRedstoneStrength(blockState) : 0;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    protected abstract int computeRedstoneStrength(World worldIn, BlockPos pos);

    protected abstract int getRedstoneStrength(IBlockState state);

    protected abstract IBlockState setRedstoneStrength(IBlockState state, int strength);

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}

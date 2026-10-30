package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 observer: pulses (POWERED, bit 3) out of its back for two ticks when the block it faces (bits 0-2) changes. */
public class BlockObserver extends BlockDirectional {
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockObserver() {
        super(Material.ROCK);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH).withProperty(POWERED, false));
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
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
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (state.getValue(POWERED)) {
            worldIn.setBlockState(pos, state.withProperty(POWERED, false), 2);
        } else {
            worldIn.setBlockState(pos, state.withProperty(POWERED, true), 2);
            worldIn.scheduleUpdate(pos, this, 2);
        }
        updateNeighborsInFront(worldIn, pos, state);
    }

    /** The block it watches changed: pulse in two ticks. */
    public void observedNeighborChange(IBlockState observerState, World world, BlockPos observerPos, Block changedBlock, BlockPos changedBlockPos) {
        if (!world.isRemote && observerPos.offset(observerState.getValue(FACING)).equals(changedBlockPos)) startSignal(observerState, world, observerPos);
    }

    private void startSignal(IBlockState state, World worldIn, BlockPos pos) {
        if (!state.getValue(POWERED)) worldIn.scheduleUpdate(pos, this, 2);
    }

    protected void updateNeighborsInFront(World worldIn, BlockPos pos, IBlockState state) {
        EnumFacing f = state.getValue(FACING);
        BlockPos back = pos.offset(f.getOpposite());
        worldIn.neighborChanged(back, this, pos);
        worldIn.notifyNeighborsOfStateExcept(back, this, f);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return blockState.getWeakPower(blockAccess, pos, side);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return blockState.getValue(POWERED) && blockState.getValue(FACING) == side ? 15 : 0;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer).getOpposite());
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(FACING).getIndex();
        if (state.getValue(POWERED)) i |= 8;
        return i;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7)).withProperty(POWERED, (meta & 8) != 0);
    }
}

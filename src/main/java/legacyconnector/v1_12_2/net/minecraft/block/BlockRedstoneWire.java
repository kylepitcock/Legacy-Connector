package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 redstone dust: its power 0-15 is the metadata, the sides it connects to are its actual state. The signal itself runs on the
 * modern dust (vanilla dust is the modern block).
 */
public class BlockRedstoneWire extends Block {
    public static final PropertyEnum<EnumAttachPosition> NORTH = PropertyEnum.create("north", EnumAttachPosition.class);
    public static final PropertyEnum<EnumAttachPosition> EAST = PropertyEnum.create("east", EnumAttachPosition.class);
    public static final PropertyEnum<EnumAttachPosition> SOUTH = PropertyEnum.create("south", EnumAttachPosition.class);
    public static final PropertyEnum<EnumAttachPosition> WEST = PropertyEnum.create("west", EnumAttachPosition.class);
    public static final PropertyInteger POWER = PropertyInteger.create("power", 0, 15);
    protected static final AxisAlignedBB REDSTONE_WIRE_AABB = new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 0.8125D, 0.0625D, 0.8125D);

    public BlockRedstoneWire() {
        super(Material.CIRCUITS);
        setDefaultState(blockState.getBaseState().withProperty(NORTH, EnumAttachPosition.NONE).withProperty(EAST, EnumAttachPosition.NONE)
                .withProperty(SOUTH, EnumAttachPosition.NONE).withProperty(WEST, EnumAttachPosition.NONE).withProperty(POWER, 0));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return REDSTONE_WIRE_AABB;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.withProperty(WEST, getAttachPosition(worldIn, pos, EnumFacing.WEST)).withProperty(EAST, getAttachPosition(worldIn, pos, EnumFacing.EAST))
                .withProperty(NORTH, getAttachPosition(worldIn, pos, EnumFacing.NORTH)).withProperty(SOUTH, getAttachPosition(worldIn, pos, EnumFacing.SOUTH));
    }

    private EnumAttachPosition getAttachPosition(IBlockAccess worldIn, BlockPos pos, EnumFacing direction) {
        BlockPos side = pos.offset(direction);
        IBlockState sideState = worldIn.getBlockState(side);
        if (!canConnectTo(sideState, direction, worldIn, side) && (sideState.isNormalCube() || !canConnectUpwardsTo(worldIn, side.down()))) {
            IBlockState above = worldIn.getBlockState(pos.up());
            if (!above.isNormalCube()) {
                boolean solid = worldIn.getBlockState(side).isSideSolid(worldIn, side, EnumFacing.UP) || Block.mcc$named(worldIn.getBlockState(side).getBlock(), "glowstone");
                if (solid && canConnectUpwardsTo(worldIn, side.up())) {
                    return sideState.isBlockNormalCube() ? EnumAttachPosition.UP : EnumAttachPosition.SIDE;
                }
            }
            return EnumAttachPosition.NONE;
        }
        return EnumAttachPosition.SIDE;
    }

    private static boolean canConnectUpwardsTo(IBlockAccess worldIn, BlockPos pos) {
        return canConnectTo(worldIn.getBlockState(pos), null, worldIn, pos);
    }

    /** Whether redstone dust connects to that block from a side (dust, diodes facing it, power sources). */
    protected static boolean canConnectTo(IBlockState blockState, EnumFacing side, IBlockAccess world, BlockPos pos) {
        Block block = blockState.getBlock();
        if (block instanceof BlockRedstoneWire || Block.mcc$named(block, "redstone_wire")) return true;
        if (BlockRedstoneDiode.isDiode(blockState)) {
            if (!blockState.getPropertyKeys().contains(BlockHorizontal.FACING)) return true;
            EnumFacing facing = blockState.getValue(BlockHorizontal.FACING);
            return facing == side || facing.getOpposite() == side;
        }
        if (Block.mcc$named(block, "observer")) return side == blockState.getValue(BlockDirectional.FACING);
        return blockState.getBlock().canConnectRedstone(blockState, world, pos, side);
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
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        IBlockState below = worldIn.getBlockState(pos.down());
        return below.isTopSolid() || below.getBlockFaceShape(worldIn, pos.down(), EnumFacing.UP) == BlockFaceShape.SOLID
                || Block.mcc$named(below.getBlock(), "glowstone");
    }

    /** Positions to notify once the power of a wire has been recalculated (1.12 collected them while it ran). */
    private final java.util.Set<BlockPos> blocksNeedingUpdate = new java.util.HashSet<>();
    /** False while the wire asks what powers it, so that it does not read its own signal back. */
    private boolean canProvidePower = true;

    private IBlockState updateSurroundingRedstone(World worldIn, BlockPos pos, IBlockState state) {
        state = calculateCurrentChanges(worldIn, pos, pos, state);
        java.util.List<BlockPos> list = new java.util.ArrayList<>(blocksNeedingUpdate);
        blocksNeedingUpdate.clear();
        for (BlockPos blockpos : list) worldIn.notifyNeighborsOfStateChange(blockpos, this, false);
        return state;
    }

    private IBlockState calculateCurrentChanges(World worldIn, BlockPos pos1, BlockPos pos2, IBlockState state) {
        IBlockState iblockstate = state;
        int i = state.getValue(POWER);
        canProvidePower = false;
        int j = worldIn.isBlockIndirectlyGettingPowered(pos1);
        canProvidePower = true;
        if (j > 0 && j > i - 1) i = j;
        int k = 0;
        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) {
            BlockPos blockpos = pos1.offset(enumfacing);
            boolean flag = blockpos.getX() != pos2.getX() || blockpos.getZ() != pos2.getZ();
            if (flag) k = getMaxCurrentStrength(worldIn, blockpos, k);
            if (worldIn.getBlockState(blockpos).isNormalCube() && !worldIn.getBlockState(pos1.up()).isNormalCube()) {
                if (flag && pos1.getY() >= pos2.getY()) k = getMaxCurrentStrength(worldIn, blockpos.up(), k);
            } else if (!worldIn.getBlockState(blockpos).isNormalCube() && flag && pos1.getY() <= pos2.getY()) {
                k = getMaxCurrentStrength(worldIn, blockpos.down(), k);
            }
        }
        if (k > i) i = k - 1;
        else if (i > 0) --i;
        else i = 0;
        if (j > i - 1) i = j;
        if (state.getValue(POWER) != i) {
            state = state.withProperty(POWER, i);
            if (worldIn.getBlockState(pos1) == iblockstate) worldIn.setBlockState(pos1, state, 2);
            blocksNeedingUpdate.add(pos1);
            for (EnumFacing enumfacing1 : EnumFacing.values()) blocksNeedingUpdate.add(pos1.offset(enumfacing1));
        }
        return state;
    }

    private void notifyWireNeighborsOfStateChange(World worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock() == this) {
            worldIn.notifyNeighborsOfStateChange(pos, this, false);
            for (EnumFacing enumfacing : EnumFacing.values()) worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
        }
    }

    private void notifyWireNeighbors(World worldIn, BlockPos pos) {
        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) notifyWireNeighborsOfStateChange(worldIn, pos.offset(enumfacing));
        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) {
            BlockPos blockpos = pos.offset(enumfacing);
            if (worldIn.getBlockState(blockpos).isNormalCube()) notifyWireNeighborsOfStateChange(worldIn, blockpos.up());
            else notifyWireNeighborsOfStateChange(worldIn, blockpos.down());
        }
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            updateSurroundingRedstone(worldIn, pos, state);
            for (EnumFacing enumfacing : EnumFacing.Plane.VERTICAL) worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
            notifyWireNeighbors(worldIn, pos);
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        if (!worldIn.isRemote) {
            for (EnumFacing enumfacing : EnumFacing.values()) worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
            updateSurroundingRedstone(worldIn, pos, state);
            notifyWireNeighbors(worldIn, pos);
        }
    }

    private int getMaxCurrentStrength(World worldIn, BlockPos pos, int strength) {
        if (worldIn.getBlockState(pos).getBlock() != this) return strength;
        int i = worldIn.getBlockState(pos).getValue(POWER);
        return Math.max(i, strength);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!worldIn.isRemote) {
            if (canPlaceBlockAt(worldIn, pos)) {
                updateSurroundingRedstone(worldIn, pos, state);
            } else {
                dropBlockAsItem(worldIn, pos, state, 0);
                worldIn.setBlockToAir(pos);
            }
        }
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return !canProvidePower ? 0 : blockState.getWeakPower(blockAccess, pos, side);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (!canProvidePower) return 0;
        int i = blockState.getValue(POWER);
        if (i == 0) return 0;
        if (side == EnumFacing.UP) return i;
        java.util.EnumSet<EnumFacing> set = java.util.EnumSet.noneOf(EnumFacing.class);
        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) if (isPowerSourceAt(blockAccess, pos, enumfacing)) set.add(enumfacing);
        if (side.getAxis().isHorizontal() && set.isEmpty()) return i;
        if (set.contains(side) && !set.contains(side.rotateYCCW()) && !set.contains(side.rotateY())) return i;
        return 0;
    }

    private boolean isPowerSourceAt(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        BlockPos blockpos = pos.offset(side);
        IBlockState iblockstate = worldIn.getBlockState(blockpos);
        boolean flag = iblockstate.isNormalCube();
        boolean flag1 = worldIn.getBlockState(pos.up()).isNormalCube();
        if (!flag1 && flag && canConnectUpwardsTo(worldIn, blockpos.up())) return true;
        if (canConnectTo(iblockstate, side, worldIn, pos)) return true;
        if (BlockRedstoneDiode.isDiode(iblockstate) && iblockstate.getPropertyKeys().contains(BlockHorizontal.FACING)
                && iblockstate.getValue(BlockHorizontal.FACING) == side && Block.mcc$named(iblockstate.getBlock(), "powered_repeater")) return true;
        return !flag && canConnectUpwardsTo(worldIn, blockpos.down());
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    /** The colour of dust at a power level (dark red to bright red). */
    public static int colorMultiplier(int p_176337_0_) {
        float f = p_176337_0_ / 15.0F;
        float r = f * 0.6F + 0.4F;
        if (p_176337_0_ == 0) r = 0.3F;
        float g = Math.max(0.0F, f * f * 0.7F - 0.5F);
        float b = Math.max(0.0F, f * f * 0.6F - 0.7F);
        int ri = Math.max(0, Math.min(255, (int) (r * 255.0F)));
        int gi = Math.max(0, Math.min(255, (int) (g * 255.0F)));
        int bi = Math.max(0, Math.min(255, (int) (b * 255.0F)));
        return -16777216 | ri << 16 | gi << 8 | bi;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.REDSTONE;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.REDSTONE);
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(POWER, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWER);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST, POWER);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    enum EnumAttachPosition implements IStringSerializable {
        UP("up"),
        SIDE("side"),
        NONE("none");

        private final String name;

        EnumAttachPosition(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}

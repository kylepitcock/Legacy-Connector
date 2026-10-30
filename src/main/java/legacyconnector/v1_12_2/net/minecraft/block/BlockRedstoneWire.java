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

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return blockState.getWeakPower(blockAccess, pos, side);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        int power = blockState.getValue(POWER);
        if (power == 0 || side == EnumFacing.DOWN) return 0;
        if (side == EnumFacing.UP) return power;
        IBlockState actual = getActualState(blockState, blockAccess, pos);
        EnumFacing toward = side.getOpposite();
        PropertyEnum<EnumAttachPosition> prop = toward == EnumFacing.NORTH ? NORTH : toward == EnumFacing.SOUTH ? SOUTH : toward == EnumFacing.EAST ? EAST : WEST;
        if (actual.getValue(prop) != EnumAttachPosition.NONE) return power;
        boolean anyOther = false;
        for (PropertyEnum<EnumAttachPosition> p : java.util.List.of(NORTH, SOUTH, EAST, WEST)) {
            if (p != prop && actual.getValue(p) != EnumAttachPosition.NONE) anyOther = true;
        }
        return anyOther ? 0 : power;
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

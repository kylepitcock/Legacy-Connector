package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 doors: two blocks; the lower half stores facing and open, the upper half hinge and powered (4 bits each), and each half's
 * actual state takes the rest from the other. Wooden doors open by hand, all doors by redstone.
 */
public class BlockDoor extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool OPEN = PropertyBool.create("open");
    public static final PropertyEnum<EnumHingePosition> HINGE = PropertyEnum.create("hinge", EnumHingePosition.class);
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyEnum<EnumDoorHalf> HALF = PropertyEnum.create("half", EnumDoorHalf.class);
    protected static final AxisAlignedBB SOUTH_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.1875D);
    protected static final AxisAlignedBB NORTH_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.8125D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB WEST_AABB = new AxisAlignedBB(0.8125D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB EAST_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.1875D, 1.0D, 1.0D);

    protected BlockDoor(Material materialIn) {
        super(materialIn);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(OPEN, false).withProperty(HINGE, EnumHingePosition.LEFT)
                .withProperty(POWERED, false).withProperty(HALF, EnumDoorHalf.LOWER));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        state = state.getActualState(source, pos);
        EnumFacing enumfacing = state.getValue(FACING);
        boolean closed = !state.getValue(OPEN);
        boolean right = state.getValue(HINGE) == EnumHingePosition.RIGHT;
        return switch (enumfacing) {
            case SOUTH -> closed ? SOUTH_AABB : (right ? EAST_AABB : WEST_AABB);
            case WEST -> closed ? WEST_AABB : (right ? SOUTH_AABB : NORTH_AABB);
            case NORTH -> closed ? NORTH_AABB : (right ? WEST_AABB : EAST_AABB);
            default -> closed ? EAST_AABB : (right ? NORTH_AABB : SOUTH_AABB);
        };
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return isOpen(combineMetadata(worldIn, pos));
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    private int getCloseSound() {
        return getMaterialLegacy() == Material.IRON ? 1011 : 1012;
    }

    private int getOpenSound() {
        return getMaterialLegacy() == Material.IRON ? 1005 : 1006;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (getMaterialLegacy() == Material.IRON) return false;
        BlockPos blockpos = state.getValue(HALF) == EnumDoorHalf.LOWER ? pos : pos.down();
        IBlockState iblockstate = pos.equals(blockpos) ? state : worldIn.getBlockState(blockpos);
        if (iblockstate.getBlock() != this) return false;
        state = iblockstate.cycleProperty(OPEN);
        worldIn.setBlockState(blockpos, state, 10);
        worldIn.markBlockRangeForRenderUpdate(blockpos, pos);
        worldIn.playEvent(playerIn, state.getValue(OPEN) ? getOpenSound() : getCloseSound(), pos, 0);
        return true;
    }

    public void toggleDoor(World worldIn, BlockPos pos, boolean open) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        if (iblockstate.getBlock() != this) return;
        BlockPos blockpos = iblockstate.getValue(HALF) == EnumDoorHalf.LOWER ? pos : pos.down();
        IBlockState iblockstate1 = pos == blockpos ? iblockstate : worldIn.getBlockState(blockpos);
        if (iblockstate1.getBlock() == this && iblockstate1.getValue(OPEN) != open) {
            worldIn.setBlockState(blockpos, iblockstate1.withProperty(OPEN, open), 10);
            worldIn.markBlockRangeForRenderUpdate(blockpos, pos);
            worldIn.playEvent(null, open ? getOpenSound() : getCloseSound(), pos, 0);
        }
    }

    /** Keeps the two halves together, on solid ground, and opens/closes with redstone. */
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (state.getValue(HALF) == EnumDoorHalf.UPPER) {
            BlockPos blockpos = pos.down();
            IBlockState iblockstate = worldIn.getBlockState(blockpos);
            if (iblockstate.getBlock() != this) worldIn.setBlockToAir(pos);
            else if (blockIn != this) iblockstate.neighborChanged(worldIn, blockpos, blockIn, fromPos);
            return;
        }
        boolean broken = false;
        BlockPos up = pos.up();
        IBlockState upState = worldIn.getBlockState(up);
        if (upState.getBlock() != this) {
            worldIn.setBlockToAir(pos);
            broken = true;
        }
        if (!worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP)) {
            worldIn.setBlockToAir(pos);
            broken = true;
            if (upState.getBlock() == this) worldIn.setBlockToAir(up);
        }
        if (broken) {
            if (!worldIn.isRemote) dropBlockAsItem(worldIn, pos, state, 0);
            return;
        }
        boolean powered = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(up);
        if (blockIn != this && (powered || blockIn.getDefaultState().canProvidePower()) && powered != upState.getValue(POWERED)) {
            worldIn.setBlockState(up, upState.withProperty(POWERED, powered), 2);
            if (powered != state.getValue(OPEN)) {
                worldIn.setBlockState(pos, state.withProperty(OPEN, powered), 2);
                worldIn.markBlockRangeForRenderUpdate(pos, pos);
                worldIn.playEvent(null, powered ? getOpenSound() : getCloseSound(), pos, 0);
            }
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return state.getValue(HALF) == EnumDoorHalf.UPPER ? legacyconnector.v1_12_2.net.minecraft.init.Items.AIR : super.getItemDropped(state, rand, fortune);
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        if (pos.getY() >= worldIn.getHeight() - 1) return false;
        IBlockState below = worldIn.getBlockState(pos.down());
        return below.isSideSolid(worldIn, pos.down(), EnumFacing.UP) && super.canPlaceBlockAt(worldIn, pos) && super.canPlaceBlockAt(worldIn, pos.up());
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    /** The combined state bits of both halves at a position (lower bits 0-2 and 4: facing, open; upper: hinge, powered). */
    public static int combineMetadata(IBlockAccess worldIn, BlockPos pos) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        int i = iblockstate.getBlock().getMetaFromState(iblockstate);
        boolean upper = isTop(i);
        IBlockState other = worldIn.getBlockState(upper ? pos.down() : pos.up());
        int j = other.getBlock().getMetaFromState(other);
        int k = upper ? j : i;
        int l = upper ? i : j;
        boolean hingeRight = (l & 1) != 0;
        boolean powered = (l & 2) != 0;
        return removeHalfBit(k) | (upper ? 8 : 0) | (hingeRight ? 16 : 0) | (powered ? 32 : 0);
    }

    protected static int removeHalfBit(int meta) {
        return meta & 7;
    }

    public static boolean isOpen(IBlockAccess worldIn, BlockPos pos) {
        return isOpen(combineMetadata(worldIn, pos));
    }

    public static EnumFacing getFacing(IBlockAccess worldIn, BlockPos pos) {
        return getFacing(combineMetadata(worldIn, pos));
    }

    public static EnumFacing getFacing(int combinedMeta) {
        return EnumFacing.byHorizontalIndex(combinedMeta & 3).rotateYCCW();
    }

    protected static boolean isOpen(int combinedMeta) {
        return (combinedMeta & 4) != 0;
    }

    protected static boolean isTop(int meta) {
        return (meta & 8) != 0;
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
        BlockPos down = pos.down();
        BlockPos up = pos.up();
        if (player.isCreative() && state.getValue(HALF) == EnumDoorHalf.UPPER && worldIn.getBlockState(down).getBlock() == this) worldIn.setBlockToAir(down);
        if (state.getValue(HALF) == EnumDoorHalf.LOWER && worldIn.getBlockState(up).getBlock() == this) {
            if (player.isCreative()) worldIn.setBlockToAir(pos);
            worldIn.setBlockToAir(up);
        }
    }

    /** Each half takes the other's properties. */
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        if (state.getValue(HALF) == EnumDoorHalf.LOWER) {
            IBlockState upper = worldIn.getBlockState(pos.up());
            if (upper.getBlock() == this) state = state.withProperty(HINGE, upper.getValue(HINGE)).withProperty(POWERED, upper.getValue(POWERED));
        } else {
            IBlockState lower = worldIn.getBlockState(pos.down());
            if (lower.getBlock() == this) state = state.withProperty(FACING, lower.getValue(FACING)).withProperty(OPEN, lower.getValue(OPEN));
        }
        return state;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.getValue(HALF) != EnumDoorHalf.LOWER ? state : state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return mirrorIn == Mirror.NONE ? state : state.withRotation(mirrorIn.toRotation(state.getValue(FACING))).cycleProperty(HINGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return (meta & 8) > 0
                ? getDefaultState().withProperty(HALF, EnumDoorHalf.UPPER).withProperty(HINGE, (meta & 1) > 0 ? EnumHingePosition.RIGHT : EnumHingePosition.LEFT)
                        .withProperty(POWERED, (meta & 2) > 0)
                : getDefaultState().withProperty(HALF, EnumDoorHalf.LOWER).withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3).rotateYCCW())
                        .withProperty(OPEN, (meta & 4) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        if (state.getValue(HALF) == EnumDoorHalf.UPPER) {
            i = i | 8;
            if (state.getValue(HINGE) == EnumHingePosition.RIGHT) i |= 1;
            if (state.getValue(POWERED)) i |= 2;
        } else {
            i = i | state.getValue(FACING).rotateY().getHorizontalIndex();
            if (state.getValue(OPEN)) i |= 4;
        }
        return i;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HALF, FACING, OPEN, HINGE, POWERED);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    public enum EnumDoorHalf implements IStringSerializable {
        UPPER,
        LOWER;

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public String getName() {
            return this == UPPER ? "upper" : "lower";
        }
    }

    public enum EnumHingePosition implements IStringSerializable {
        LEFT,
        RIGHT;

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public String getName() {
            return this == LEFT ? "left" : "right";
        }
    }
}

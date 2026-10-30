package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryHelper;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryLargeChest;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityChest;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.ILockableContainer;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 chests (plain and trapped): face the placer (the facing is the metadata), pair with a neighbouring chest of the same kind into a
 * double chest, open unless a cat sits on them or a solid block is above, and the trapped one gives a redstone signal while open.
 */
public class BlockChest extends BlockContainer {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    protected static final AxisAlignedBB NORTH_CHEST_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0D, 0.9375D, 0.875D, 0.9375D);
    protected static final AxisAlignedBB SOUTH_CHEST_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.875D, 1.0D);
    protected static final AxisAlignedBB WEST_CHEST_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0625D, 0.9375D, 0.875D, 0.9375D);
    protected static final AxisAlignedBB EAST_CHEST_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 1.0D, 0.875D, 0.9375D);
    protected static final AxisAlignedBB NOT_CONNECTED_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.875D, 0.9375D);
    public final Type chestType;

    protected BlockChest(Type chestTypeIn) {
        super(Material.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.chestType = chestTypeIn;
        setCreativeTab(chestTypeIn == Type.TRAP ? CreativeTabs.REDSTONE : CreativeTabs.DECORATIONS);
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
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        if (source.getBlockState(pos.north()).getBlock() == this) return NORTH_CHEST_AABB;
        if (source.getBlockState(pos.south()).getBlock() == this) return SOUTH_CHEST_AABB;
        if (source.getBlockState(pos.west()).getBlock() == this) return WEST_CHEST_AABB;
        return source.getBlockState(pos.east()).getBlock() == this ? EAST_CHEST_AABB : NOT_CONNECTED_AABB;
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        checkForSurroundingChests(worldIn, pos, state);
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            BlockPos p = pos.offset(f);
            IBlockState s = worldIn.getBlockState(p);
            if (s.getBlock() == this) checkForSurroundingChests(worldIn, p, s);
        }
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    /** Faces the placer and turns a neighbouring chest of the same kind the same way. */
    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        state = state.withProperty(FACING, placer.getHorizontalFacing().getOpposite());
        for (BlockPos p : new BlockPos[] {pos.north(), pos.south(), pos.west(), pos.east()}) {
            if (worldIn.getBlockState(p).getBlock() == this) worldIn.setBlockState(p, state, 3);
        }
        worldIn.setBlockState(pos, state, 3);
        if (stack.hasDisplayName()) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityChest chest) chest.setCustomName(stack.getDisplayName());
        }
    }

    /** Turns a chest so a double chest faces one way, away from solid blocks where it can. */
    public IBlockState checkForSurroundingChests(World worldIn, BlockPos pos, IBlockState state) {
        if (worldIn.isRemote) return state;
        IBlockState north = worldIn.getBlockState(pos.north());
        IBlockState south = worldIn.getBlockState(pos.south());
        IBlockState west = worldIn.getBlockState(pos.west());
        IBlockState east = worldIn.getBlockState(pos.east());
        EnumFacing facing = state.getValue(FACING);
        if (north.getBlock() != this && south.getBlock() != this) {
            boolean northFull = north.isFullBlock();
            boolean southFull = south.isFullBlock();
            if (west.getBlock() == this || east.getBlock() == this) {
                BlockPos other = west.getBlock() == this ? pos.west() : pos.east();
                IBlockState otherNorth = worldIn.getBlockState(other.north());
                IBlockState otherSouth = worldIn.getBlockState(other.south());
                facing = EnumFacing.SOUTH;
                EnumFacing otherFacing = west.getBlock() == this ? west.getValue(FACING) : east.getValue(FACING);
                if (otherFacing == EnumFacing.NORTH) facing = EnumFacing.NORTH;
                if ((northFull || otherNorth.isFullBlock()) && !southFull && !otherSouth.isFullBlock()) facing = EnumFacing.SOUTH;
                if ((southFull || otherSouth.isFullBlock()) && !northFull && !otherNorth.isFullBlock()) facing = EnumFacing.NORTH;
            }
        } else {
            BlockPos other = north.getBlock() == this ? pos.north() : pos.south();
            IBlockState otherWest = worldIn.getBlockState(other.west());
            IBlockState otherEast = worldIn.getBlockState(other.east());
            facing = EnumFacing.EAST;
            EnumFacing otherFacing = north.getBlock() == this ? north.getValue(FACING) : south.getValue(FACING);
            if (otherFacing == EnumFacing.WEST) facing = EnumFacing.WEST;
            boolean westFull = west.isFullBlock();
            boolean eastFull = east.isFullBlock();
            if ((westFull || otherWest.isFullBlock()) && !eastFull && !otherEast.isFullBlock()) facing = EnumFacing.EAST;
            if ((eastFull || otherEast.isFullBlock()) && !westFull && !otherWest.isFullBlock()) facing = EnumFacing.WEST;
        }
        state = state.withProperty(FACING, facing);
        worldIn.setBlockState(pos, state, 3);
        return state;
    }

    /** A single chest's facing turned away from a solid block in front of it. */
    public IBlockState correctFacing(World worldIn, BlockPos pos, IBlockState state) {
        EnumFacing facing = null;
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            IBlockState s = worldIn.getBlockState(pos.offset(f));
            if (s.getBlock() == this) return state;
            if (s.isFullBlock()) {
                if (facing != null) {
                    facing = null;
                    break;
                }
                facing = f;
            }
        }
        if (facing != null) return state.withProperty(FACING, facing.getOpposite());
        EnumFacing current = state.getValue(FACING);
        if (worldIn.getBlockState(pos.offset(current)).isFullBlock()) current = current.getOpposite();
        if (worldIn.getBlockState(pos.offset(current)).isFullBlock()) current = current.rotateY();
        if (worldIn.getBlockState(pos.offset(current)).isFullBlock()) current = current.getOpposite();
        return state.withProperty(FACING, current);
    }

    /** No more than two chests side by side. */
    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        int chests = 0;
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            BlockPos p = pos.offset(f);
            if (worldIn.getBlockState(p).getBlock() == this) {
                if (isDoubleChest(worldIn, p)) return false;
                ++chests;
            }
        }
        return chests <= 1 && super.canPlaceBlockAt(worldIn, pos);
    }

    private boolean isDoubleChest(World worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock() != this) return false;
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) if (worldIn.getBlockState(pos.offset(f)).getBlock() == this) return true;
        return false;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityChest) te.updateContainingBlockInfo();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof legacyconnector.v1_12_2.net.minecraft.inventory.IInventory inv) {
            InventoryHelper.dropInventoryItems(worldIn, pos, inv);
            worldIn.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        ILockableContainer container = getLockableContainer(worldIn, pos);
        if (container != null) playerIn.displayGUIChest(container);
        return true;
    }

    public ILockableContainer getLockableContainer(World worldIn, BlockPos pos) {
        return getContainer(worldIn, pos, false);
    }

    /** The chest's inventory, or the double chest's when it has a partner; null when blocked (unless allowed). */
    public ILockableContainer getContainer(World worldIn, BlockPos pos, boolean allowBlocking) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityChest chest)) return null;
        if (!allowBlocking && isBlocked(worldIn, pos)) return null;
        ILockableContainer out = chest;
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            BlockPos p = pos.offset(f);
            if (worldIn.getBlockState(p).getBlock() != this) continue;
            if (!allowBlocking && isBlocked(worldIn, p)) return null;
            TileEntity other = worldIn.getTileEntity(p);
            if (other instanceof TileEntityChest otherChest) {
                out = f != EnumFacing.WEST && f != EnumFacing.NORTH ? new InventoryLargeChest("container.chestDouble", out, otherChest)
                        : new InventoryLargeChest("container.chestDouble", otherChest, out);
            }
        }
        return out;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityChest();
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return chestType == Type.TRAP;
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (!blockState.canProvidePower()) return 0;
        TileEntity te = blockAccess.getTileEntity(pos);
        return te instanceof TileEntityChest chest ? Math.max(0, Math.min(15, chest.numPlayersUsing)) : 0;
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.UP ? blockState.getWeakPower(blockAccess, pos, side) : 0;
    }

    /** A solid block above, or a sitting cat on top, keeps the chest shut. */
    protected boolean isBlocked(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos.up()).isSideSolid(worldIn, pos.up(), EnumFacing.DOWN);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return Container.calcRedstoneFromInventory(getLockableContainer(worldIn, pos));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing f = EnumFacing.byIndex(meta);
        if (f.getAxis() == EnumFacing.Axis.Y) f = EnumFacing.NORTH;
        return getDefaultState().withProperty(FACING, f);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
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
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    public enum Type {
        BASIC,
        TRAP;
    }
}

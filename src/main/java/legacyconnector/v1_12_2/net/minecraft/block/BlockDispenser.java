package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.dispenser.IBehaviorDispenseItem;
import legacyconnector.v1_12_2.net.minecraft.dispenser.IBlockSource;
import legacyconnector.v1_12_2.net.minecraft.dispenser.IPosition;
import legacyconnector.v1_12_2.net.minecraft.dispenser.PositionImpl;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryHelper;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityDispenser;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryDefaulted;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2's dispenser: faces any side, fires on a rising redstone edge (a 4-tick update), and dispenses a random filled slot with the
 * behaviour {@link #DISPENSE_BEHAVIOR_REGISTRY} holds for its item. The registry is the modern dispenser's as well
 * ({@link org.kylepitcock.legacyconnector.v1_12_2.bridge.Dispensers1122}).
 */
public class BlockDispenser extends BlockContainer {
    public static final PropertyDirection FACING = BlockDirectional.FACING;
    public static final PropertyBool TRIGGERED = PropertyBool.create("triggered");
    public static final RegistryDefaulted<Item, IBehaviorDispenseItem> DISPENSE_BEHAVIOR_REGISTRY =
            new org.kylepitcock.legacyconnector.v1_12_2.bridge.Dispensers1122.Registry();
    protected Random rand = new Random();

    protected BlockDispenser() {
        super(Material.ROCK);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TRIGGERED, false));
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Override
    public int tickRate(World worldIn) {
        return 4;
    }

    /** Turns to face away from a solid block when placed without a placer facing. */
    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        setDefaultDirection(worldIn, pos, state);
    }

    private void setDefaultDirection(World worldIn, BlockPos pos, IBlockState state) {
        if (worldIn.isRemote) return;
        EnumFacing facing = state.getValue(FACING);
        boolean north = worldIn.getBlockState(pos.north()).isFullBlock();
        boolean south = worldIn.getBlockState(pos.south()).isFullBlock();
        if (facing == EnumFacing.NORTH && north && !south) facing = EnumFacing.SOUTH;
        else if (facing == EnumFacing.SOUTH && south && !north) facing = EnumFacing.NORTH;
        else {
            boolean west = worldIn.getBlockState(pos.west()).isFullBlock();
            boolean east = worldIn.getBlockState(pos.east()).isFullBlock();
            if (facing == EnumFacing.WEST && west && !east) facing = EnumFacing.EAST;
            else if (facing == EnumFacing.EAST && east && !west) facing = EnumFacing.WEST;
        }
        worldIn.setBlockState(pos, state.withProperty(FACING, facing).withProperty(TRIGGERED, false), 2);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityDispenser dispenser) playerIn.displayGUIChest(dispenser);
        return true;
    }

    /** Dispenses one item of a random filled slot (a click and nothing else when empty). */
    protected void dispense(World worldIn, BlockPos pos) {
        BlockSourceImpl source = new BlockSourceImpl(worldIn, pos);
        if (!(source.<TileEntity>getBlockTileEntity() instanceof TileEntityDispenser te)) return;
        int slot = te.getDispenseSlot();
        if (slot < 0) {
            worldIn.playEvent(1001, pos, 0);
            return;
        }
        ItemStack stack = te.getStackInSlot(slot);
        IBehaviorDispenseItem behavior = getBehavior(stack);
        if (behavior != IBehaviorDispenseItem.DEFAULT_BEHAVIOR) te.setInventorySlotContents(slot, behavior.dispense(source, stack));
    }

    protected IBehaviorDispenseItem getBehavior(ItemStack stack) {
        return DISPENSE_BEHAVIOR_REGISTRY.getObject(stack.getItem());
    }

    /** A rising redstone edge schedules the dispense; a falling one clears TRIGGERED. */
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        boolean powered = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(pos.up());
        boolean triggered = state.getValue(TRIGGERED);
        if (powered && !triggered) {
            worldIn.scheduleUpdate(pos, this, tickRate(worldIn));
            worldIn.setBlockState(pos, state.withProperty(TRIGGERED, true), 4);
        } else if (!powered && triggered) {
            worldIn.setBlockState(pos, state.withProperty(TRIGGERED, false), 4);
        }
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!worldIn.isRemote) dispense(worldIn, pos);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityDispenser();
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer)).withProperty(TRIGGERED, false);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos, state.withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer)), 2);
        if (stack.hasDisplayName()) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityDispenser dispenser) dispenser.setCustomName(stack.getDisplayName());
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityDispenser dispenser) {
            InventoryHelper.dropInventoryItems(worldIn, pos, dispenser);
            worldIn.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(worldIn, pos, state);
    }

    /** The point 0.7 blocks out of the dispenser's front face, where items leave it. */
    public static IPosition getDispensePosition(IBlockSource coords) {
        EnumFacing facing = coords instanceof legacyconnector.v1_12_2.net.minecraft.dispenser.BlockSourceImpl impl ? impl.mcc$facing() : coords.getBlockState().getValue(FACING);
        return new PositionImpl(coords.getX() + 0.7D * facing.getXOffset(), coords.getY() + 0.7D * facing.getYOffset(),
                coords.getZ() + 0.7D * facing.getZOffset());
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return Container.calcRedstone(worldIn.getTileEntity(pos));
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7)).withProperty(TRIGGERED, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(FACING).getIndex();
        if (state.getValue(TRIGGERED)) i |= 8;
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
        return new BlockStateContainer(this, FACING, TRIGGERED);
    }
}

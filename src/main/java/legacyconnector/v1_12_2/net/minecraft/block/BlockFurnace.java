package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryHelper;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityFurnace;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 furnace (and the lit furnace, a block of its own in 1.12): faces the placer (the facing is the metadata). {@link #setState}
 * lights or puts out the furnace at a position, the modern furnace's LIT for a vanilla one.
 */
public class BlockFurnace extends BlockContainer {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private final boolean isBurning;
    private static boolean keepInventory;

    public BlockFurnace(boolean isBurning) {
        super(Material.ROCK);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.isBurning = isBurning;
        if (!isBurning) setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.FURNACE);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFurnace furnace) playerIn.displayGUIChest(furnace);
        return true;
    }

    /** Lights or puts out the furnace at a position, keeping its contents. */
    public static void setState(boolean active, World worldIn, BlockPos pos) {
        if (worldIn == null || pos == null) return;
        var level = worldIn.mcc$level();
        if (level != null) {
            var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos);
            var modern = level.getBlockState(mp);
            var lit = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
            if (modern.hasProperty(lit)) {
                level.setBlock(mp, modern.setValue(lit, active), 3);
                return;
            }
        }
        IBlockState state = worldIn.getBlockState(pos);
        TileEntity te = worldIn.getTileEntity(pos);
        keepInventory = true;
        Block target = active ? legacyconnector.v1_12_2.net.minecraft.init.Blocks.LIT_FURNACE : legacyconnector.v1_12_2.net.minecraft.init.Blocks.FURNACE;
        worldIn.setBlockState(pos, target.getDefaultState().withProperty(FACING, state.getValue(FACING)), 3);
        keepInventory = false;
        if (te != null) {
            te.validate();
            worldIn.setTileEntity(pos, te);
        }
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFurnace();
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
        if (stack.hasDisplayName()) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityFurnace furnace) furnace.setCustomInventoryName(stack.getDisplayName());
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (!keepInventory) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityFurnace furnace) {
                InventoryHelper.dropInventoryItems(worldIn, pos, furnace);
                worldIn.updateComparatorOutputLevel(pos, this);
            }
        }
        super.breakBlock(worldIn, pos, state);
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
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Blocks.FURNACE);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
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
}

package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityShulkerBox;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 shulker boxes: one block per colour, opening towards a facing (the metadata); broken, they drop themselves with their contents
 * in the item's BlockEntityTag.
 */
public class BlockShulkerBox extends BlockContainer {
    public static final legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum<EnumFacing> FACING = PropertyDirection.create("facing");
    private final EnumDyeColor color;

    public BlockShulkerBox(EnumDyeColor colorIn) {
        super(Material.ROCK, MapColor.AIR);
        this.color = colorIn;
        setCreativeTab(CreativeTabs.DECORATIONS);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityShulkerBox();
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
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (worldIn.isRemote || playerIn.isSpectator()) return true;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityShulkerBox box) {
            EnumFacing f = state.getValue(FACING);
            if (box.getAnimationStatus() == TileEntityShulkerBox.AnimationStatus.CLOSED
                    && !worldIn.isAirBlock(pos.offset(f)) && worldIn.getBlockState(pos.offset(f)).isOpaqueCube()) {
                return true;
            }
            playerIn.displayGUIChest(box);
        }
        return true;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta));
    }

    /** Drops itself with its contents (unless creative-broken empty). */
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityShulkerBox box && !box.isCleared() && box.shouldDrop()) {
            ItemStack stack = new ItemStack(Item.getItemFromBlock(this));
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagCompound entityTag = new NBTTagCompound();
            tag.setTag("BlockEntityTag", box.saveToNbt(entityTag));
            stack.setTagCompound(tag);
            if (box.hasCustomName()) {
                stack.setStackDisplayName(box.getName());
                box.setCustomName("");
            }
            spawnAsEntity(worldIn, pos, stack);
            worldIn.updateComparatorOutputLevel(pos, state.getBlock());
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (stack.hasDisplayName()) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityShulkerBox box) box.setCustomName(stack.getDisplayName());
        }
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return MapColor.getBlockColor(color);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        ItemStack stack = super.getItem(worldIn, pos, state);
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityShulkerBox box) {
            NBTTagCompound data = box.saveToNbt(new NBTTagCompound());
            if (!data.isEmpty()) stack.setTagInfo("BlockEntityTag", data);
        }
        return stack;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return legacyconnector.v1_12_2.net.minecraft.inventory.Container.calcRedstoneFromInventory((legacyconnector.v1_12_2.net.minecraft.inventory.IInventory) worldIn.getTileEntity(pos));
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    /** The colour of the box (null for the undyed purple box of 1.12, which is purple). */
    public EnumDyeColor getColor() {
        return color;
    }

    public static Block getBlockByColor(EnumDyeColor colorIn) {
        String name = colorIn == null ? "purple_shulker_box" : (colorIn == EnumDyeColor.SILVER ? "silver" : colorIn.getName()) + "_shulker_box";
        return getBlockFromName("minecraft:" + name);
    }

    public static ItemStack getColoredItemStack(EnumDyeColor colorIn) {
        return new ItemStack(getBlockByColor(colorIn));
    }

    public static EnumDyeColor getColorFromItem(Item itemIn) {
        return getColorFromBlock(Block.getBlockFromItem(itemIn));
    }

    public static EnumDyeColor getColorFromBlock(Block blockIn) {
        return blockIn instanceof BlockShulkerBox box ? box.getColor() : EnumDyeColor.PURPLE;
    }
}

package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's wooden slabs: six woods by metadata bits 0-2, bit 3 the top half (single slabs only). */
public abstract class BlockWoodSlab extends BlockSlab {
    public static final PropertyEnum<BlockPlanks.EnumType> VARIANT = PropertyEnum.create("variant", BlockPlanks.EnumType.class);

    public BlockWoodSlab() {
        super(Material.WOOD);
        IBlockState state = blockState.getBaseState();
        if (!isDouble()) state = state.withProperty(HALF, EnumBlockHalf.BOTTOM);
        setDefaultState(state.withProperty(VARIANT, BlockPlanks.EnumType.OAK));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.getValue(VARIANT).getMapColor();
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.WOODEN_SLAB);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Blocks.WOODEN_SLAB, 1, state.getValue(VARIANT).getMetadata());
    }

    @Override
    public String getTranslationKey(int meta) {
        return super.getTranslationKey() + "." + BlockPlanks.EnumType.byMetadata(meta).getTranslationKey();
    }

    @Override
    public IProperty<?> getVariantProperty() {
        return VARIANT;
    }

    @Override
    public Comparable<?> getTypeForItem(ItemStack stack) {
        return BlockPlanks.EnumType.byMetadata(stack.getMetadata() & 7);
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (BlockPlanks.EnumType t : BlockPlanks.EnumType.values()) items.add(new ItemStack(this, 1, t.getMetadata()));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState state = getDefaultState().withProperty(VARIANT, BlockPlanks.EnumType.byMetadata(meta & 7));
        if (!isDouble()) state = state.withProperty(HALF, (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(VARIANT).getMetadata();
        if (!isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP) i |= 8;
        return i;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return isDouble() ? new BlockStateContainer(this, VARIANT) : new BlockStateContainer(this, HALF, VARIANT);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }
}

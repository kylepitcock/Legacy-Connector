package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's red sandstone slab: bit 3 is the top half, or for the double slab the seamless form. */
public abstract class BlockStoneSlabNew extends BlockSlab {
    public static final PropertyBool SEAMLESS = PropertyBool.create("seamless");
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);

    public BlockStoneSlabNew() {
        super(Material.ROCK);
        IBlockState state = blockState.getBaseState();
        state = isDouble() ? state.withProperty(SEAMLESS, false) : state.withProperty(HALF, EnumBlockHalf.BOTTOM);
        setDefaultState(state.withProperty(VARIANT, EnumType.RED_SANDSTONE));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + ".red_sandstone.name");
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.STONE_SLAB2);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Blocks.STONE_SLAB2, 1, state.getValue(VARIANT).getMetadata());
    }

    @Override
    public String getTranslationKey(int meta) {
        return super.getTranslationKey() + "." + EnumType.byMetadata(meta).getTranslationKey();
    }

    @Override
    public IProperty<?> getVariantProperty() {
        return VARIANT;
    }

    @Override
    public Comparable<?> getTypeForItem(ItemStack stack) {
        return EnumType.byMetadata(stack.getMetadata() & 7);
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (EnumType t : EnumType.values()) items.add(new ItemStack(this, 1, t.getMetadata()));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState state = getDefaultState().withProperty(VARIANT, EnumType.byMetadata(meta & 7));
        if (isDouble()) return state.withProperty(SEAMLESS, (meta & 8) != 0);
        return state.withProperty(HALF, (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(VARIANT).getMetadata();
        if (isDouble()) {
            if (state.getValue(SEAMLESS)) i |= 8;
        } else if (state.getValue(HALF) == EnumBlockHalf.TOP) {
            i |= 8;
        }
        return i;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return isDouble() ? new BlockStateContainer(this, SEAMLESS, VARIANT) : new BlockStateContainer(this, HALF, VARIANT);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.getValue(VARIANT).getMapColor();
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    public enum EnumType implements IStringSerializable {
        RED_SANDSTONE(0, "red_sandstone", BlockSand.EnumType.RED_SAND.getMapColor());

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final String name;
        private final MapColor mapColor;

        EnumType(int meta, String name, MapColor mapColor) {
            this.meta = meta;
            this.name = name;
            this.mapColor = mapColor;
        }

        public int getMetadata() {
            return meta;
        }

        public MapColor getMapColor() {
            return mapColor;
        }

        @Override
        public String toString() {
            return name;
        }

        public static EnumType byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) meta = 0;
            return META_LOOKUP[meta];
        }

        @Override
        public String getName() {
            return name;
        }

        public String getTranslationKey() {
            return name;
        }

        public String getUnlocalizedName() {
            return name;
        }

        static {
            for (EnumType t : values()) META_LOOKUP[t.getMetadata()] = t;
        }
    }
}

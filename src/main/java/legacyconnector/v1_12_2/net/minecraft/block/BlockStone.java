package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
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

/** 1.12.2 stone: stone, granite, diorite, andesite and their polished forms by metadata; plain stone drops cobblestone. */
public class BlockStone extends Block {
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);

    public BlockStone() {
        super(Material.ROCK);
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, EnumType.STONE));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + "." + EnumType.STONE.getTranslationKey() + ".name");
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.getValue(VARIANT).getMapColor();
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return state.getValue(VARIANT) == EnumType.STONE ? Item.getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.COBBLESTONE)
                : Item.getItemFromBlock(this);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (EnumType t : EnumType.values()) items.add(new ItemStack(this, 1, t.getMetadata()));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(VARIANT, EnumType.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    public enum EnumType implements IStringSerializable {
        STONE(0, MapColor.STONE, "stone", true),
        GRANITE(1, MapColor.DIRT, "granite", true),
        GRANITE_SMOOTH(2, MapColor.DIRT, "smooth_granite", "graniteSmooth", false),
        DIORITE(3, MapColor.QUARTZ, "diorite", true),
        DIORITE_SMOOTH(4, MapColor.QUARTZ, "smooth_diorite", "dioriteSmooth", false),
        ANDESITE(5, MapColor.STONE, "andesite", true),
        ANDESITE_SMOOTH(6, MapColor.STONE, "smooth_andesite", "andesiteSmooth", false);

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final String name;
        private final String unlocalizedName;
        private final MapColor mapColor;
        private final boolean isNatural;

        EnumType(int meta, MapColor color, String name, boolean natural) {
            this(meta, color, name, name, natural);
        }

        EnumType(int meta, MapColor color, String name, String unlocalizedName, boolean natural) {
            this.meta = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
            this.mapColor = color;
            this.isNatural = natural;
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
            return unlocalizedName;
        }

        public String getUnlocalizedName() {
            return unlocalizedName;
        }

        /** Whether ore generation replaces it (the unpolished kinds). */
        public boolean isNatural() {
            return isNatural;
        }

        static {
            for (EnumType t : values()) META_LOOKUP[t.getMetadata()] = t;
        }
    }
}

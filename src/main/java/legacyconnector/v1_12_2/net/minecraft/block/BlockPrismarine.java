package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** 1.12.2 prismarine: rough, bricks or dark by metadata. */
public class BlockPrismarine extends Block {
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);
    public static final int ROUGH_META = EnumType.ROUGH.getMetadata();
    public static final int BRICKS_META = EnumType.BRICKS.getMetadata();
    public static final int DARK_META = EnumType.DARK.getMetadata();

    public BlockPrismarine() {
        super(Material.ROCK);
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, EnumType.ROUGH));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.getValue(VARIANT) == EnumType.ROUGH ? MapColor.CYAN : MapColor.DIAMOND;
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
        ROUGH(0, "prismarine", "rough"),
        BRICKS(1, "prismarine_bricks", "bricks"),
        DARK(2, "dark_prismarine", "dark");

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final String name;
        private final String translationKey;

        EnumType(int meta, String name, String translationKey) {
            this.meta = meta;
            this.name = name;
            this.translationKey = translationKey;
        }

        public int getMetadata() {
            return meta;
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
            return translationKey;
        }

        public String getUnlocalizedName() {
            return translationKey;
        }

        static {
            for (EnumType t : values()) META_LOOKUP[t.getMetadata()] = t;
        }
    }
}

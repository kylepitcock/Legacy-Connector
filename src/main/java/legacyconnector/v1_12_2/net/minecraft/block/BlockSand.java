package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** 1.12.2 sand: falls, sand or red sand by metadata. */
public class BlockSand extends BlockFalling {
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);

    public BlockSand() {
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, EnumType.SAND));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
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
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.getValue(VARIANT).getMapColor();
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

    public int getDustColor(IBlockState state) {
        return state.getValue(VARIANT).getDustColor();
    }

    public enum EnumType implements IStringSerializable {
        SAND(0, "sand", "default", MapColor.SAND, -2370656),
        RED_SAND(1, "red_sand", "red", MapColor.ADOBE, -5679071);

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final String name;
        private final MapColor mapColor;
        private final String unlocalizedName;
        private final int dustColor;

        EnumType(int meta, String name, String unlocalizedName, MapColor mapColor, int dustColor) {
            this.meta = meta;
            this.name = name;
            this.mapColor = mapColor;
            this.unlocalizedName = unlocalizedName;
            this.dustColor = dustColor;
        }

        public int getDustColor() {
            return dustColor;
        }

        public int getMetadata() {
            return meta;
        }

        @Override
        public String toString() {
            return name;
        }

        public MapColor getMapColor() {
            return mapColor;
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

        static {
            for (EnumType t : values()) META_LOOKUP[t.getMetadata()] = t;
        }
    }
}

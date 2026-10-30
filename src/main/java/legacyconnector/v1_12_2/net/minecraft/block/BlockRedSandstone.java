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

/** 1.12.2 red sandstone: plain, chiseled or smooth by metadata. */
public class BlockRedSandstone extends Block {
    public static final PropertyEnum<EnumType> TYPE = PropertyEnum.create("type", EnumType.class);

    public BlockRedSandstone() {
        super(Material.ROCK, BlockSand.EnumType.RED_SAND.getMapColor());
        setDefaultState(blockState.getBaseState().withProperty(TYPE, EnumType.DEFAULT));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(TYPE).getMetadata();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (EnumType t : EnumType.values()) items.add(new ItemStack(this, 1, t.getMetadata()));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPE, EnumType.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE).getMetadata();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE);
    }

    public enum EnumType implements IStringSerializable {
        DEFAULT(0, "red_sandstone", "default"),
        CHISELED(1, "chiseled_red_sandstone", "chiseled"),
        SMOOTH(2, "smooth_red_sandstone", "smooth");

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final String name;
        private final String unlocalizedName;

        EnumType(int meta, String name, String unlocalizedName) {
            this.meta = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
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

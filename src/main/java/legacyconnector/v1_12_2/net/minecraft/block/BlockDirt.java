package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** 1.12.2 dirt: dirt, coarse dirt or podzol by metadata; podzol is snowy under snow. Coarse dirt drops plain dirt. */
public class BlockDirt extends Block {
    public static final PropertyEnum<DirtType> VARIANT = PropertyEnum.create("variant", DirtType.class);
    public static final PropertyBool SNOWY = PropertyBool.create("snowy");

    protected BlockDirt() {
        super(Material.GROUND);
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, DirtType.DIRT).withProperty(SNOWY, false));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setSoundType(SoundType.GROUND);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.getValue(VARIANT).getColor();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        if (state.getValue(VARIANT) == DirtType.PODZOL) {
            Block above = worldIn.getBlockState(pos.up()).getBlock();
            state = state.withProperty(SNOWY, Block.mcc$named(above, "snow", "snow_layer"));
        }
        return state;
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, DirtType.DIRT.getMetadata()));
        items.add(new ItemStack(this, 1, DirtType.COARSE_DIRT.getMetadata()));
        items.add(new ItemStack(this, 1, DirtType.PODZOL.getMetadata()));
    }

    @Override
    public ItemStack getItem(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(this, 1, state.getValue(VARIANT).getMetadata());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(VARIANT, DirtType.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT, SNOWY);
    }

    @Override
    public int damageDropped(IBlockState state) {
        DirtType t = state.getValue(VARIANT);
        if (t == DirtType.PODZOL) t = DirtType.DIRT;
        return t.getMetadata();
    }

    public enum DirtType implements IStringSerializable {
        DIRT(0, "dirt", "default", MapColor.DIRT),
        COARSE_DIRT(1, "coarse_dirt", "coarse", MapColor.DIRT),
        PODZOL(2, "podzol", MapColor.OBSIDIAN);

        private static final DirtType[] METADATA_LOOKUP = new DirtType[values().length];
        private final int metadata;
        private final String name;
        private final String unlocalizedName;
        private final MapColor color;

        DirtType(int meta, String name, MapColor color) {
            this(meta, name, name, color);
        }

        DirtType(int meta, String name, String unlocalizedName, MapColor color) {
            this.metadata = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
            this.color = color;
        }

        public int getMetadata() {
            return metadata;
        }

        public String getTranslationKey() {
            return unlocalizedName;
        }

        public String getUnlocalizedName() {
            return unlocalizedName;
        }

        public MapColor getColor() {
            return color;
        }

        @Override
        public String toString() {
            return name;
        }

        public static DirtType byMetadata(int metadata) {
            if (metadata < 0 || metadata >= METADATA_LOOKUP.length) metadata = 0;
            return METADATA_LOOKUP[metadata];
        }

        @Override
        public String getName() {
            return name;
        }

        static {
            for (DirtType t : values()) METADATA_LOOKUP[t.getMetadata()] = t;
        }
    }
}

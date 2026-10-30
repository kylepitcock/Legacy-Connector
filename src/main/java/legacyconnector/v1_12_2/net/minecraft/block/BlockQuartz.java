package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 quartz blocks: plain, chiseled, or a pillar along y, x or z (metadata 0-4); pillars turn to the face they are placed on. */
public class BlockQuartz extends Block {
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);

    public BlockQuartz() {
        super(Material.ROCK, MapColor.QUARTZ);
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, EnumType.DEFAULT));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        if (meta == EnumType.LINES_Y.getMetadata()) {
            return switch (facing.getAxis()) {
                case Z -> getDefaultState().withProperty(VARIANT, EnumType.LINES_Z);
                case X -> getDefaultState().withProperty(VARIANT, EnumType.LINES_X);
                default -> getDefaultState().withProperty(VARIANT, EnumType.LINES_Y);
            };
        }
        return meta == EnumType.CHISELED.getMetadata() ? getDefaultState().withProperty(VARIANT, EnumType.CHISELED)
                : getDefaultState().withProperty(VARIANT, EnumType.DEFAULT);
    }

    @Override
    public int damageDropped(IBlockState state) {
        EnumType t = state.getValue(VARIANT);
        return t != EnumType.LINES_X && t != EnumType.LINES_Z ? t.getMetadata() : EnumType.LINES_Y.getMetadata();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, EnumType.DEFAULT.getMetadata()));
        items.add(new ItemStack(this, 1, EnumType.CHISELED.getMetadata()));
        items.add(new ItemStack(this, 1, EnumType.LINES_Y.getMetadata()));
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
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        if (rot == Rotation.COUNTERCLOCKWISE_90 || rot == Rotation.CLOCKWISE_90) {
            EnumType t = state.getValue(VARIANT);
            if (t == EnumType.LINES_X) return state.withProperty(VARIANT, EnumType.LINES_Z);
            if (t == EnumType.LINES_Z) return state.withProperty(VARIANT, EnumType.LINES_X);
        }
        return state;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    public enum EnumType implements IStringSerializable {
        DEFAULT(0, "default", "default"),
        CHISELED(1, "chiseled", "chiseled"),
        LINES_Y(2, "lines_y", "lines"),
        LINES_X(3, "lines_x", "lines"),
        LINES_Z(4, "lines_z", "lines");

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final String serializedName;
        private final String unlocalizedName;

        EnumType(int meta, String name, String unlocalizedName) {
            this.meta = meta;
            this.serializedName = name;
            this.unlocalizedName = unlocalizedName;
        }

        public int getMetadata() {
            return meta;
        }

        @Override
        public String toString() {
            return unlocalizedName;
        }

        public static EnumType byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) meta = 0;
            return META_LOOKUP[meta];
        }

        @Override
        public String getName() {
            return serializedName;
        }

        static {
            for (EnumType t : values()) META_LOOKUP[t.getMetadata()] = t;
        }
    }
}

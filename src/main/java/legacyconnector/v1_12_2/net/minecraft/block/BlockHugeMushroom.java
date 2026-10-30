package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 huge mushroom blocks: which faces show the cap or the stem is the metadata; drops a few small mushrooms. */
public class BlockHugeMushroom extends Block {
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);
    private final Block smallBlock;

    public BlockHugeMushroom(Material materialIn, MapColor color, Block smallBlockIn) {
        super(materialIn, color);
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, EnumType.ALL_OUTSIDE));
        this.smallBlock = smallBlockIn;
    }

    @Override
    public int quantityDropped(Random random) {
        return Math.max(0, random.nextInt(10) - 7);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        EnumType t = state.getValue(VARIANT);
        if (t == EnumType.ALL_STEM || t == EnumType.STEM) return MapColor.CLOTH;
        return super.getMapColor(state, worldIn, pos);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return smallBlock == null ? null : Item.getItemFromBlock(smallBlock);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(this);
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState();
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
        NORTH_WEST(1, "north_west"),
        NORTH(2, "north"),
        NORTH_EAST(3, "north_east"),
        WEST(4, "west"),
        CENTER(5, "center"),
        EAST(6, "east"),
        SOUTH_WEST(7, "south_west"),
        SOUTH(8, "south"),
        SOUTH_EAST(9, "south_east"),
        STEM(10, "stem"),
        ALL_INSIDE(0, "all_inside"),
        ALL_OUTSIDE(14, "all_outside"),
        ALL_STEM(15, "all_stem");

        private static final EnumType[] META_LOOKUP = new EnumType[16];
        private final int meta;
        private final String name;

        EnumType(int meta, String name) {
            this.meta = meta;
            this.name = name;
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
            EnumType t = META_LOOKUP[meta];
            return t == null ? META_LOOKUP[0] : t;
        }

        @Override
        public String getName() {
            return name;
        }

        static {
            for (EnumType t : values()) META_LOOKUP[t.getMetadata()] = t;
        }
    }
}

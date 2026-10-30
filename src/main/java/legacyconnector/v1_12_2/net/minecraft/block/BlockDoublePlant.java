package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 two-block plants (sunflower, lilac, double grass, large fern, rose bush, peony): the lower half stores the kind (bits 0-2),
 * the upper half only its own bit 3 and reads the kind from below.
 */
public class BlockDoublePlant extends BlockBush implements IGrowable {
    public static final PropertyEnum<EnumPlantType> VARIANT = PropertyEnum.create("variant", EnumPlantType.class);
    public static final PropertyEnum<EnumBlockHalf> HALF = PropertyEnum.create("half", EnumBlockHalf.class);
    public static final PropertyEnum<EnumFacing> FACING = BlockHorizontal.FACING;

    public BlockDoublePlant() {
        super(Material.VINE);
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, EnumPlantType.SUNFLOWER).withProperty(HALF, EnumBlockHalf.LOWER)
                .withProperty(FACING, EnumFacing.NORTH));
        setSoundType(SoundType.PLANT);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    /** The kind at a position (the upper half reads the lower one). */
    private EnumPlantType getType(IBlockAccess blockAccess, BlockPos pos, IBlockState state) {
        if (state.getBlock() == this) {
            state = state.getActualState(blockAccess, pos);
            return state.getValue(VARIANT);
        }
        return EnumPlantType.FERN;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return super.canPlaceBlockAt(worldIn, pos) && worldIn.isAirBlock(pos.up());
    }

    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() != this) return true;
        EnumPlantType t = state.getActualState(worldIn, pos).getValue(VARIANT);
        return t == EnumPlantType.FERN || t == EnumPlantType.GRASS;
    }

    @Override
    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state) {
        if (state.getBlock() != this) return super.canBlockStay(worldIn, pos, state);
        if (state.getValue(HALF) == EnumBlockHalf.UPPER) return worldIn.getBlockState(pos.down()).getBlock() == this;
        IBlockState up = worldIn.getBlockState(pos.up());
        return up.getBlock() == this && super.canBlockStay(worldIn, pos, up);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        if (state.getValue(HALF) == EnumBlockHalf.UPPER) return null;
        EnumPlantType t = state.getValue(VARIANT);
        if (t == EnumPlantType.FERN) return null;
        if (t == EnumPlantType.GRASS) return rand.nextInt(8) == 0 ? legacyconnector.v1_12_2.net.minecraft.init.Items.WHEAT_SEEDS : null;
        return super.getItemDropped(state, rand, fortune);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(HALF) != EnumBlockHalf.UPPER && state.getValue(VARIANT) != EnumPlantType.GRASS ? state.getValue(VARIANT).getMeta() : 0;
    }

    /** Places both halves of a plant of a kind. */
    public void placeAt(World worldIn, BlockPos lowerPos, EnumPlantType variant, int flags) {
        worldIn.setBlockState(lowerPos, getDefaultState().withProperty(HALF, EnumBlockHalf.LOWER).withProperty(VARIANT, variant), flags);
        worldIn.setBlockState(lowerPos.up(), getDefaultState().withProperty(HALF, EnumBlockHalf.UPPER), flags);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos.up(), getDefaultState().withProperty(HALF, EnumBlockHalf.UPPER), 2);
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (EnumPlantType t : EnumPlantType.values()) items.add(new ItemStack(this, 1, t.getMeta()));
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(this, 1, getType(worldIn, pos, state).getMeta());
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        EnumPlantType t = getType(worldIn, pos, state);
        return t != EnumPlantType.GRASS && t != EnumPlantType.FERN;
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    /** Bone meal drops another of the flower. */
    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        spawnAsEntity(worldIn, pos, new ItemStack(this, 1, getType(worldIn, pos, state).getMeta()));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return (meta & 8) > 0 ? getDefaultState().withProperty(HALF, EnumBlockHalf.UPPER)
                : getDefaultState().withProperty(HALF, EnumBlockHalf.LOWER).withProperty(VARIANT, EnumPlantType.byMetadata(meta & 7));
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        if (state.getValue(HALF) == EnumBlockHalf.UPPER) {
            IBlockState below = worldIn.getBlockState(pos.down());
            if (below.getBlock() == this) state = state.withProperty(VARIANT, below.getValue(VARIANT));
        }
        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HALF) == EnumBlockHalf.UPPER ? 8 | state.getValue(FACING).getHorizontalIndex() : state.getValue(VARIANT).getMeta();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HALF, VARIANT, FACING);
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    public enum EnumBlockHalf implements IStringSerializable {
        UPPER,
        LOWER;

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public String getName() {
            return this == UPPER ? "upper" : "lower";
        }
    }

    public enum EnumPlantType implements IStringSerializable {
        SUNFLOWER(0, "sunflower"),
        SYRINGA(1, "syringa"),
        GRASS(2, "double_grass", "grass"),
        FERN(3, "double_fern", "fern"),
        ROSE(4, "double_rose", "rose"),
        PAEONIA(5, "paeonia");

        private static final EnumPlantType[] META_LOOKUP = new EnumPlantType[values().length];
        private final int meta;
        private final String name;
        private final String unlocalizedName;

        EnumPlantType(int meta, String name) {
            this(meta, name, name);
        }

        EnumPlantType(int meta, String name, String unlocalizedName) {
            this.meta = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
        }

        public int getMeta() {
            return meta;
        }

        @Override
        public String toString() {
            return name;
        }

        public static EnumPlantType byMetadata(int meta) {
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
            for (EnumPlantType t : values()) META_LOOKUP[t.getMeta()] = t;
        }
    }
}

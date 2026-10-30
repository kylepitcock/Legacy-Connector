package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.IShearable;

/** 1.12.2 tall grass: dead bush, grass or fern by metadata; drops seeds now and then, shears take it, bone meal makes it double. */
public class BlockTallGrass extends BlockBush implements IGrowable, IShearable {
    public static final PropertyEnum<EnumType> TYPE = PropertyEnum.create("type", EnumType.class);
    protected static final AxisAlignedBB TALL_GRASS_AABB = new AxisAlignedBB(0.09999999403953552D, 0.0D, 0.09999999403953552D, 0.8999999761581421D,
            0.800000011920929D, 0.8999999761581421D);

    protected BlockTallGrass() {
        super(Material.VINE);
        setDefaultState(blockState.getBaseState().withProperty(TYPE, EnumType.DEAD_BUSH));
        setSoundType(SoundType.PLANT);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return TALL_GRASS_AABB;
    }

    @Override
    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state) {
        return super.canBlockStay(worldIn, pos, state);
    }

    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        return 1 + random.nextInt(fortune * 2 + 1);
    }

    /** Forge: a grass seed now and then (the registered seed entries), nothing for dead bushes. */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (RANDOM.nextInt(8) != 0) return;
        ItemStack seed = legacyconnector.v1_12_2.net.minecraftforge.common.ForgeHooks.getGrassSeed(RANDOM, fortune);
        if (!seed.isEmpty()) drops.add(seed);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(TYPE).getMeta();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (int i = 1; i < 3; ++i) items.add(new ItemStack(this, 1, i));
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return state.getValue(TYPE) != EnumType.DEAD_BUSH;
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    /** Bone meal: grass becomes double grass, a fern a large fern (the modern tall plants). */
    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        var level = worldIn.mcc$level();
        if (level == null) return;
        var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos);
        var tall = state.getValue(TYPE) == EnumType.FERN ? net.minecraft.world.level.block.Blocks.LARGE_FERN : net.minecraft.world.level.block.Blocks.TALL_GRASS;
        if (level.isEmptyBlock(mp.above())) net.minecraft.world.level.block.DoublePlantBlock.placeAt(level, tall.defaultBlockState(), mp, 2);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPE, EnumType.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE).getMeta();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE);
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XYZ;
    }

    @Override
    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return java.util.List.of(new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Blocks.TALLGRASS, 1,
                world.getBlockState(pos).getValue(TYPE).getMeta()));
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 60;
    }

    public enum EnumType implements IStringSerializable {
        DEAD_BUSH(0, "dead_bush"),
        GRASS(1, "tall_grass"),
        FERN(2, "fern");

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final String name;

        EnumType(int meta, String name) {
            this.meta = meta;
            this.name = name;
        }

        public int getMeta() {
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

        static {
            for (EnumType t : values()) META_LOOKUP[t.getMeta()] = t;
        }
    }
}

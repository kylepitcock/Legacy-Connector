package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A slab: a half block in the bottom or top half (HALF), or the double block of two. Subclasses name the variants. */
public abstract class BlockSlab extends Block {
    public static final PropertyEnum<EnumBlockHalf> HALF = PropertyEnum.create("half", EnumBlockHalf.class);
    protected static final AxisAlignedBB AABB_BOTTOM_HALF = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    protected static final AxisAlignedBB AABB_TOP_HALF = new AxisAlignedBB(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);

    public BlockSlab(Material material) {
        this(material, material == null ? null : material.getMaterialMapColor());
    }

    public BlockSlab(Material material, MapColor color) {
        super(material, color);
        this.fullBlock = isDouble();
        setLightOpacity(255);
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    /** The half the slab fills: the shape of the modern block. */
    @Override
    public AxisAlignedBB mcc$staticBounds(IBlockState state) {
        if (isDouble()) return FULL_BLOCK_AABB;
        try {
            return state.getValue(HALF) == EnumBlockHalf.TOP ? AABB_TOP_HALF : AABB_BOTTOM_HALF;
        } catch (RuntimeException e) {
            return AABB_BOTTOM_HALF;
        }
    }

    @Override
    public boolean isTopSolid(IBlockState state) {
        return isDouble() || state.getValue(HALF) == EnumBlockHalf.TOP;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return isDouble();
    }

    /** Placed into the top half when clicked under a block or on the upper half of a side. */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(HALF, EnumBlockHalf.BOTTOM);
        if (isDouble()) return state;
        return facing != EnumFacing.DOWN && (facing == EnumFacing.UP || hitY <= 0.5D) ? state : state.withProperty(HALF, EnumBlockHalf.TOP);
    }

    @Override
    public int quantityDropped(Random random) {
        return isDouble() ? 2 : 1;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return isDouble();
    }

    protected static boolean isHalfSlab(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockSlab slab && !slab.isDouble();
    }

    public abstract String getTranslationKey(int meta);

    public abstract boolean isDouble();

    public abstract IProperty<?> getVariantProperty();

    public abstract Comparable<?> getTypeForItem(ItemStack stack);

    /** 1.12's other name for {@link #getTranslationKey(int)}. */
    public String getUnlocalizedName(int meta) {
        return getTranslationKey(meta);
    }

    public enum EnumBlockHalf implements IStringSerializable {
        TOP("top"), BOTTOM("bottom");

        private final String name;

        EnumBlockHalf(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}

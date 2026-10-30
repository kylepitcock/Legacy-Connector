package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** 1.12.2's first log block: oak, spruce, birch and jungle (metadata bits 0-1) with the axis in bits 2-3. */
public class BlockOldLog extends BlockLog {
    public static final PropertyEnum<BlockPlanks.EnumType> VARIANT = PropertyEnum.create("variant", BlockPlanks.EnumType.class,
            t -> t.getMetadata() < 4);

    public BlockOldLog() {
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, BlockPlanks.EnumType.OAK).withProperty(LOG_AXIS, EnumAxis.Y));
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        BlockPlanks.EnumType t = state.getValue(VARIANT);
        if (state.getValue(LOG_AXIS) == EnumAxis.Y) return t.getMapColor();
        return switch (t) {
            case SPRUCE -> BlockPlanks.EnumType.DARK_OAK.getMapColor();
            case BIRCH -> MapColor.QUARTZ;
            case JUNGLE -> BlockPlanks.EnumType.SPRUCE.getMapColor();
            default -> BlockPlanks.EnumType.SPRUCE.getMapColor();
        };
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (int i = 0; i < 4; i++) items.add(new ItemStack(this, 1, i));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(VARIANT, BlockPlanks.EnumType.byMetadata((meta & 3) % 4)).withProperty(LOG_AXIS, axisOf(meta));
    }

    static EnumAxis axisOf(int meta) {
        return switch (meta & 12) {
            case 0 -> EnumAxis.Y;
            case 4 -> EnumAxis.X;
            case 8 -> EnumAxis.Z;
            default -> EnumAxis.NONE;
        };
    }

    static int axisBits(EnumAxis axis) {
        return switch (axis) {
            case X -> 4;
            case Z -> 8;
            case NONE -> 12;
            default -> 0;
        };
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata() | axisBits(state.getValue(LOG_AXIS));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT, LOG_AXIS);
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.item.Item.getItemFromBlock(this), 1, state.getValue(VARIANT).getMetadata());
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }
}

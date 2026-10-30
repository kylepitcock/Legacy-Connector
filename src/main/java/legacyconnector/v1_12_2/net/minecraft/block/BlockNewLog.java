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

/** 1.12.2's second log block: acacia and dark oak (metadata bit 0) with the axis in bits 2-3. */
public class BlockNewLog extends BlockLog {
    public static final PropertyEnum<BlockPlanks.EnumType> VARIANT = PropertyEnum.create("variant", BlockPlanks.EnumType.class,
            t -> t.getMetadata() >= 4);

    public BlockNewLog() {
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, BlockPlanks.EnumType.ACACIA).withProperty(LOG_AXIS, EnumAxis.Y));
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        BlockPlanks.EnumType t = state.getValue(VARIANT);
        if (state.getValue(LOG_AXIS) == EnumAxis.Y) return t.getMapColor();
        return t == BlockPlanks.EnumType.ACACIA ? MapColor.STONE : BlockPlanks.EnumType.DARK_OAK.getMapColor();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
        items.add(new ItemStack(this, 1, 1));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(VARIANT, BlockPlanks.EnumType.byMetadata((meta & 3) + 4)).withProperty(LOG_AXIS, BlockOldLog.axisOf(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata() - 4 | BlockOldLog.axisBits(state.getValue(LOG_AXIS));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT, LOG_AXIS);
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.item.Item.getItemFromBlock(this), 1, state.getValue(VARIANT).getMetadata() - 4);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata() - 4;
    }
}

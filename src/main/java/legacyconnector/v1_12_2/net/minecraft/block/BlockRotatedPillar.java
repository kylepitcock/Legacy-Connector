package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A pillar along an axis (quartz pillars, hay, bones): placed along the clicked side's axis; metadata bits 2-3 hold the axis. */
public class BlockRotatedPillar extends Block {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class);

    protected BlockRotatedPillar(Material material) {
        super(material, material == null ? null : material.getMaterialMapColor());
    }

    protected BlockRotatedPillar(Material material, MapColor color) {
        super(material, color);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        if (rot != Rotation.COUNTERCLOCKWISE_90 && rot != Rotation.CLOCKWISE_90) return state;
        return switch (state.getValue(AXIS)) {
            case X -> state.withProperty(AXIS, EnumFacing.Axis.Z);
            case Z -> state.withProperty(AXIS, EnumFacing.Axis.X);
            default -> state;
        };
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing.Axis axis = EnumFacing.Axis.Y;
        int i = meta & 12;
        if (i == 4) axis = EnumFacing.Axis.X;
        else if (i == 8) axis = EnumFacing.Axis.Z;
        return getDefaultState().withProperty(AXIS, axis);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        EnumFacing.Axis axis = state.getValue(AXIS);
        return axis == EnumFacing.Axis.X ? 4 : axis == EnumFacing.Axis.Z ? 8 : 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(this));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(AXIS, facing.getAxis());
    }
}

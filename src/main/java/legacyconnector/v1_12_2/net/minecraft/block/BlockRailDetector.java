package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's detector rail: powered (bit 3) while a cart is on it; straight shapes in bits 0-2. */
public class BlockRailDetector extends BlockRailBase {
    public static final PropertyEnum<EnumRailDirection> SHAPE = PropertyEnum.create("shape", EnumRailDirection.class,
            d -> d != EnumRailDirection.NORTH_EAST && d != EnumRailDirection.NORTH_WEST && d != EnumRailDirection.SOUTH_EAST
                    && d != EnumRailDirection.SOUTH_WEST);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockRailDetector() {
        super(true);
        setDefaultState(blockState.getBaseState().withProperty(POWERED, false).withProperty(SHAPE, EnumRailDirection.NORTH_SOUTH));
        setTickRandomly(true);
    }

    @Override
    public int tickRate(World worldIn) {
        return 20;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (!worldIn.isRemote && !state.getValue(POWERED)) updatePoweredState(worldIn, pos, state);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!worldIn.isRemote && state.getValue(POWERED)) updatePoweredState(worldIn, pos, state);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return blockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return !blockState.getValue(POWERED) ? 0 : side == EnumFacing.UP ? 15 : 0;
    }

    private void updatePoweredState(World worldIn, BlockPos pos, IBlockState state) {
        boolean was = state.getValue(POWERED);
        boolean now = !findMinecarts(worldIn, pos, EntityMinecart.class).isEmpty();
        if (now != was) {
            worldIn.setBlockState(pos, state.withProperty(POWERED, now), 3);
            worldIn.notifyNeighborsOfStateChange(pos, this, false);
            worldIn.notifyNeighborsOfStateChange(pos.down(), this, false);
        }
        if (now) worldIn.scheduleUpdate(new BlockPos(pos), this, tickRate(worldIn));
        worldIn.updateComparatorOutputLevel(pos, this);
    }

    @Override
    public IProperty<EnumRailDirection> getShapeProperty() {
        return SHAPE;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return 0;
    }

    @SafeVarargs
    protected final <T extends EntityMinecart> List<T> findMinecarts(World worldIn, BlockPos pos, Class<T> clazz,
            com.google.common.base.Predicate<Entity>... filter) {
        AxisAlignedBB box = new AxisAlignedBB(pos.getX() + 0.2F, pos.getY(), pos.getZ() + 0.2F, pos.getX() + 1 - 0.2F, pos.getY() + 1 - 0.2F,
                pos.getZ() + 1 - 0.2F);
        return worldIn.getEntitiesWithinAABB(clazz, box);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(SHAPE, EnumRailDirection.byMetadata(meta & 7)).withProperty(POWERED, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(SHAPE).getMetadata();
        if (state.getValue(POWERED)) i |= 8;
        return i;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SHAPE, POWERED);
    }
}

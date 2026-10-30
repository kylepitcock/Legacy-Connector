package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's powered and activator rails: straight shapes (bits 0-2), POWERED (bit 3) while a redstone signal reaches them. */
public class BlockRailPowered extends BlockRailBase {
    public static final PropertyEnum<EnumRailDirection> SHAPE = PropertyEnum.create("shape", EnumRailDirection.class,
            d -> d != EnumRailDirection.NORTH_EAST && d != EnumRailDirection.NORTH_WEST && d != EnumRailDirection.SOUTH_EAST
                    && d != EnumRailDirection.SOUTH_WEST);
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    private final boolean isActivator;

    protected BlockRailPowered() {
        this(false);
    }

    protected BlockRailPowered(boolean isActivator) {
        super(true);
        this.isActivator = isActivator;
        setDefaultState(blockState.getBaseState().withProperty(SHAPE, EnumRailDirection.NORTH_SOUTH).withProperty(POWERED, false));
    }

    /** Forge: whether this is an activator rail. */
    public boolean isActivatorRail() {
        return isActivator;
    }

    @Override
    protected void updateState(IBlockState state, World worldIn, BlockPos pos, Block blockIn) {
        boolean was = state.getValue(POWERED);
        boolean now = worldIn.isBlockPowered(pos);
        if (now != was) {
            worldIn.setBlockState(pos, state.withProperty(POWERED, now), 3);
            worldIn.notifyNeighborsOfStateChange(pos.down(), this, false);
            if (state.getValue(SHAPE).isAscending()) worldIn.notifyNeighborsOfStateChange(pos.up(), this, false);
        }
    }

    @Override
    public IProperty<EnumRailDirection> getShapeProperty() {
        return SHAPE;
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

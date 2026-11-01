package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;

/** The stairs base class; mods extend it for their own stairs (and test vanilla blocks against it). */
public class BlockStairs extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", java.util.List.of(
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.SOUTH,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.WEST, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.EAST));
    public static final PropertyEnum<EnumHalf> HALF = PropertyEnum.create("half", EnumHalf.class);
    public static final PropertyEnum<EnumShape> SHAPE = PropertyEnum.create("shape", EnumShape.class);

    protected final Block modelBlock;
    protected final IBlockState modelState;

    protected BlockStairs(IBlockState modelState) {
        super(modelState.getMaterial());
        this.modelBlock = modelState.getBlock();
        this.modelState = modelState;
        setDefaultState(blockState.getBaseState().withProperty(FACING, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH)
                .withProperty(HALF, EnumHalf.BOTTOM).withProperty(SHAPE, EnumShape.STRAIGHT));
        this.fullBlock = false;
        this.lightOpacity = 255;
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer(this, FACING, HALF, SHAPE);
    }

    /** Metadata as 1.12 stored it: bit 2 is the top half, the low two bits are 5 - the index of the facing (east, west, south, north). */
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HALF, (meta & 4) > 0 ? EnumHalf.TOP : EnumHalf.BOTTOM)
                .withProperty(FACING, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.byIndex(5 - (meta & 3)));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(HALF) == EnumHalf.TOP ? 4 : 0;
        return meta | (5 - state.getValue(FACING).getIndex());
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** Faces the placer, in the upper half when clicked under a block or on the upper half of a side. */
    @Override
    public IBlockState getStateForPlacement(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase placer) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(SHAPE, EnumShape.STRAIGHT);
        legacyconnector.v1_12_2.net.minecraft.util.EnumFacing look = placer == null ? legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH : placer.getHorizontalFacing();
        boolean top = facing != legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP && (facing == legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.DOWN || hitY > 0.5F);
        return state.withProperty(FACING, look).withProperty(HALF, top ? EnumHalf.TOP : EnumHalf.BOTTOM);
    }

    public enum EnumHalf implements IStringSerializable {
        TOP("top"), BOTTOM("bottom");

        private final String name;

        EnumHalf(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum EnumShape implements IStringSerializable {
        STRAIGHT("straight"), INNER_LEFT("inner_left"), INNER_RIGHT("inner_right"), OUTER_LEFT("outer_left"), OUTER_RIGHT("outer_right");

        private final String name;

        EnumShape(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Whether the state is of a stairs block. */
    public static boolean isBlockStairs(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return state.getBlock() instanceof BlockStairs;
    }
}

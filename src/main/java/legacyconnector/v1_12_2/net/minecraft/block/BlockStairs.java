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

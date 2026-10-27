package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3i;

public enum EnumFacing implements IStringSerializable {
    DOWN(0, 1, -1, "down", AxisDirection.NEGATIVE, Axis.Y, new Vec3i(0, -1, 0)),
    UP(1, 0, -1, "up", AxisDirection.POSITIVE, Axis.Y, new Vec3i(0, 1, 0)),
    NORTH(2, 3, 2, "north", AxisDirection.NEGATIVE, Axis.Z, new Vec3i(0, 0, -1)),
    SOUTH(3, 2, 0, "south", AxisDirection.POSITIVE, Axis.Z, new Vec3i(0, 0, 1)),
    WEST(4, 5, 1, "west", AxisDirection.NEGATIVE, Axis.X, new Vec3i(-1, 0, 0)),
    EAST(5, 4, 3, "east", AxisDirection.POSITIVE, Axis.X, new Vec3i(1, 0, 0));

    private final int index;
    private final int opposite;
    private final int horizontalIndex;
    private final String name;
    private final Axis axis;
    private final AxisDirection axisDirection;
    private final Vec3i directionVec;

    private static final EnumFacing[] VALUES = values();
    private static final EnumFacing[] HORIZONTALS = {SOUTH, WEST, NORTH, EAST};

    EnumFacing(int index, int opposite, int horizontalIndex, String name, AxisDirection axisDirection, Axis axis, Vec3i directionVec) {
        this.index = index;
        this.horizontalIndex = horizontalIndex;
        this.opposite = opposite;
        this.name = name;
        this.axis = axis;
        this.axisDirection = axisDirection;
        this.directionVec = directionVec;
    }

    public int getIndex() {
        return index;
    }

    public int getHorizontalIndex() {
        return horizontalIndex;
    }

    public AxisDirection getAxisDirection() {
        return axisDirection;
    }

    public EnumFacing getOpposite() {
        return VALUES[opposite];
    }

    public EnumFacing rotateAround(Axis axis) {
        switch (axis) {
            case X:
                if (this != WEST && this != EAST) return rotateX();
                return this;
            case Y:
                if (this != UP && this != DOWN) return rotateY();
                return this;
            case Z:
                if (this != NORTH && this != SOUTH) return rotateZ();
                return this;
            default:
                throw new IllegalStateException("Unable to get CW facing for axis " + axis);
        }
    }

    public EnumFacing rotateY() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
        };
    }

    public EnumFacing rotateX() {
        return switch (this) {
            case NORTH -> DOWN;
            case EAST, WEST -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
            case SOUTH -> UP;
            case UP -> NORTH;
            case DOWN -> SOUTH;
        };
    }

    public EnumFacing rotateZ() {
        return switch (this) {
            case EAST -> DOWN;
            case SOUTH, NORTH -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
            case WEST -> UP;
            case UP -> EAST;
            case DOWN -> WEST;
        };
    }

    public EnumFacing rotateYCCW() {
        return switch (this) {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
            default -> throw new IllegalStateException("Unable to get CCW facing of " + this);
        };
    }

    public int getXOffset() {
        return axis == Axis.X ? axisDirection.getOffset() : 0;
    }

    public int getYOffset() {
        return axis == Axis.Y ? axisDirection.getOffset() : 0;
    }

    public int getZOffset() {
        return axis == Axis.Z ? axisDirection.getOffset() : 0;
    }

    public String getName2() {
        return name;
    }

    public Axis getAxis() {
        return axis;
    }

    public static EnumFacing byName(String name) {
        if (name == null) return null;
        for (EnumFacing f : VALUES) if (f.name.equals(name.toLowerCase(Locale.ROOT))) return f;
        return null;
    }

    public static EnumFacing byIndex(int index) {
        return VALUES[Math.abs(index % VALUES.length)];
    }

    public static EnumFacing byHorizontalIndex(int index) {
        return HORIZONTALS[Math.abs(index % HORIZONTALS.length)];
    }

    public static EnumFacing fromAngle(double angle) {
        return byHorizontalIndex((int) Math.floor(angle / 90.0D + 0.5D) & 3);
    }

    public float getHorizontalAngle() {
        return (float) ((horizontalIndex & 3) * 90);
    }

    public static EnumFacing random(Random rand) {
        return VALUES[rand.nextInt(VALUES.length)];
    }

    public static EnumFacing getFacingFromVector(float x, float y, float z) {
        EnumFacing best = NORTH;
        float max = Float.MIN_VALUE;
        for (EnumFacing f : VALUES) {
            float dot = x * f.directionVec.getX() + y * f.directionVec.getY() + z * f.directionVec.getZ();
            if (dot > max) {
                max = dot;
                best = f;
            }
        }
        return best;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    public static EnumFacing getFacingFromAxis(AxisDirection direction, Axis axis) {
        for (EnumFacing f : VALUES) if (f.getAxisDirection() == direction && f.getAxis() == axis) return f;
        throw new IllegalArgumentException("No such direction: " + direction + " " + axis);
    }

    public Vec3i getDirectionVec() {
        return directionVec;
    }

    public enum Axis implements com.google.common.base.Predicate<EnumFacing>, IStringSerializable {
        X("x", Plane.HORIZONTAL), Y("y", Plane.VERTICAL), Z("z", Plane.HORIZONTAL);

        private final String name;
        private final Plane plane;

        Axis(String name, Plane plane) {
            this.name = name;
            this.plane = plane;
        }

        public static Axis byName(String name) {
            if (name == null) return null;
            for (Axis a : values()) if (a.name.equals(name.toLowerCase(Locale.ROOT))) return a;
            return null;
        }

        public String getName2() {
            return name;
        }

        public boolean isVertical() {
            return plane == Plane.VERTICAL;
        }

        public boolean isHorizontal() {
            return plane == Plane.HORIZONTAL;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean apply(EnumFacing facing) {
            return facing != null && facing.getAxis() == this;
        }

        public Plane getPlane() {
            return plane;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum AxisDirection {
        POSITIVE(1, "Towards positive"), NEGATIVE(-1, "Towards negative");

        private final int offset;
        private final String description;

        AxisDirection(int offset, String description) {
            this.offset = offset;
            this.description = description;
        }

        public int getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public enum Plane implements com.google.common.base.Predicate<EnumFacing>, Iterable<EnumFacing> {
        HORIZONTAL, VERTICAL;

        public EnumFacing[] facings() {
            return this == HORIZONTAL ? new EnumFacing[] {NORTH, EAST, SOUTH, WEST} : new EnumFacing[] {UP, DOWN};
        }

        public EnumFacing random(Random rand) {
            EnumFacing[] f = facings();
            return f[rand.nextInt(f.length)];
        }

        @Override
        public boolean apply(EnumFacing facing) {
            return facing != null && facing.getAxis().getPlane() == this;
        }

        @Override
        public Iterator<EnumFacing> iterator() {
            return java.util.Arrays.asList(facings()).iterator();
        }
    }

    public net.minecraft.core.Direction mcc$modern() {
        return net.minecraft.core.Direction.from3DDataValue(index);
    }

    /** The face of a block placed at {@code pos} that faces the placer (up or down when it stands right above or below). */
    public static EnumFacing getDirectionFromEntityLiving(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase placer) {
        net.minecraft.world.entity.Entity e = placer.mcc$modern();
        if (e == null) return NORTH;
        if (Math.abs(e.getX() - (pos.getX() + 0.5F)) < 2.0D && Math.abs(e.getZ() - (pos.getZ() + 0.5F)) < 2.0D) {
            double eyes = e.getEyeY();
            if (eyes - pos.getY() > 2.0D) return UP;
            if (pos.getY() - eyes > 0.0D) return DOWN;
        }
        return mcc$of(e.getDirection()).getOpposite();
    }

    public static EnumFacing mcc$of(net.minecraft.core.Direction direction) {
        return direction == null ? null : VALUES[direction.get3DDataValue()];
    }
}

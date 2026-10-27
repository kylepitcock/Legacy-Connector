package legacyconnector.v1_12_2.net.minecraft.util;

public enum Mirror {
    NONE, LEFT_RIGHT, FRONT_BACK;

    public Rotation toRotation(EnumFacing facing) {
        EnumFacing.Axis axis = facing.getAxis();
        return (this != LEFT_RIGHT || axis != EnumFacing.Axis.Z) && (this != FRONT_BACK || axis != EnumFacing.Axis.X) ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    public EnumFacing mirror(EnumFacing facing) {
        if (this == FRONT_BACK && facing.getAxis() == EnumFacing.Axis.X) return facing.getOpposite();
        return this == LEFT_RIGHT && facing.getAxis() == EnumFacing.Axis.Z ? facing.getOpposite() : facing;
    }

    /** The rotation index (of {@code rotationCount} steps) seen in the mirror. */
    public int mirrorRotation(int rotationIn, int rotationCount) {
        int i = rotationCount / 2;
        int j = rotationIn > i ? rotationIn - rotationCount : rotationIn;
        return switch (this) {
            case FRONT_BACK -> (rotationCount - j) % rotationCount;
            case LEFT_RIGHT -> (i - j + rotationCount) % rotationCount;
            default -> rotationIn;
        };
    }
}

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
}

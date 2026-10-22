package legacyconnector.v1_12_2.net.minecraft.util;

public enum Rotation {
    NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90;

    public Rotation add(Rotation rotation) {
        return values()[(ordinal() + rotation.ordinal()) % 4];
    }

    public EnumFacing rotate(EnumFacing facing) {
        if (facing.getAxis() == EnumFacing.Axis.Y) return facing;
        EnumFacing f = facing;
        for (int i = 0; i < ordinal(); i++) f = f.rotateY();
        return f;
    }

    public int rotate(int value, int count) {
        return (value + ordinal() * (count / 4)) % count;
    }
}

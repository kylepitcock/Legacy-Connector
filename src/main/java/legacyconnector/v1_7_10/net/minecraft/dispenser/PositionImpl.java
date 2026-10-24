package legacyconnector.v1_7_10.net.minecraft.dispenser;

public class PositionImpl implements IPosition {
    protected final double x;
    protected final double y;
    protected final double z;

    public PositionImpl(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }
}

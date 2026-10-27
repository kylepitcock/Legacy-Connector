package legacyconnector.v1_12_2.net.minecraft.dispenser;

public class PositionImpl implements IPosition {
    protected final double x;
    protected final double y;
    protected final double z;

    public PositionImpl(double xCoord, double yCoord, double zCoord) {
        this.x = xCoord;
        this.y = yCoord;
        this.z = zCoord;
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

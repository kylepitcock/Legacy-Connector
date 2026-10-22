package legacyconnector.v1_12_2.net.minecraft.util.math;

public class Vec3i implements Comparable<Vec3i> {
    public static final Vec3i NULL_VECTOR = new Vec3i(0, 0, 0);
    private final int x;
    private final int y;
    private final int z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3i(double x, double y, double z) {
        this(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vec3i v)) return false;
        return getX() == v.getX() && getY() == v.getY() && getZ() == v.getZ();
    }

    @Override
    public int hashCode() {
        return (getY() + getZ() * 31) * 31 + getX();
    }

    @Override
    public int compareTo(Vec3i o) {
        if (getY() == o.getY()) return getZ() == o.getZ() ? getX() - o.getX() : getZ() - o.getZ();
        return getY() - o.getY();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Vec3i crossProduct(Vec3i v) {
        return new Vec3i(getY() * v.getZ() - getZ() * v.getY(), getZ() * v.getX() - getX() * v.getZ(), getX() * v.getY() - getY() * v.getX());
    }

    public double getDistance(int xIn, int yIn, int zIn) {
        double dx = getX() - xIn, dy = getY() - yIn, dz = getZ() - zIn;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distanceSq(double toX, double toY, double toZ) {
        double dx = getX() - toX, dy = getY() - toY, dz = getZ() - toZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceSqToCenter(double xIn, double yIn, double zIn) {
        double dx = getX() + 0.5D - xIn, dy = getY() + 0.5D - yIn, dz = getZ() + 0.5D - zIn;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceSq(Vec3i to) {
        return distanceSq(to.getX(), to.getY(), to.getZ());
    }

    @Override
    public String toString() {
        return "Vec3i{x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}

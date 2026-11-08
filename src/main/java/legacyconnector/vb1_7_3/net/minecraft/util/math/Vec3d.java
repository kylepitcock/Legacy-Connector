package legacyconnector.vb1_7_3.net.minecraft.util.math;

/** Beta 1.7.3 {@code Vec3D}. The original pooled its instances ({@link #createCached}); here every call makes a new one. */
public class Vec3d {
    public double x;
    public double y;
    public double z;

    protected Vec3d(double x, double y, double z) {
        if (x == -0.0) x = 0.0;
        if (y == -0.0) y = 0.0;
        if (z == -0.0) z = 0.0;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** MCP {@code createVectorHelper}. */
    public static Vec3d create(double x, double y, double z) {
        return new Vec3d(x, y, z);
    }

    public static void clearCache() {}

    public static void resetCacheCount() {}

    /** MCP {@code createVector} (pooled in b1.7.3). */
    public static Vec3d createCached(double x, double y, double z) {
        return new Vec3d(x, y, z);
    }

    protected Vec3d set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /** MCP {@code subtract}: {@code other - this}. */
    public Vec3d relativize(Vec3d other) {
        return createCached(other.x - x, other.y - y, other.z - z);
    }

    public Vec3d normalize() {
        double len = Math.sqrt(x * x + y * y + z * z);
        return len < 1.0E-4 ? createCached(0.0, 0.0, 0.0) : createCached(x / len, y / len, z / len);
    }

    public Vec3d crossProduct(Vec3d o) {
        return createCached(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public Vec3d add(double dx, double dy, double dz) {
        return createCached(x + dx, y + dy, z + dz);
    }

    public double distanceTo(Vec3d o) {
        return Math.sqrt(squaredDistanceTo(o));
    }

    public double squaredDistanceTo(Vec3d o) {
        double dx = o.x - x, dy = o.y - y, dz = o.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double squaredDistanceTo(double px, double py, double pz) {
        double dx = px - x, dy = py - y, dz = pz - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** MCP {@code getIntermediateWithXValue}: the point of the segment to {@code o} at that x, or null. */
    public Vec3d interpolateByX(Vec3d o, double at) {
        double dx = o.x - x, dy = o.y - y, dz = o.z - z;
        if (dx * dx < 1.0E-7) return null;
        double t = (at - x) / dx;
        return t >= 0.0 && t <= 1.0 ? createCached(x + dx * t, y + dy * t, z + dz * t) : null;
    }

    public Vec3d interpolateByY(Vec3d o, double at) {
        double dx = o.x - x, dy = o.y - y, dz = o.z - z;
        if (dy * dy < 1.0E-7) return null;
        double t = (at - y) / dy;
        return t >= 0.0 && t <= 1.0 ? createCached(x + dx * t, y + dy * t, z + dz * t) : null;
    }

    public Vec3d interpolateByZ(Vec3d o, double at) {
        double dx = o.x - x, dy = o.y - y, dz = o.z - z;
        if (dz * dz < 1.0E-7) return null;
        double t = (at - z) / dz;
        return t >= 0.0 && t <= 1.0 ? createCached(x + dx * t, y + dy * t, z + dz * t) : null;
    }

    public void rotateX(float angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double ny = y * c + z * s, nz = z * c - y * s;
        y = ny;
        z = nz;
    }

    public void rotateY(float angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double nx = x * c + z * s, nz = z * c - x * s;
        x = nx;
        z = nz;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}

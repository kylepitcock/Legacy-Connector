package legacyconnector.v1_12_2.net.minecraft.util.math;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

public class Vec3d {
    public static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);
    public final double x;
    public final double y;
    public final double z;

    public Vec3d(double xIn, double yIn, double zIn) {
        if (xIn == -0.0D) xIn = 0.0D;
        if (yIn == -0.0D) yIn = 0.0D;
        if (zIn == -0.0D) zIn = 0.0D;
        this.x = xIn;
        this.y = yIn;
        this.z = zIn;
    }

    public Vec3d(Vec3i vector) {
        this(vector.getX(), vector.getY(), vector.getZ());
    }

    public Vec3d subtractReverse(Vec3d vec) {
        return new Vec3d(vec.x - x, vec.y - y, vec.z - z);
    }

    public Vec3d normalize() {
        double d = MathHelper.sqrt(x * x + y * y + z * z);
        return d < 1.0E-4D ? ZERO : new Vec3d(x / d, y / d, z / d);
    }

    public double dotProduct(Vec3d vec) {
        return x * vec.x + y * vec.y + z * vec.z;
    }

    public Vec3d crossProduct(Vec3d vec) {
        return new Vec3d(y * vec.z - z * vec.y, z * vec.x - x * vec.z, x * vec.y - y * vec.x);
    }

    public Vec3d subtract(Vec3d vec) {
        return subtract(vec.x, vec.y, vec.z);
    }

    public Vec3d subtract(double xIn, double yIn, double zIn) {
        return addVector(-xIn, -yIn, -zIn);
    }

    public Vec3d add(Vec3d vec) {
        return addVector(vec.x, vec.y, vec.z);
    }

    public Vec3d add(double xIn, double yIn, double zIn) {
        return addVector(xIn, yIn, zIn);
    }

    public Vec3d addVector(double xIn, double yIn, double zIn) {
        return new Vec3d(x + xIn, y + yIn, z + zIn);
    }

    public double distanceTo(Vec3d vec) {
        double dx = vec.x - x, dy = vec.y - y, dz = vec.z - z;
        return MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double squareDistanceTo(Vec3d vec) {
        double dx = vec.x - x, dy = vec.y - y, dz = vec.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double squareDistanceTo(double xIn, double yIn, double zIn) {
        double dx = xIn - x, dy = yIn - y, dz = zIn - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public Vec3d scale(double factor) {
        return new Vec3d(x * factor, y * factor, z * factor);
    }

    public double length() {
        return MathHelper.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    /** The point on the line to {@code vec} whose x is {@code x}, or null when it lies outside the segment. */
    public Vec3d getIntermediateWithXValue(Vec3d vec, double xv) {
        double dx = vec.x - x, dy = vec.y - y, dz = vec.z - z;
        if (dx * dx < 1.0000000116860974E-7D) return null;
        double d = (xv - x) / dx;
        return d >= 0.0D && d <= 1.0D ? new Vec3d(x + dx * d, y + dy * d, z + dz * d) : null;
    }

    public Vec3d getIntermediateWithYValue(Vec3d vec, double yv) {
        double dx = vec.x - x, dy = vec.y - y, dz = vec.z - z;
        if (dy * dy < 1.0000000116860974E-7D) return null;
        double d = (yv - y) / dy;
        return d >= 0.0D && d <= 1.0D ? new Vec3d(x + dx * d, y + dy * d, z + dz * d) : null;
    }

    public Vec3d getIntermediateWithZValue(Vec3d vec, double zv) {
        double dx = vec.x - x, dy = vec.y - y, dz = vec.z - z;
        if (dz * dz < 1.0000000116860974E-7D) return null;
        double d = (zv - z) / dz;
        return d >= 0.0D && d <= 1.0D ? new Vec3d(x + dx * d, y + dy * d, z + dz * d) : null;
    }

    public static Vec3d fromPitchYaw(Vec2f rotation) {
        return fromPitchYaw(rotation.x, rotation.y);
    }

    public static Vec3d fromPitchYaw(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    public Vec3d rotatePitch(float pitch) {
        float f = MathHelper.cos(pitch), f1 = MathHelper.sin(pitch);
        return new Vec3d(x, y * f + z * f1, z * f - y * f1);
    }

    public Vec3d rotateYaw(float yaw) {
        float f = MathHelper.cos(yaw), f1 = MathHelper.sin(yaw);
        return new Vec3d(x * f + z * f1, y, z * f - x * f1);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Vec3d v && Double.compare(v.x, x) == 0 && Double.compare(v.y, y) == 0 && Double.compare(v.z, z) == 0;
    }

    @Override
    public int hashCode() {
        long j = Double.doubleToLongBits(x);
        int i = (int) (j ^ j >>> 32);
        j = Double.doubleToLongBits(y);
        i = 31 * i + (int) (j ^ j >>> 32);
        j = Double.doubleToLongBits(z);
        return 31 * i + (int) (j ^ j >>> 32);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public net.minecraft.world.phys.Vec3 mcc$modern() {
        return new net.minecraft.world.phys.Vec3(x, y, z);
    }

    public static Vec3d mcc$of(net.minecraft.world.phys.Vec3 v) {
        return new Vec3d(v.x, v.y, v.z);
    }


    /** {@link #length} under the name of the stable 1.12 mappings. */
    public double lengthVector() {
        return length();
    }
}

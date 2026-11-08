package legacyconnector.v1_7_10.codechicken.lib.vec;

import legacyconnector.v1_7_10.net.minecraft.util.Vec3;

/** CodeChickenLib's double vector (the common arithmetic; mutating methods return this, as in the original). */
public class Vector3 implements Cloneable {
    public static final Vector3 zero = new Vector3();
    public static final Vector3 one = new Vector3(1, 1, 1);
    public static final Vector3 center = new Vector3(0.5, 0.5, 0.5);

    public double x;
    public double y;
    public double z;

    public Vector3() {
    }

    public Vector3(double d, double d1, double d2) {
        x = d;
        y = d1;
        z = d2;
    }

    public Vector3(Vector3 v) {
        this(v.x, v.y, v.z);
    }

    public Vector3(Vec3 v) {
        this(v.xCoord, v.yCoord, v.zCoord);
    }

    public static Vector3 fromBlockCoord(BlockCoord c) {
        return new Vector3(c.x, c.y, c.z);
    }

    public static Vector3 fromEntity(legacyconnector.v1_7_10.net.minecraft.entity.Entity e) {
        return new Vector3(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posX(e),
                legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posY(e),
                legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posZ(e));
    }

    public static Vector3 fromTileEntity(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity e) {
        return new Vector3(e.xCoord, e.yCoord, e.zCoord);
    }

    public static Vector3 fromTileEntityCenter(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity e) {
        return new Vector3(e.xCoord + 0.5, e.yCoord + 0.5, e.zCoord + 0.5);
    }

    public Vector3 copy() {
        return new Vector3(this);
    }

    public Vector3 set(double d, double d1, double d2) {
        x = d;
        y = d1;
        z = d2;
        return this;
    }

    public Vector3 set(Vector3 v) {
        return set(v.x, v.y, v.z);
    }

    public Vector3 add(double dx, double dy, double dz) {
        x += dx;
        y += dy;
        z += dz;
        return this;
    }

    public Vector3 add(Vector3 v) {
        return add(v.x, v.y, v.z);
    }

    public Vector3 add(double d) {
        return add(d, d, d);
    }

    public Vector3 subtract(Vector3 v) {
        return add(-v.x, -v.y, -v.z);
    }

    public Vector3 sub(Vector3 v) {
        return subtract(v);
    }

    public Vector3 multiply(double d) {
        x *= d;
        y *= d;
        z *= d;
        return this;
    }

    public Vector3 multiply(double fx, double fy, double fz) {
        x *= fx;
        y *= fy;
        z *= fz;
        return this;
    }

    public Vector3 multiply(Vector3 v) {
        return multiply(v.x, v.y, v.z);
    }

    public Vector3 negate() {
        return multiply(-1);
    }

    public double mag() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double magSquared() {
        return x * x + y * y + z * z;
    }

    public Vector3 normalize() {
        double d = mag();
        if (d != 0) multiply(1 / d);
        return this;
    }

    public double dotProduct(Vector3 v) {
        double d = v.x * x + v.y * y + v.z * z;
        if (d > 1 && d < 1.00001) d = 1;
        else if (d < -1 && d > -1.00001) d = -1;
        return d;
    }

    public Vector3 crossProduct(Vector3 v) {
        double nx = y * v.z - z * v.y;
        double ny = z * v.x - x * v.z;
        double nz = x * v.y - y * v.x;
        return set(nx, ny, nz);
    }

    public Vector3 perpendicular() {
        if (z == 0) return zCrossProduct();
        return xCrossProduct();
    }

    public Vector3 xCrossProduct() {
        double d = z;
        double d1 = -y;
        x = 0;
        y = d;
        z = d1;
        return this;
    }

    public Vector3 zCrossProduct() {
        double d = y;
        double d1 = -x;
        x = d;
        y = d1;
        z = 0;
        return this;
    }

    public double angle(Vector3 v) {
        return Math.acos(copy().normalize().dotProduct(v.copy().normalize()));
    }

    public Vector3 project(Vector3 v) {
        double d = v.magSquared();
        if (d == 0) return set(0, 0, 0);
        double f = dotProduct(v) / d;
        return set(v.x * f, v.y * f, v.z * f);
    }

    public Vector3 lerp(Vector3 to, double t) {
        return set(x + (to.x - x) * t, y + (to.y - y) * t, z + (to.z - z) * t);
    }

    public Vector3 floor() {
        return set(Math.floor(x), Math.floor(y), Math.floor(z));
    }

    public Vector3 ceil() {
        return set(Math.ceil(x), Math.ceil(y), Math.ceil(z));
    }

    /** Rotates around the axis (Rodrigues' formula). */
    public Vector3 rotate(double angle, Vector3 axis) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        Vector3 a = axis.copy().normalize();
        Vector3 cross = a.copy().crossProduct(this);
        double dot = a.dotProduct(this);
        return set(x * c + cross.x * s + a.x * dot * (1 - c), y * c + cross.y * s + a.y * dot * (1 - c), z * c + cross.z * s + a.z * dot * (1 - c));
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    public boolean isAxial() {
        return x == 0 ? (y == 0 || z == 0) : (y == 0 && z == 0);
    }

    public Vec3 toVec3D() {
        return Vec3.createVectorHelper(x, y, z);
    }

    public BlockCoord pos() {
        return new BlockCoord((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public double scalarProject(Vector3 b) {
        double l = b.mag();
        return l == 0 ? 0 : dotProduct(b) / l;
    }

    public Vector3 clone() {
        return copy();
    }

    public boolean equals(Object o) {
        return o instanceof Vector3 v && v.x == x && v.y == y && v.z == z;
    }

    public int hashCode() {
        long l = Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y);
        l = l * 31 + Double.doubleToLongBits(z);
        return (int) (l ^ (l >>> 32));
    }

    public String toString() {
        return "Vector3(" + x + ", " + y + ", " + z + ")";
    }
}

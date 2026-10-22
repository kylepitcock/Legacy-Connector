package legacyconnector.v1_7_10.net.minecraft.util;

public class Vec3 {
    public double xCoord;
    public double yCoord;
    public double zCoord;

    protected Vec3(double x, double y, double z) {
        if (x == -0.0D) x = 0.0D;
        if (y == -0.0D) y = 0.0D;
        if (z == -0.0D) z = 0.0D;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    public static Vec3 createVectorHelper(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    public Vec3 subtract(Vec3 o) {
        return createVectorHelper(o.xCoord - xCoord, o.yCoord - yCoord, o.zCoord - zCoord);
    }

    public Vec3 normalize() {
        double d = Math.sqrt(xCoord * xCoord + yCoord * yCoord + zCoord * zCoord);
        return d < 1.0E-4D ? createVectorHelper(0, 0, 0) : createVectorHelper(xCoord / d, yCoord / d, zCoord / d);
    }

    public double dotProduct(Vec3 o) {
        return xCoord * o.xCoord + yCoord * o.yCoord + zCoord * o.zCoord;
    }

    public Vec3 crossProduct(Vec3 o) {
        return createVectorHelper(yCoord * o.zCoord - zCoord * o.yCoord, zCoord * o.xCoord - xCoord * o.zCoord, xCoord * o.yCoord - yCoord * o.xCoord);
    }

    public Vec3 addVector(double x, double y, double z) {
        return createVectorHelper(xCoord + x, yCoord + y, zCoord + z);
    }

    public double distanceTo(Vec3 o) {
        return Math.sqrt(squareDistanceTo(o));
    }

    public double squareDistanceTo(Vec3 o) {
        double dx = o.xCoord - xCoord, dy = o.yCoord - yCoord, dz = o.zCoord - zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    public double squareDistanceTo(double x, double y, double z) {
        double dx = x - xCoord, dy = y - yCoord, dz = z - zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    public double lengthVector() {
        return Math.sqrt(xCoord * xCoord + yCoord * yCoord + zCoord * zCoord);
    }

    public void rotateAroundX(float angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double y = yCoord * c + zCoord * s, z = zCoord * c - yCoord * s;
        yCoord = y;
        zCoord = z;
    }

    public void rotateAroundY(float angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double x = xCoord * c + zCoord * s, z = zCoord * c - xCoord * s;
        xCoord = x;
        zCoord = z;
    }

    public void rotateAroundZ(float angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double x = xCoord * c + yCoord * s, y = yCoord * c - xCoord * s;
        xCoord = x;
        yCoord = y;
    }

    @Override
    public String toString() {
        return "(" + xCoord + ", " + yCoord + ", " + zCoord + ")";
    }

    public net.minecraft.world.phys.Vec3 mcc$modern() {
        return new net.minecraft.world.phys.Vec3(xCoord, yCoord, zCoord);
    }
}

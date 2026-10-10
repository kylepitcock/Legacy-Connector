package mccompat.v1_7_10.net.minecraft.util;

import net.minecraft.world.phys.AABB;

public class AxisAlignedBB {
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;

    protected AxisAlignedBB(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.minX = x1;
        this.minY = y1;
        this.minZ = z1;
        this.maxX = x2;
        this.maxY = y2;
        this.maxZ = z2;
    }

    public static AxisAlignedBB getBoundingBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
    }

    public AxisAlignedBB setBounds(double x1, double y1, double z1, double x2, double y2, double z2) {
        minX = x1;
        minY = y1;
        minZ = z1;
        maxX = x2;
        maxY = y2;
        maxZ = z2;
        return this;
    }

    public AxisAlignedBB addCoord(double x, double y, double z) {
        double a = minX, b = minY, c = minZ, d = maxX, e = maxY, f = maxZ;
        if (x < 0) a += x;
        if (x > 0) d += x;
        if (y < 0) b += y;
        if (y > 0) e += y;
        if (z < 0) c += z;
        if (z > 0) f += z;
        return getBoundingBox(a, b, c, d, e, f);
    }

    public AxisAlignedBB expand(double x, double y, double z) {
        return getBoundingBox(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z);
    }

    public AxisAlignedBB contract(double x, double y, double z) {
        return getBoundingBox(minX + x, minY + y, minZ + z, maxX - x, maxY - y, maxZ - z);
    }

    public AxisAlignedBB union(AxisAlignedBB o) {
        return getBoundingBox(Math.min(minX, o.minX), Math.min(minY, o.minY), Math.min(minZ, o.minZ),
                Math.max(maxX, o.maxX), Math.max(maxY, o.maxY), Math.max(maxZ, o.maxZ));
    }

    public AxisAlignedBB getOffsetBoundingBox(double x, double y, double z) {
        return getBoundingBox(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    public AxisAlignedBB offset(double x, double y, double z) {
        minX += x;
        minY += y;
        minZ += z;
        maxX += x;
        maxY += y;
        maxZ += z;
        return this;
    }

    public boolean intersectsWith(AxisAlignedBB o) {
        return o.maxX > minX && o.minX < maxX && o.maxY > minY && o.minY < maxY && o.maxZ > minZ && o.minZ < maxZ;
    }

    public double getAverageEdgeLength() {
        return ((maxX - minX) + (maxY - minY) + (maxZ - minZ)) / 3.0D;
    }

    public AxisAlignedBB copy() {
        return getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void setBB(AxisAlignedBB o) {
        setBounds(o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ);
    }

    @Override
    public String toString() {
        return "box[" + minX + ", " + minY + ", " + minZ + " -> " + maxX + ", " + maxY + ", " + maxZ + "]";
    }

    public AABB mcc$modern() {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static AxisAlignedBB mcc$of(AABB box) {
        return new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}

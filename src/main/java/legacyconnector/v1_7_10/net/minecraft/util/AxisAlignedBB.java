package legacyconnector.v1_7_10.net.minecraft.util;

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

    public boolean isVecInside(Vec3 v) {
        return v.xCoord > minX && v.xCoord < maxX && v.yCoord > minY && v.yCoord < maxY && v.zCoord > minZ && v.zCoord < maxZ;
    }

    private boolean inYZ(Vec3 v) {
        return v != null && v.yCoord >= minY && v.yCoord <= maxY && v.zCoord >= minZ && v.zCoord <= maxZ;
    }

    private boolean inXZ(Vec3 v) {
        return v != null && v.xCoord >= minX && v.xCoord <= maxX && v.zCoord >= minZ && v.zCoord <= maxZ;
    }

    private boolean inXY(Vec3 v) {
        return v != null && v.xCoord >= minX && v.xCoord <= maxX && v.yCoord >= minY && v.yCoord <= maxY;
    }

    /** The first face of this box the line from {@code from} to {@code to} crosses (side 0..5), or null. */
    public MovingObjectPosition calculateIntercept(Vec3 from, Vec3 to) {
        Vec3 west = from.getIntermediateWithXValue(to, minX);
        Vec3 east = from.getIntermediateWithXValue(to, maxX);
        Vec3 down = from.getIntermediateWithYValue(to, minY);
        Vec3 up = from.getIntermediateWithYValue(to, maxY);
        Vec3 north = from.getIntermediateWithZValue(to, minZ);
        Vec3 south = from.getIntermediateWithZValue(to, maxZ);
        if (!inYZ(west)) west = null;
        if (!inYZ(east)) east = null;
        if (!inXZ(down)) down = null;
        if (!inXZ(up)) up = null;
        if (!inXY(north)) north = null;
        if (!inXY(south)) south = null;
        Vec3[] hits = {down, up, north, south, west, east};
        int side = -1;
        for (int i = 0; i < hits.length; i++) {
            if (hits[i] != null && (side < 0 || from.squareDistanceTo(hits[i]) < from.squareDistanceTo(hits[side]))) side = i;
        }
        return side < 0 ? null : new MovingObjectPosition(0, 0, 0, side, hits[side]);
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

    /** How far this box can move along X before it hits {@code other}, given the wanted move (collision resolution). */
    public double calculateXOffset(AxisAlignedBB other, double dx) {
        if (other.maxY <= minY || other.minY >= maxY) return dx;
        if (other.maxZ <= minZ || other.minZ >= maxZ) return dx;
        if (dx > 0.0D && other.maxX <= minX) {
            double d = minX - other.maxX;
            if (d < dx) dx = d;
        }
        if (dx < 0.0D && other.minX >= maxX) {
            double d = maxX - other.minX;
            if (d > dx) dx = d;
        }
        return dx;
    }

    public double calculateYOffset(AxisAlignedBB other, double dy) {
        if (other.maxX <= minX || other.minX >= maxX) return dy;
        if (other.maxZ <= minZ || other.minZ >= maxZ) return dy;
        if (dy > 0.0D && other.maxY <= minY) {
            double d = minY - other.maxY;
            if (d < dy) dy = d;
        }
        if (dy < 0.0D && other.minY >= maxY) {
            double d = maxY - other.minY;
            if (d > dy) dy = d;
        }
        return dy;
    }

    public double calculateZOffset(AxisAlignedBB other, double dz) {
        if (other.maxX <= minX || other.minX >= maxX) return dz;
        if (other.maxY <= minY || other.minY >= maxY) return dz;
        if (dz > 0.0D && other.maxZ <= minZ) {
            double d = minZ - other.maxZ;
            if (d < dz) dz = d;
        }
        if (dz < 0.0D && other.minZ >= maxZ) {
            double d = maxZ - other.minZ;
            if (d > dz) dz = d;
        }
        return dz;
    }
}

package legacyconnector.v1_12_2.net.minecraft.util.math;

import net.minecraft.world.phys.AABB;

/** Immutable, as in 1.12.2. */
public class AxisAlignedBB {
    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public AxisAlignedBB(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public AxisAlignedBB(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    public AxisAlignedBB(BlockPos pos1, BlockPos pos2) {
        this(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public AxisAlignedBB setMaxY(double y2) {
        return new AxisAlignedBB(minX, minY, minZ, maxX, y2, maxZ);
    }

    public AxisAlignedBB contract(double x, double y, double z) {
        double nx1 = minX, ny1 = minY, nz1 = minZ, nx2 = maxX, ny2 = maxY, nz2 = maxZ;
        if (x < 0.0D) nx1 -= x; else if (x > 0.0D) nx2 -= x;
        if (y < 0.0D) ny1 -= y; else if (y > 0.0D) ny2 -= y;
        if (z < 0.0D) nz1 -= z; else if (z > 0.0D) nz2 -= z;
        return new AxisAlignedBB(nx1, ny1, nz1, nx2, ny2, nz2);
    }

    public AxisAlignedBB expand(double x, double y, double z) {
        double nx1 = minX, ny1 = minY, nz1 = minZ, nx2 = maxX, ny2 = maxY, nz2 = maxZ;
        if (x < 0.0D) nx1 += x; else if (x > 0.0D) nx2 += x;
        if (y < 0.0D) ny1 += y; else if (y > 0.0D) ny2 += y;
        if (z < 0.0D) nz1 += z; else if (z > 0.0D) nz2 += z;
        return new AxisAlignedBB(nx1, ny1, nz1, nx2, ny2, nz2);
    }

    public AxisAlignedBB grow(double x, double y, double z) {
        return new AxisAlignedBB(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z);
    }

    public AxisAlignedBB grow(double value) {
        return grow(value, value, value);
    }

    public AxisAlignedBB shrink(double value) {
        return grow(-value);
    }

    public AxisAlignedBB intersect(AxisAlignedBB o) {
        return new AxisAlignedBB(Math.max(minX, o.minX), Math.max(minY, o.minY), Math.max(minZ, o.minZ),
                Math.min(maxX, o.maxX), Math.min(maxY, o.maxY), Math.min(maxZ, o.maxZ));
    }

    public AxisAlignedBB union(AxisAlignedBB o) {
        return new AxisAlignedBB(Math.min(minX, o.minX), Math.min(minY, o.minY), Math.min(minZ, o.minZ),
                Math.max(maxX, o.maxX), Math.max(maxY, o.maxY), Math.max(maxZ, o.maxZ));
    }

    public AxisAlignedBB offset(double x, double y, double z) {
        return new AxisAlignedBB(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    public AxisAlignedBB offset(BlockPos pos) {
        return new AxisAlignedBB(minX + pos.getX(), minY + pos.getY(), minZ + pos.getZ(), maxX + pos.getX(), maxY + pos.getY(), maxZ + pos.getZ());
    }

    public boolean intersects(AxisAlignedBB o) {
        return intersects(o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ);
    }

    public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2) {
        return minX < x2 && maxX > x1 && minY < y2 && maxY > y1 && minZ < z2 && maxZ > z1;
    }

    public boolean contains(Vec3d vec) {
        return vec.x > minX && vec.x < maxX && vec.y > minY && vec.y < maxY && vec.z > minZ && vec.z < maxZ;
    }

    public double getAverageEdgeLength() {
        return ((maxX - minX) + (maxY - minY) + (maxZ - minZ)) / 3.0D;
    }

    public boolean hasNaN() {
        return Double.isNaN(minX) || Double.isNaN(minY) || Double.isNaN(minZ) || Double.isNaN(maxX) || Double.isNaN(maxY) || Double.isNaN(maxZ);
    }

    public Vec3d getCenter() {
        return new Vec3d(minX + (maxX - minX) * 0.5D, minY + (maxY - minY) * 0.5D, minZ + (maxZ - minZ) * 0.5D);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AxisAlignedBB b)) return false;
        return Double.compare(b.minX, minX) == 0 && Double.compare(b.minY, minY) == 0 && Double.compare(b.minZ, minZ) == 0
                && Double.compare(b.maxX, maxX) == 0 && Double.compare(b.maxY, maxY) == 0 && Double.compare(b.maxZ, maxZ) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
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

    // ---- 1.12.2 AxisAlignedBB, continued

    public AxisAlignedBB(Vec3d min, Vec3d max) {
        this(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public AxisAlignedBB offset(Vec3d vec) {
        return offset(vec.x, vec.y, vec.z);
    }

    /** How far {@code other} may move along x by {@code offsetX} before it touches this box (the collision clip). */
    public double calculateXOffset(AxisAlignedBB other, double offsetX) {
        if (other.maxY > minY && other.minY < maxY && other.maxZ > minZ && other.minZ < maxZ) {
            if (offsetX > 0.0D && other.maxX <= minX) {
                double d = minX - other.maxX;
                if (d < offsetX) offsetX = d;
            } else if (offsetX < 0.0D && other.minX >= maxX) {
                double d = maxX - other.minX;
                if (d > offsetX) offsetX = d;
            }
        }
        return offsetX;
    }

    public double calculateYOffset(AxisAlignedBB other, double offsetY) {
        if (other.maxX > minX && other.minX < maxX && other.maxZ > minZ && other.minZ < maxZ) {
            if (offsetY > 0.0D && other.maxY <= minY) {
                double d = minY - other.maxY;
                if (d < offsetY) offsetY = d;
            } else if (offsetY < 0.0D && other.minY >= maxY) {
                double d = maxY - other.minY;
                if (d > offsetY) offsetY = d;
            }
        }
        return offsetY;
    }

    public double calculateZOffset(AxisAlignedBB other, double offsetZ) {
        if (other.maxX > minX && other.minX < maxX && other.maxY > minY && other.minY < maxY) {
            if (offsetZ > 0.0D && other.maxZ <= minZ) {
                double d = minZ - other.maxZ;
                if (d < offsetZ) offsetZ = d;
            } else if (offsetZ < 0.0D && other.minZ >= maxZ) {
                double d = maxZ - other.minZ;
                if (d > offsetZ) offsetZ = d;
            }
        }
        return offsetZ;
    }

    /** Where the segment from {@code vecA} to {@code vecB} first enters this box, with the side it enters through; null if it misses. */
    public RayTraceResult calculateIntercept(Vec3d vecA, Vec3d vecB) {
        Vec3d hit = collideWithXPlane(minX, vecA, vecB);
        legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.WEST;
        Vec3d v = collideWithXPlane(maxX, vecA, vecB);
        if (v != null && isClosest(vecA, hit, v)) {
            hit = v;
            side = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.EAST;
        }
        v = collideWithYPlane(minY, vecA, vecB);
        if (v != null && isClosest(vecA, hit, v)) {
            hit = v;
            side = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.DOWN;
        }
        v = collideWithYPlane(maxY, vecA, vecB);
        if (v != null && isClosest(vecA, hit, v)) {
            hit = v;
            side = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP;
        }
        v = collideWithZPlane(minZ, vecA, vecB);
        if (v != null && isClosest(vecA, hit, v)) {
            hit = v;
            side = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH;
        }
        v = collideWithZPlane(maxZ, vecA, vecB);
        if (v != null && isClosest(vecA, hit, v)) {
            hit = v;
            side = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.SOUTH;
        }
        return hit == null ? null : new RayTraceResult(hit, side);
    }

    boolean isClosest(Vec3d from, Vec3d current, Vec3d candidate) {
        return current == null || from.squareDistanceTo(candidate) < from.squareDistanceTo(current);
    }

    Vec3d collideWithXPlane(double x, Vec3d a, Vec3d b) {
        Vec3d v = a.getIntermediateWithXValue(b, x);
        return v != null && intersectsWithYZ(v) ? v : null;
    }

    Vec3d collideWithYPlane(double y, Vec3d a, Vec3d b) {
        Vec3d v = a.getIntermediateWithYValue(b, y);
        return v != null && intersectsWithXZ(v) ? v : null;
    }

    Vec3d collideWithZPlane(double z, Vec3d a, Vec3d b) {
        Vec3d v = a.getIntermediateWithZValue(b, z);
        return v != null && intersectsWithXY(v) ? v : null;
    }

    public boolean intersectsWithYZ(Vec3d vec) {
        return vec.y >= minY && vec.y <= maxY && vec.z >= minZ && vec.z <= maxZ;
    }

    public boolean intersectsWithXZ(Vec3d vec) {
        return vec.x >= minX && vec.x <= maxX && vec.z >= minZ && vec.z <= maxZ;
    }

    public boolean intersectsWithXY(Vec3d vec) {
        return vec.x >= minX && vec.x <= maxX && vec.y >= minY && vec.y <= maxY;
    }
}

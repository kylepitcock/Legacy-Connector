package legacyconnector.v1_6_4.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;

/** 1.6.4 {@code AABBPool}: stateless here, boxes are plain allocations (see {@link Vec3Pool}). */
public class AABBPool {
    public static final AABBPool INSTANCE = new AABBPool(1024, 1024);

    public AABBPool(int truncateAfter, int maxPerTick) {}

    public AxisAlignedBB getAABB(double x1, double y1, double z1, double x2, double y2, double z2) {
        return AxisAlignedBB.getBoundingBox(x1, y1, z1, x2, y2, z2);
    }

    /** The 1.4.7 name of {@link #getAABB}. */
    public AxisAlignedBB addOrModifyAABBInPool(double x1, double y1, double z1, double x2, double y2, double z2) {
        return getAABB(x1, y1, z1, x2, y2, z2);
    }

    public void clear() {}

    public void cleanPool() {}

    public int getlistAABBsize() {
        return 0;
    }

    public int getnextPoolIndex() {
        return 0;
    }
}

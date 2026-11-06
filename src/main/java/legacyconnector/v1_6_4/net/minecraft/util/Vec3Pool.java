package legacyconnector.v1_6_4.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.util.Vec3;

/**
 * 1.6.4 {@code Vec3Pool}: a per-world pool of short-lived vectors. Vectors are plain allocations here (1.7.10 dropped the pool), so
 * the pool is stateless and one instance serves every world.
 */
public class Vec3Pool {
    public static final Vec3Pool INSTANCE = new Vec3Pool(1024, 1024);

    public Vec3Pool(int truncateAfter, int maxPerTick) {}

    public Vec3 getVecFromPool(double x, double y, double z) {
        return Vec3.createVectorHelper(x, y, z);
    }

    public void clear() {}

    public void clearAndFreeCache() {}

    public int getPoolSize() {
        return 0;
    }
}

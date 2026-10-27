package legacyconnector.v1_12_2.net.minecraft.util.math;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** A chunk's column coordinates (immutable), as 1.12.2's. */
public class ChunkPos {
    public final int x;
    public final int z;

    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public ChunkPos(BlockPos pos) {
        this.x = pos.getX() >> 4;
        this.z = pos.getZ() >> 4;
    }

    public static long asLong(int x, int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    @Override
    public int hashCode() {
        int i = 1664525 * x + 1013904223;
        int j = 1664525 * (z ^ -559038737) + 1013904223;
        return i ^ j;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ChunkPos c && c.x == x && c.z == z;
    }

    public double getDistanceSq(Entity entity) {
        double dx = (x * 16 + 8) - Entity.mcc$get$posX(entity);
        double dz = (z * 16 + 8) - Entity.mcc$get$posZ(entity);
        return dx * dx + dz * dz;
    }

    public int getXStart() {
        return x << 4;
    }

    public int getZStart() {
        return z << 4;
    }

    public int getXEnd() {
        return (x << 4) + 15;
    }

    public int getZEnd() {
        return (z << 4) + 15;
    }

    public BlockPos getBlock(int x, int y, int z) {
        return new BlockPos((this.x << 4) + x, y, (this.z << 4) + z);
    }

    public int getRegionCoordX() {
        return x >> 5;
    }

    public int getRegionCoordZ() {
        return z >> 5;
    }

    public int getRegionPositionX() {
        return x & 31;
    }

    public int getRegionPositionZ() {
        return z & 31;
    }

    @Override
    public String toString() {
        return "[" + x + ", " + z + "]";
    }

    public net.minecraft.world.level.ChunkPos mcc$modern() {
        return new net.minecraft.world.level.ChunkPos(x, z);
    }

    public static ChunkPos mcc$of(net.minecraft.world.level.ChunkPos pos) {
        return pos == null ? null : new ChunkPos(pos.x, pos.z);
    }
}

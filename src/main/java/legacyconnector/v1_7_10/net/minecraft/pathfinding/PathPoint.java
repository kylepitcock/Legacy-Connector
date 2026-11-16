package legacyconnector.v1_7_10.net.minecraft.pathfinding;

public class PathPoint {
    public final int xCoord;
    public final int yCoord;
    public final int zCoord;
    public int index = -1;
    public boolean isFirst;
    /** Pathfinding bookkeeping of a path search (see {@link Path}): cost from the start, estimated remainder, estimated total, the way back. */
    public float totalPathDistance;
    public float distanceToNext;
    public float distanceToTarget;
    public PathPoint previous;
    public final int hash;

    public PathPoint(int x, int y, int z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.hash = makeHash(x, y, z);
    }

    public static int makeHash(int x, int y, int z) {
        return y & 255 | (x & 32767) << 8 | (z & 32767) << 24 | (x < 0 ? Integer.MIN_VALUE : 0) | (z < 0 ? 32768 : 0);
    }

    public float distanceTo(PathPoint other) {
        float dx = other.xCoord - xCoord, dy = other.yCoord - yCoord, dz = other.zCoord - zCoord;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public float distanceToSquared(PathPoint other) {
        float dx = other.xCoord - xCoord, dy = other.yCoord - yCoord, dz = other.zCoord - zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    public boolean isAssigned() {
        return index >= 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PathPoint p && p.xCoord == xCoord && p.yCoord == yCoord && p.zCoord == zCoord;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return xCoord + ", " + yCoord + ", " + zCoord;
    }
}

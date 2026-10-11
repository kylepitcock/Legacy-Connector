package mccompat.v1_7_10.net.minecraft.pathfinding;

public class PathPoint {
    public final int xCoord;
    public final int yCoord;
    public final int zCoord;
    public int index = -1;
    public boolean isFirst;

    public PathPoint(int x, int y, int z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
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
        return xCoord & 255 | (yCoord & 255) << 8 | (zCoord & 255) << 16;
    }

    @Override
    public String toString() {
        return xCoord + ", " + yCoord + ", " + zCoord;
    }
}

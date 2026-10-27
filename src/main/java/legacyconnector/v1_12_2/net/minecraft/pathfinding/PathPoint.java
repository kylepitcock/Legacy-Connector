package legacyconnector.v1_12_2.net.minecraft.pathfinding;

/** One step of a path (a copy of the modern node). */
public class PathPoint {
    public final int x;
    public final int y;
    public final int z;
    public int index = -1;
    public float totalPathDistance;
    public float distanceToNext;
    public float distanceToTarget;
    public PathPoint previous;
    public boolean visited;
    public float distanceFromOrigin;
    public float cost;
    public float costMalus;
    public PathNodeType nodeType = PathNodeType.BLOCKED;

    public PathPoint(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static PathPoint mcc$of(net.minecraft.world.level.pathfinder.Node node) {
        if (node == null) return null;
        PathPoint p = new PathPoint(node.x, node.y, node.z);
        p.nodeType = PathNodeType.mcc$of(node.type);
        p.costMalus = node.costMalus;
        p.visited = node.closed;
        return p;
    }

    public PathPoint cloneMove(int x, int y, int z) {
        PathPoint p = new PathPoint(x, y, z);
        p.index = index;
        p.totalPathDistance = totalPathDistance;
        p.distanceToNext = distanceToNext;
        p.distanceToTarget = distanceToTarget;
        p.previous = previous;
        p.visited = visited;
        p.distanceFromOrigin = distanceFromOrigin;
        p.cost = cost;
        p.costMalus = costMalus;
        p.nodeType = nodeType;
        return p;
    }

    public static int makeHash(int x, int y, int z) {
        return y & 255 | (x & 32767) << 8 | (z & 32767) << 24 | (x < 0 ? Integer.MIN_VALUE : 0) | (z < 0 ? 32768 : 0);
    }

    public float distanceTo(PathPoint other) {
        float dx = other.x - x, dy = other.y - y, dz = other.z - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public float distanceToSquared(PathPoint other) {
        float dx = other.x - x, dy = other.y - y, dz = other.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public float distanceManhattan(PathPoint other) {
        return Math.abs(other.x - x) + Math.abs(other.y - y) + Math.abs(other.z - z);
    }

    public boolean isAssigned() {
        return index >= 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PathPoint p && p.x == x && p.y == y && p.z == z;
    }

    @Override
    public int hashCode() {
        return makeHash(x, y, z);
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}

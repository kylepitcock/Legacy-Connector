package legacyconnector.v1_7_10.net.minecraft.pathfinding;

/**
 * The open set of a path search (1.7.10 {@code net.minecraft.pathfinding.Path}): a binary min-heap of {@link PathPoint}s ordered by
 * {@link PathPoint#distanceToTarget}, each point remembering its heap slot in {@link PathPoint#index}. Mods write their own pathfinders on it.
 */
public class Path {
    private PathPoint[] pathPoints = new PathPoint[1024];
    private int count;

    public PathPoint addPoint(PathPoint point) {
        if (point.index >= 0) throw new IllegalStateException("OW KNOWS!");
        if (count == pathPoints.length) pathPoints = java.util.Arrays.copyOf(pathPoints, count << 1);
        pathPoints[count] = point;
        point.index = count;
        sortBack(count++);
        return point;
    }

    public void clearPath() {
        count = 0;
    }

    public PathPoint dequeue() {
        PathPoint first = pathPoints[0];
        pathPoints[0] = pathPoints[--count];
        pathPoints[count] = null;
        if (count > 0) sortForward(0);
        first.index = -1;
        return first;
    }

    public void changeDistance(PathPoint point, float distance) {
        float old = point.distanceToTarget;
        point.distanceToTarget = distance;
        if (distance < old) sortBack(point.index);
        else sortForward(point.index);
    }

    private void sortBack(int i) {
        PathPoint point = pathPoints[i];
        float cost = point.distanceToTarget;
        while (i > 0) {
            int parent = i - 1 >> 1;
            PathPoint other = pathPoints[parent];
            if (cost >= other.distanceToTarget) break;
            pathPoints[i] = other;
            other.index = i;
            i = parent;
        }
        pathPoints[i] = point;
        point.index = i;
    }

    private void sortForward(int i) {
        PathPoint point = pathPoints[i];
        float cost = point.distanceToTarget;
        while (true) {
            int left = 1 + (i << 1);
            int right = left + 1;
            if (left >= count) break;
            PathPoint a = pathPoints[left];
            float costA = a.distanceToTarget;
            PathPoint b;
            float costB;
            if (right >= count) {
                b = null;
                costB = Float.POSITIVE_INFINITY;
            } else {
                b = pathPoints[right];
                costB = b.distanceToTarget;
            }
            if (costA < costB) {
                if (costA >= cost) break;
                pathPoints[i] = a;
                a.index = i;
                i = left;
            } else {
                if (costB >= cost) break;
                pathPoints[i] = b;
                b.index = i;
                i = right;
            }
        }
        pathPoints[i] = point;
        point.index = i;
    }

    public boolean isPathEmpty() {
        return count == 0;
    }
}

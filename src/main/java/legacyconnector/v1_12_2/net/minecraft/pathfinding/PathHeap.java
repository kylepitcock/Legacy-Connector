package legacyconnector.v1_12_2.net.minecraft.pathfinding;

/** The 1.12 open set of the path finder: a binary heap of points ordered by {@code distanceToTarget}. */
public class PathHeap {
    private PathPoint[] pathPoints = new PathPoint[128];
    private int count;

    public PathHeap() {
    }

    public PathPoint addPoint(PathPoint point) {
        if (point.index >= 0) throw new IllegalStateException("OW KNOWS!");
        if (count == pathPoints.length) {
            PathPoint[] grown = new PathPoint[count << 1];
            System.arraycopy(pathPoints, 0, grown, 0, count);
            pathPoints = grown;
        }
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

    private void sortBack(int index) {
        PathPoint point = pathPoints[index];
        float f = point.distanceToTarget;
        while (index > 0) {
            int parent = index - 1 >> 1;
            PathPoint p = pathPoints[parent];
            if (!(f < p.distanceToTarget)) break;
            pathPoints[index] = p;
            p.index = index;
            index = parent;
        }
        pathPoints[index] = point;
        point.index = index;
    }

    private void sortForward(int index) {
        PathPoint point = pathPoints[index];
        float f = point.distanceToTarget;
        while (true) {
            int left = 1 + (index << 1);
            int right = left + 1;
            if (left >= count) break;
            PathPoint a = pathPoints[left];
            float fa = a.distanceToTarget;
            PathPoint b;
            float fb;
            if (right >= count) {
                b = null;
                fb = Float.POSITIVE_INFINITY;
            } else {
                b = pathPoints[right];
                fb = b.distanceToTarget;
            }
            if (fa < fb) {
                if (!(fa < f)) break;
                pathPoints[index] = a;
                a.index = index;
                index = left;
            } else {
                if (!(fb < f)) break;
                pathPoints[index] = b;
                b.index = index;
                index = right;
            }
        }
        pathPoints[index] = point;
        point.index = index;
    }

    public boolean isPathEmpty() {
        return count == 0;
    }
}

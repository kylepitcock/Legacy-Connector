package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import java.util.HashSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/**
 * The 1.12 A* path finder over a {@link NodeProcessor}. Navigators whose {@code getPathFinder} a mod overrides find their paths
 * with it; the result is handed to the mob's modern navigation to follow.
 */
public class PathFinder {
    private final PathHeap path = new PathHeap();
    private final Set<PathPoint> closedSet = new HashSet<>();
    private final PathPoint[] pathOptions = new PathPoint[32];
    private final NodeProcessor nodeProcessor;

    public PathFinder(NodeProcessor processor) {
        this.nodeProcessor = processor;
    }

    public Path findPath(IBlockAccess worldIn, EntityLiving entity, Entity target, float maxDistance) {
        return findPath(worldIn, entity, target.mcc$x(), target.getEntityBoundingBox().minY, target.mcc$z(), maxDistance);
    }

    public Path findPath(IBlockAccess worldIn, EntityLiving entity, BlockPos target, float maxDistance) {
        return findPath(worldIn, entity, target.getX() + 0.5F, target.getY() + 0.5F, target.getZ() + 0.5F, maxDistance);
    }

    private Path findPath(IBlockAccess worldIn, EntityLiving entity, double x, double y, double z, float maxDistance) {
        path.clearPath();
        nodeProcessor.init(worldIn, entity);
        try {
            PathPoint start = nodeProcessor.getStart();
            PathPoint end = nodeProcessor.getPathPointToCoords(x, y, z);
            return start == null || end == null ? null : findPath(start, end, maxDistance);
        } finally {
            nodeProcessor.postProcess();
        }
    }

    private Path findPath(PathPoint from, PathPoint to, float maxDistance) {
        from.totalPathDistance = 0.0F;
        from.distanceToNext = from.distanceManhattan(to);
        from.distanceToTarget = from.distanceToNext;
        path.clearPath();
        closedSet.clear();
        path.addPoint(from);
        PathPoint best = from;
        int steps = 0;
        while (!path.isPathEmpty()) {
            if (++steps >= 200) break;
            PathPoint current = path.dequeue();
            if (current.equals(to)) {
                best = to;
                break;
            }
            if (current.distanceManhattan(to) < best.distanceManhattan(to)) best = current;
            current.visited = true;
            int n = nodeProcessor.findPathOptions(pathOptions, current, to, maxDistance);
            for (int k = 0; k < n; k++) {
                PathPoint option = pathOptions[k];
                float d = current.distanceManhattan(option);
                option.distanceFromOrigin = current.distanceFromOrigin + d;
                option.cost = d + option.costMalus;
                float total = current.totalPathDistance + option.cost;
                if (option.distanceFromOrigin < maxDistance && (!option.isAssigned() || total < option.totalPathDistance)) {
                    option.previous = current;
                    option.totalPathDistance = total;
                    option.distanceToNext = option.distanceManhattan(to) + option.costMalus;
                    if (option.isAssigned()) {
                        path.changeDistance(option, option.totalPathDistance + option.distanceToNext);
                    } else {
                        option.distanceToTarget = option.totalPathDistance + option.distanceToNext;
                        path.addPoint(option);
                    }
                }
            }
        }
        return best == from ? null : createPath(best);
    }

    private static Path createPath(PathPoint end) {
        int n = 1;
        for (PathPoint p = end; p.previous != null; p = p.previous) n++;
        PathPoint[] points = new PathPoint[n];
        PathPoint p = end;
        points[--n] = end;
        while (p.previous != null) {
            p = p.previous;
            points[--n] = p;
        }
        return new Path(points);
    }
}

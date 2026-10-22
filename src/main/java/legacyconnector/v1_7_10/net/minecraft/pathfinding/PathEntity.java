package legacyconnector.v1_7_10.net.minecraft.pathfinding;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

/** A path: a view of a modern {@link Path}. */
public class PathEntity {
    private final Path path;

    public PathEntity(Path path) {
        this.path = path;
    }

    public PathEntity(PathPoint[] points) {
        java.util.List<Node> nodes = new java.util.ArrayList<>();
        for (PathPoint p : points) nodes.add(new Node(p.xCoord, p.yCoord, p.zCoord));
        PathPoint last = points.length == 0 ? new PathPoint(0, 0, 0) : points[points.length - 1];
        this.path = new Path(nodes, new net.minecraft.core.BlockPos(last.xCoord, last.yCoord, last.zCoord), true);
    }

    public Path mcc$modern() {
        return path;
    }

    private static PathPoint point(Node n) {
        return n == null ? null : new PathPoint(n.x, n.y, n.z);
    }

    public void incrementPathIndex() {
        path.advance();
    }

    public boolean isFinished() {
        return path.isDone();
    }

    public PathPoint getFinalPathPoint() {
        return point(path.getEndNode());
    }

    public PathPoint getPathPointFromIndex(int index) {
        return point(path.getNode(index));
    }

    public int getCurrentPathLength() {
        return path.getNodeCount();
    }

    public void setCurrentPathLength(int length) {
        path.truncateNodes(length);
    }

    public int getCurrentPathIndex() {
        return path.getNextNodeIndex();
    }

    public void setCurrentPathIndex(int index) {
        path.setNextNodeIndex(index);
    }

    public Vec3 getVectorFromIndex(Entity entity, int index) {
        var n = path.getNode(index);
        double half = (int) (entity.mcc$get$width(entity) + 1.0F) * 0.5;
        return Vec3.createVectorHelper(n.x + half, n.y, n.z + half);
    }

    public Vec3 getPosition(Entity entity) {
        return getVectorFromIndex(entity, getCurrentPathIndex());
    }

    public boolean isSamePath(PathEntity other) {
        return other != null && path.sameAs(other.path);
    }

    public boolean isDestinationSame(Vec3 v) {
        PathPoint end = getFinalPathPoint();
        return end != null && end.xCoord == (int) v.xCoord && end.zCoord == (int) v.zCoord;
    }
}

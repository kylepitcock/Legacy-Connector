package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** A path: a view of the modern path (or a list of points a mod built itself). */
public class Path {
    private final net.minecraft.world.level.pathfinder.Path mcc$path;
    private final PathPoint[] points;
    private int currentPathIndex;

    public Path(PathPoint[] points) {
        this.points = points;
        java.util.List<net.minecraft.world.level.pathfinder.Node> nodes = new java.util.ArrayList<>();
        for (PathPoint p : points) nodes.add(new net.minecraft.world.level.pathfinder.Node(p.x, p.y, p.z));
        PathPoint last = points.length == 0 ? null : points[points.length - 1];
        this.mcc$path = new net.minecraft.world.level.pathfinder.Path(nodes,
                last == null ? net.minecraft.core.BlockPos.ZERO : new net.minecraft.core.BlockPos(last.x, last.y, last.z), true);
    }

    private Path(net.minecraft.world.level.pathfinder.Path modern) {
        this.mcc$path = modern;
        this.points = null;
    }

    public static Path mcc$of(net.minecraft.world.level.pathfinder.Path modern) {
        return modern == null ? null : new Path(modern);
    }

    public net.minecraft.world.level.pathfinder.Path mcc$modern() {
        return mcc$path;
    }

    public void incrementPathIndex() {
        mcc$path.advance();
    }

    public boolean isFinished() {
        return mcc$path.isDone();
    }

    public PathPoint getFinalPathPoint() {
        return PathPoint.mcc$of(mcc$path.getEndNode());
    }

    public PathPoint getPathPointFromIndex(int index) {
        return PathPoint.mcc$of(mcc$path.getNode(index));
    }

    public void setPoint(int index, PathPoint point) {
        mcc$path.replaceNode(index, new net.minecraft.world.level.pathfinder.Node(point.x, point.y, point.z));
    }

    public int getCurrentPathLength() {
        return mcc$path.getNodeCount();
    }

    public void setCurrentPathLength(int length) {
        mcc$path.truncateNodes(length);
    }

    public int getCurrentPathIndex() {
        return mcc$path.getNextNodeIndex();
    }

    public void setCurrentPathIndex(int index) {
        mcc$path.setNextNodeIndex(index);
    }

    public Vec3d getVectorFromIndex(Entity entity, int index) {
        var node = mcc$path.getNode(index);
        double w = (int) (Entity.mcc$get$width(entity) + 1.0F) * 0.5D;
        return new Vec3d(node.x + w, node.y, node.z + w);
    }

    public Vec3d getPosition(Entity entity) {
        return getVectorFromIndex(entity, getCurrentPathIndex());
    }

    public Vec3d getCurrentPos() {
        var node = mcc$path.getNextNode();
        return new Vec3d(node.x, node.y, node.z);
    }

    public boolean isSamePath(Path other) {
        return other != null && mcc$path.sameAs(other.mcc$path);
    }

    public boolean isDestinationSame(Vec3d vec) {
        PathPoint p = getFinalPathPoint();
        return p != null && p.x == (int) vec.x && p.z == (int) vec.z;
    }
}

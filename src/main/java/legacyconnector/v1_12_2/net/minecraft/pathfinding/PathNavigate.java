package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

/**
 * A mob's navigation: a view of the modern path navigation of the mob (the modern mob ticks it). Mods' own subclasses keep working
 * through the same calls; their custom node processors are not used (the modern pathfinder finds the paths).
 */
public abstract class PathNavigate {
    protected EntityLiving entity;
    protected World world;
    protected Path currentPath;
    protected double speed;
    protected int totalTicks;
    protected NodeProcessor nodeProcessor;
    protected PathFinder pathFinder;
    protected int ticksAtLastPos;
    protected Vec3d lastPosCheck = Vec3d.ZERO;
    protected Vec3d timeoutCachedNode = Vec3d.ZERO;
    protected long timeoutTimer;
    protected long lastTimeoutCheck;
    protected double timeoutLimit;
    protected float maxDistanceToWaypoint = 0.5F;
    protected boolean tryUpdatePath;
    protected long lastTimeUpdated;
    protected BlockPos targetPos;

    public PathNavigate(EntityLiving entity, World world) {
        this.entity = entity;
        this.world = world;
        try {
            this.pathFinder = getPathFinder();
        } catch (RuntimeException | LinkageError e) {
            // a mod's path finder needing state its constructor sets later: the modern pathfinder finds the paths
        }
    }

    /** The 1.12 path finder (a walking one by default); a mod's override makes this navigator find its paths with it. */
    protected PathFinder getPathFinder() {
        if (nodeProcessor == null) {
            nodeProcessor = new WalkNodeProcessor();
            nodeProcessor.setCanEnterDoors(true);
        }
        return new PathFinder(nodeProcessor);
    }

    /** Whether a mod's own path finder finds this navigator's paths (it overrides {@code getPathFinder}). */
    protected final boolean mcc$customPathing() {
        return pathFinder != null && org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.overrides(getClass(), "getPathFinder");
    }

    private World mcc$world() {
        World w = entity == null ? null : Entity.mcc$get$world(entity);
        return w != null ? w : world;
    }

    /** A path found by the mod's own path finder, or null (no path, or the finder failed). */
    private Path mcc$findCustom(BlockPos pos, Entity target) {
        World w = mcc$world();
        if (w == null || !canNavigate()) return null;
        try {
            float range = getPathSearchRange();
            Path path = target != null ? pathFinder.findPath(w, entity, target, range) : pathFinder.findPath(w, entity, pos, range);
            if (path != null) targetPos = pos != null ? pos : new BlockPos(target);
            return path;
        } catch (RuntimeException | LinkageError e) {
            org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.failed(getClass(), "path finding", e);
            return null;
        }
    }

    protected final PathNavigation mcc$nav() {
        return entity != null && entity.mcc$entity instanceof Mob m ? m.getNavigation() : null;
    }

    /** Runs a mod's onUpdateNavigation override (the modern mob ticks the vanilla navigation itself). */
    public final void mcc$tickCustom() {
        try {
            if (org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.overrides(getClass(), "onUpdateNavigation")) onUpdateNavigation();
        } catch (RuntimeException | LinkageError e) {
            // a custom navigator reaching an API the layer lacks: the modern navigation still moves the mob
        }
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        var nav = mcc$nav();
        if (nav != null) nav.setSpeedModifier(speed);
    }

    public float getPathSearchRange() {
        return entity == null ? 16.0F : (float) entity.getEntityAttribute(legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
    }

    public boolean canUpdatePathOnTimeout() {
        return false;
    }

    public void updatePath() {
        var nav = mcc$nav();
        if (nav != null) nav.recomputePath();
    }

    public Path getPathToXYZ(double x, double y, double z) {
        return getPathToPos(new BlockPos(x, y, z));
    }

    public Path getPathToPos(BlockPos pos) {
        if (pos != null && mcc$customPathing()) return mcc$findCustom(pos, null);
        var nav = mcc$nav();
        return nav == null || pos == null ? null : Path.mcc$of(nav.createPath(pos.mcc$modern(), 1));
    }

    public Path getPathToEntityLiving(Entity target) {
        if (target != null && mcc$customPathing()) return mcc$findCustom(null, target);
        var nav = mcc$nav();
        return nav == null || target == null || target.mcc$entity == null ? null : Path.mcc$of(nav.createPath(target.mcc$entity, 1));
    }

    public boolean tryMoveToXYZ(double x, double y, double z, double speed) {
        if (mcc$customPathing()) return setPath(getPathToXYZ(x, y, z), speed);
        var nav = mcc$nav();
        this.speed = speed;
        return nav != null && nav.moveTo(x, y, z, speed);
    }

    public boolean tryMoveToEntityLiving(Entity target, double speed) {
        if (mcc$customPathing()) {
            Path path = getPathToEntityLiving(target);
            return path != null && setPath(path, speed);
        }
        var nav = mcc$nav();
        this.speed = speed;
        return nav != null && target != null && target.mcc$entity != null && nav.moveTo(target.mcc$entity, speed);
    }

    public boolean setPath(Path path, double speed) {
        var nav = mcc$nav();
        this.speed = speed;
        this.currentPath = path;
        return nav != null && nav.moveTo(path == null ? null : path.mcc$modern(), speed);
    }

    public Path getPath() {
        var nav = mcc$nav();
        return nav == null ? currentPath : Path.mcc$of(nav.getPath());
    }

    public void onUpdateNavigation() {
    }

    public boolean noPath() {
        var nav = mcc$nav();
        return nav == null || nav.isDone();
    }

    public void clearPath() {
        var nav = mcc$nav();
        if (nav != null) nav.stop();
        currentPath = null;
    }

    public boolean canEntityStandOnPos(BlockPos pos) {
        var nav = mcc$nav();
        return nav != null && nav.isStableDestination(pos.mcc$modern());
    }

    public BlockPos getTargetPos() {
        var nav = mcc$nav();
        var t = nav == null ? null : nav.getTargetPos();
        return t == null ? null : BlockPos.mcc$of(t);
    }

    protected boolean isInLiquid() {
        return entity != null && (entity.isInWater() || entity.isInLava());
    }

    protected boolean canNavigate() {
        return true;
    }

    public NodeProcessor getNodeProcessor() {
        return nodeProcessor;
    }

    /** Where the mob stands for path following (its feet). */
    protected Vec3d getEntityPosition() {
        return entity == null ? Vec3d.ZERO : new Vec3d(entity.mcc$x(), entity.mcc$y(), entity.mcc$z());
    }

    /** Whether the mob can walk straight between two points (subclasses check the blocks; none by default). */
    protected boolean isDirectPathBetweenPoints(Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ) {
        return false;
    }

    /** The 1.12 path following: skips reached and directly reachable points of the current path, then checks for being stuck. */
    protected void pathFollow() {
        Path path = getPath();
        if (path == null || entity == null) return;
        Vec3d pos = getEntityPosition();
        int end = path.getCurrentPathLength();
        for (int j = path.getCurrentPathIndex(); j < path.getCurrentPathLength(); ++j) {
            if (path.getPathPointFromIndex(j).y != Math.floor(pos.y)) {
                end = j;
                break;
            }
        }
        float width = Entity.mcc$get$width(entity);
        maxDistanceToWaypoint = width > 0.75F ? width / 2.0F : 0.75F - width / 2.0F;
        if (!path.isFinished()) {
            Vec3d current = path.getCurrentPos();
            if (Math.abs(entity.mcc$x() - (current.x + 0.5D)) < maxDistanceToWaypoint && Math.abs(entity.mcc$z() - (current.z + 0.5D)) < maxDistanceToWaypoint
                    && Math.abs(entity.mcc$y() - current.y) < 1.0D) {
                path.setCurrentPathIndex(path.getCurrentPathIndex() + 1);
            }
        }
        int sx = MathHelper.ceil(width);
        int sy = MathHelper.ceil(Entity.mcc$get$height(entity));
        for (int j = end - 1; j >= path.getCurrentPathIndex(); --j) {
            if (isDirectPathBetweenPoints(pos, path.getVectorFromIndex(entity, j), sx, sy, sx)) {
                path.setCurrentPathIndex(j);
                break;
            }
        }
        checkForStuck(pos);
    }

    /** Drops the path when the mob made no progress for 100 ticks, or took three times longer than expected to its next point. */
    protected void checkForStuck(Vec3d positionVec3) {
        if (totalTicks - ticksAtLastPos > 100) {
            if (positionVec3.squareDistanceTo(lastPosCheck) < 2.25D) clearPath();
            ticksAtLastPos = totalTicks;
            lastPosCheck = positionVec3;
        }
        Path path = getPath();
        if (path != null && !path.isFinished()) {
            Vec3d next = path.getCurrentPos();
            if (next.equals(timeoutCachedNode)) {
                timeoutTimer += System.currentTimeMillis() - lastTimeoutCheck;
            } else {
                timeoutCachedNode = next;
                double d = positionVec3.distanceTo(timeoutCachedNode);
                float aiSpeed = entity.getAIMoveSpeed();
                timeoutLimit = aiSpeed > 0.0F ? d / aiSpeed * 1000.0D : 0.0D;
            }
            if (timeoutLimit > 0.0D && timeoutTimer > timeoutLimit * 3.0D) {
                timeoutCachedNode = Vec3d.ZERO;
                timeoutTimer = 0L;
                timeoutLimit = 0.0D;
                clearPath();
            }
            lastTimeoutCheck = System.currentTimeMillis();
        }
    }

    /** Lifts path points that lie in a cauldron (subclasses also cut the path where it leaves the shade). */
    protected void removeSunnyPath() {
        Path path = getPath();
        World w = mcc$world();
        if (path == null || w == null) return;
        for (int i = 0; i < path.getCurrentPathLength(); ++i) {
            PathPoint p = path.getPathPointFromIndex(i);
            PathPoint next = i + 1 < path.getCurrentPathLength() ? path.getPathPointFromIndex(i + 1) : null;
            if (w.getBlockState(new BlockPos(p.x, p.y, p.z)).getBlock().mcc$modern instanceof net.minecraft.world.level.block.AbstractCauldronBlock) {
                path.setPoint(i, p.cloneMove(p.x, p.y + 1, p.z));
                if (next != null && p.y >= next.y) path.setPoint(i + 1, next.cloneMove(next.x, p.y + 1, next.z));
            }
        }
    }

    public void setCanSwim(boolean canSwim) {
        var nav = mcc$nav();
        if (nav != null) nav.setCanFloat(canSwim);
    }

    public boolean getCanSwim() {
        var nav = mcc$nav();
        return nav != null && nav.canFloat();
    }
}

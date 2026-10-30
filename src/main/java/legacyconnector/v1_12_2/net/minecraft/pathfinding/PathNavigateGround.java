package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Walking navigation (doors, sun avoidance, swimming) over the modern ground navigation. */
public class PathNavigateGround extends PathNavigate {
    protected boolean shouldAvoidSun;

    public PathNavigateGround(EntityLiving entity, World world) {
        super(entity, world);
    }

    @Override
    protected PathFinder getPathFinder() {
        nodeProcessor = new WalkNodeProcessor();
        nodeProcessor.setCanEnterDoors(true);
        return new PathFinder(nodeProcessor);
    }

    private net.minecraft.world.entity.ai.navigation.GroundPathNavigation mcc$ground() {
        return mcc$nav() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation g ? g : null;
    }

    private NodeProcessor mcc$processor() {
        if (nodeProcessor == null) getPathFinder();
        return nodeProcessor;
    }

    @Override
    protected boolean canNavigate() {
        return entity != null && (Entity.mcc$get$onGround(entity) || getCanSwim() && isInLiquid() || entity.isRiding());
    }

    @Override
    protected Vec3d getEntityPosition() {
        return new Vec3d(entity.mcc$x(), getPathablePosY(), entity.mcc$z());
    }

    /** The y the mob paths from: its feet, or the water surface while it swims. */
    private int getPathablePosY() {
        World w = Entity.mcc$get$world(entity);
        if (entity.isInWater() && getCanSwim() && w != null) {
            int y = (int) entity.getEntityBoundingBox().minY;
            int start = y;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(MathHelper.floor(entity.mcc$x()), y, MathHelper.floor(entity.mcc$z()));
            while (w.getBlockState(pos).getMaterial() == Material.WATER) {
                ++y;
                pos.setPos(MathHelper.floor(entity.mcc$x()), y, MathHelper.floor(entity.mcc$z()));
                if (y - start > 16) return (int) entity.getEntityBoundingBox().minY;
            }
            return y;
        }
        return (int) (entity.getEntityBoundingBox().minY + 0.5D);
    }

    @Override
    protected void removeSunnyPath() {
        super.removeSunnyPath();
        Path path = getPath();
        World w = Entity.mcc$get$world(entity);
        if (!shouldAvoidSun || path == null || w == null) return;
        if (w.canSeeSky(new BlockPos(MathHelper.floor(entity.mcc$x()), (int) (entity.getEntityBoundingBox().minY + 0.5D), MathHelper.floor(entity.mcc$z())))) return;
        for (int i = 0; i < path.getCurrentPathLength(); ++i) {
            PathPoint p = path.getPathPointFromIndex(i);
            if (w.canSeeSky(new BlockPos(p.x, p.y, p.z))) {
                path.setCurrentPathLength(i - 1);
                return;
            }
        }
    }

    @Override
    protected boolean isDirectPathBetweenPoints(Vec3d from, Vec3d to, int sizeX, int sizeY, int sizeZ) {
        int x = MathHelper.floor(from.x);
        int z = MathHelper.floor(from.z);
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double len2 = dx * dx + dz * dz;
        if (len2 < 1.0E-8D) return false;
        double inv = 1.0D / Math.sqrt(len2);
        dx *= inv;
        dz *= inv;
        if (!isSafeToStandAt(x, (int) from.y, z, sizeX + 2, sizeY, sizeZ + 2, from, dx, dz)) return false;
        double stepX = 1.0D / Math.abs(dx);
        double stepZ = 1.0D / Math.abs(dz);
        double nextX = x - from.x;
        double nextZ = z - from.z;
        if (dx >= 0.0D) ++nextX;
        if (dz >= 0.0D) ++nextZ;
        nextX /= dx;
        nextZ /= dz;
        int signX = dx < 0.0D ? -1 : 1;
        int signZ = dz < 0.0D ? -1 : 1;
        int endX = MathHelper.floor(to.x);
        int endZ = MathHelper.floor(to.z);
        int leftX = endX - x;
        int leftZ = endZ - z;
        int guard = 0;
        while ((leftX * signX > 0 || leftZ * signZ > 0) && guard++ < 1024) {
            if (nextX < nextZ) {
                nextX += stepX;
                x += signX;
                leftX = endX - x;
            } else {
                nextZ += stepZ;
                z += signZ;
                leftZ = endZ - z;
            }
            if (!isSafeToStandAt(x, (int) from.y, z, sizeX, sizeY, sizeZ, from, dx, dz)) return false;
        }
        return true;
    }

    private boolean isSafeToStandAt(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vec3d from, double dx, double dz) {
        int x0 = x - sizeX / 2;
        int z0 = z - sizeZ / 2;
        if (!isPositionClear(x0, y, z0, sizeX, sizeY, sizeZ, from, dx, dz)) return false;
        World w = Entity.mcc$get$world(entity);
        NodeProcessor processor = mcc$processor();
        for (int i = x0; i < x0 + sizeX; ++i) {
            for (int k = z0; k < z0 + sizeZ; ++k) {
                double ox = i + 0.5D - from.x;
                double oz = k + 0.5D - from.z;
                if (ox * dx + oz * dz < 0.0D) continue;
                PathNodeType below = processor.getPathNodeType(w, i, y - 1, k, entity, sizeX, sizeY, sizeZ, true, true);
                if (below == PathNodeType.WATER || below == PathNodeType.LAVA || below == PathNodeType.OPEN) return false;
                PathNodeType at = processor.getPathNodeType(w, i, y, k, entity, sizeX, sizeY, sizeZ, true, true);
                float malus = entity.getPathPriority(at);
                if (malus < 0.0F || malus >= 8.0F) return false;
                if (at == PathNodeType.DAMAGE_FIRE || at == PathNodeType.DANGER_FIRE || at == PathNodeType.DAMAGE_OTHER) return false;
            }
        }
        return true;
    }

    private boolean isPositionClear(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vec3d from, double dx, double dz) {
        World w = Entity.mcc$get$world(entity);
        if (w == null) return false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = x; i < x + sizeX; ++i) {
            for (int j = y; j < y + sizeY; ++j) {
                for (int k = z; k < z + sizeZ; ++k) {
                    double ox = i + 0.5D - from.x;
                    double oz = k + 0.5D - from.z;
                    if (ox * dx + oz * dz < 0.0D) continue;
                    pos.setPos(i, j, k);
                    if (!w.getBlockState(pos).getBlock().isPassable(w, pos)) return false;
                }
            }
        }
        return true;
    }

    @Override
    public NodeProcessor getNodeProcessor() {
        return mcc$processor();
    }

    public void setBreakDoors(boolean canBreakDoors) {
        mcc$processor().setCanOpenDoors(canBreakDoors);
        var g = mcc$ground();
        if (g != null) g.setCanOpenDoors(canBreakDoors);
    }

    public void setEnterDoors(boolean canEnterDoors) {
        mcc$processor().setCanEnterDoors(canEnterDoors);
        var g = mcc$ground();
        if (g != null) g.setCanPassDoors(canEnterDoors);
    }

    public boolean getEnterDoors() {
        return mcc$processor().getCanEnterDoors();
    }

    @Override
    public void setCanSwim(boolean canSwim) {
        mcc$processor().setCanSwim(canSwim);
        super.setCanSwim(canSwim);
    }

    @Override
    public boolean getCanSwim() {
        return mcc$processor().getCanSwim() || super.getCanSwim();
    }

    public void setAvoidSun(boolean avoidSun) {
        shouldAvoidSun = avoidSun;
        var g = mcc$ground();
        if (g != null) g.setAvoidSun(avoidSun);
    }
}

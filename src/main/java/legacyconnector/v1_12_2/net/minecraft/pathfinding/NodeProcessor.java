package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.IntHashMap;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.ChunkCache;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * The 1.12 node processor: what a {@link PathFinder} asks about the blocks around a mob (start point, neighbours, node types). The
 * vanilla processors classify blocks through the modern path types when the block view is backed by a level.
 */
public abstract class NodeProcessor {
    protected IBlockAccess blockaccess;
    protected EntityLiving entity;
    protected final IntHashMap<PathPoint> pointMap = new IntHashMap<>();
    protected int entitySizeX;
    protected int entitySizeY;
    protected int entitySizeZ;
    protected boolean canEnterDoors;
    protected boolean canOpenDoors;
    protected boolean canSwim;

    public void init(IBlockAccess sourceIn, EntityLiving mob) {
        this.blockaccess = sourceIn;
        this.entity = mob;
        this.pointMap.clearMap();
        this.entitySizeX = MathHelper.floor(Entity.mcc$get$width(mob) + 1.0F);
        this.entitySizeY = MathHelper.floor(Entity.mcc$get$height(mob) + 1.0F);
        this.entitySizeZ = MathHelper.floor(Entity.mcc$get$width(mob) + 1.0F);
    }

    public void postProcess() {
        this.blockaccess = null;
        this.entity = null;
    }

    protected PathPoint openPoint(int x, int y, int z) {
        int hash = PathPoint.makeHash(x, y, z);
        PathPoint point = pointMap.lookup(hash);
        if (point == null) {
            point = new PathPoint(x, y, z);
            pointMap.addKey(hash, point);
        }
        return point;
    }

    public abstract PathPoint getStart();

    public abstract PathPoint getPathPointToCoords(double x, double y, double z);

    public abstract int findPathOptions(PathPoint[] pathOptions, PathPoint currentPoint, PathPoint targetPoint, float maxDistance);

    public abstract PathNodeType getPathNodeType(IBlockAccess blockaccessIn, int x, int y, int z, EntityLiving entity, int xSize, int ySize, int zSize,
            boolean canBreakDoorsIn, boolean canEnterDoorsIn);

    public abstract PathNodeType getPathNodeType(IBlockAccess blockaccessIn, int x, int y, int z);

    public void setCanEnterDoors(boolean value) {
        canEnterDoors = value;
    }

    public void setCanOpenDoors(boolean value) {
        canOpenDoors = value;
    }

    public void setCanSwim(boolean value) {
        canSwim = value;
    }

    public boolean getCanEnterDoors() {
        return canEnterDoors;
    }

    public boolean getCanOpenDoors() {
        return canOpenDoors;
    }

    public boolean getCanSwim() {
        return canSwim;
    }

    /** The modern block view behind a legacy one (a world, a world's view, a chunk cache), or null for a mod's own view. */
    public static net.minecraft.world.level.CollisionGetter mcc$modernView(IBlockAccess access) {
        World world = access instanceof World w ? w : access instanceof ChunkCache c ? c.mcc$world() : null;
        return world == null ? null : world.mcc$access();
    }

    /**
     * The modern path type of a block (the modern classification covers modded blocks' own path types), or null when the view or the
     * mob has no modern counterpart.
     */
    public static PathNodeType mcc$modernRawType(IBlockAccess access, EntityLiving mob, int x, int y, int z) {
        var view = mcc$modernView(access);
        if (view == null || mob == null || !(mob.mcc$entity instanceof net.minecraft.world.entity.Mob m)) return null;
        try {
            return PathNodeType.mcc$of(new net.minecraft.world.level.pathfinder.PathfindingContext(view, m).getPathTypeFromState(x, y, z));
        } catch (RuntimeException e) {
            return null;
        }
    }
}

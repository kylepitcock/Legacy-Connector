package legacyconnector.v1_7_10.net.minecraft.pathfinding;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.PathType;

/** Legacy navigator: drives the modern navigation of the entity's host (ticked by the host itself). */
public class PathNavigate {
    private final EntityLiving theEntity;
    private double speed;
    private boolean avoidsWater;

    public PathNavigate(EntityLiving entity, World world) {
        this.theEntity = entity;
    }

    private Mob mob() {
        return theEntity.mcc$entity instanceof Mob m ? m : null;
    }

    private PathNavigation nav() {
        Mob m = mob();
        return m == null ? null : m.getNavigation();
    }

    public void setAvoidsWater(boolean avoid) {
        avoidsWater = avoid;
        Mob m = mob();
        if (m != null) m.setPathfindingMalus(PathType.WATER, avoid ? -1.0F : 8.0F);
    }

    public boolean getAvoidsWater() {
        return avoidsWater;
    }

    public void setBreakDoors(boolean b) {
        if (nav() instanceof GroundPathNavigation g) g.setCanOpenDoors(b);
    }

    public void setEnterDoors(boolean b) {
        if (nav() instanceof GroundPathNavigation g) g.setCanPassDoors(b);
    }

    public boolean getCanBreakDoors() {
        return nav() instanceof GroundPathNavigation g && g.canOpenDoors();
    }

    public void setAvoidSun(boolean b) {
        if (nav() instanceof GroundPathNavigation g) g.setAvoidSun(b);
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        PathNavigation n = nav();
        if (n != null) n.setSpeedModifier(speed);
    }

    public void setCanSwim(boolean b) {
        PathNavigation n = nav();
        if (n != null) n.setCanFloat(b);
    }

    public float getPathSearchRange() {
        Mob m = mob();
        return m == null ? 16.0F : (float) m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
    }

    public PathEntity getPathToXYZ(double x, double y, double z) {
        PathNavigation n = nav();
        var p = n == null ? null : n.createPath(net.minecraft.core.BlockPos.containing(x, y, z), 0);
        return p == null ? null : new PathEntity(p);
    }

    /**
     * Walks to the block itself (or, when it cannot be stood in, the reachable block closest to it), as 1.7.10's path finder
     * did. The modern {@code moveTo(x, y, z, speed)} stops a block short (accuracy 1): Thaumcraft's golems then halted next to
     * the item they were sent to pick up, out of their pick-up reach, and walked back and forth beside it forever.
     */
    public boolean tryMoveToXYZ(double x, double y, double z, double speed) {
        PathNavigation n = nav();
        return n != null && n.moveTo(x, y, z, 0, speed);
    }

    public PathEntity getPathToEntityLiving(Entity target) {
        PathNavigation n = nav();
        if (n == null || target == null || target.mcc$entity == null) return null;
        var p = n.createPath(target.mcc$entity, 0);
        return p == null ? null : new PathEntity(p);
    }

    /** To the target's own block, as {@link #tryMoveToXYZ} (the modern {@code moveTo(entity, speed)} stops a block short). */
    public boolean tryMoveToEntityLiving(Entity target, double speed) {
        PathNavigation n = nav();
        if (n == null || target == null || target.mcc$entity == null) return false;
        var p = n.createPath(target.mcc$entity, 0);
        return p != null && n.moveTo(p, speed);
    }

    public boolean setPath(PathEntity path, double speed) {
        PathNavigation n = nav();
        return n != null && n.moveTo(path == null ? null : path.mcc$modern(), speed);
    }

    public PathEntity getPath() {
        PathNavigation n = nav();
        var p = n == null ? null : n.getPath();
        return p == null ? null : new PathEntity(p);
    }

    /** Ticked by the host's vanilla AI step. */
    public void onUpdateNavigation() {
    }

    public boolean noPath() {
        PathNavigation n = nav();
        return n == null || n.isDone();
    }

    public void clearPathEntity() {
        PathNavigation n = nav();
        if (n != null) n.stop();
    }

    /** The position the path starts from: the entity's x and z at its pathable height (1.7.10's {@code getEntityPosition}). */
    protected legacyconnector.v1_7_10.net.minecraft.util.Vec3 getEntityPosition() {
        return legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(Entity.mcc$get$posX(theEntity), getPathableYPos(), Entity.mcc$get$posZ(theEntity));
    }

    /** The height paths start from: the feet's block, or the surface above when the entity swims in water it may swim in. */
    private int getPathableYPos() {
        World world = Entity.mcc$get$worldObj(theEntity);
        int feet = (int) (Entity.mcc$get$boundingBox(theEntity).minY + 0.5D);
        if (world == null || !theEntity.isInWater() || !nav().canFloat()) return feet;
        int x = (int) Math.floor(Entity.mcc$get$posX(theEntity));
        int z = (int) Math.floor(Entity.mcc$get$posZ(theEntity));
        int y = (int) Entity.mcc$get$boundingBox(theEntity).minY;
        var block = world.getBlock(x, y, z);
        int guard = 0;
        while (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.flowing_water || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.water) {
            block = world.getBlock(x, ++y, z);
            if (++guard > 16) return feet;
        }
        return y;
    }

    /** Whether the entity is in water or lava (1.7.10's {@code isInLiquid}). */
    protected boolean isInLiquid() {
        return theEntity.isInWater() || theEntity.handleLavaMovement();
    }

    /**
     * Whether the box of the given size, centred on the block, is clear and stands on ground that is neither air, lava nor water the entity
     * is not already in, looking only at the columns ahead of {@code start} along {@code (dx, dz)} (1.7.10's {@code isSafeToStandAt}).
     */
    protected boolean isSafeToStandAt(int x, int y, int z, int sizeX, int sizeY, int sizeZ, legacyconnector.v1_7_10.net.minecraft.util.Vec3 start, double dx, double dz) {
        int minX = x - sizeX / 2;
        int minZ = z - sizeZ / 2;
        if (!isPositionClear(minX, y, minZ, sizeX, sizeY, sizeZ, start, dx, dz)) return false;
        World world = Entity.mcc$get$worldObj(theEntity);
        for (int i = minX; i < minX + sizeX; i++) {
            for (int j = minZ; j < minZ + sizeZ; j++) {
                double px = i + 0.5D - start.xCoord;
                double pz = j + 0.5D - start.zCoord;
                if (px * dx + pz * dz < 0.0D) continue;
                var material = world.getBlock(i, y - 1, j).getMaterial();
                if (material == legacyconnector.v1_7_10.net.minecraft.block.material.Material.air) return false;
                if (material == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water && !theEntity.isInWater()) return false;
                if (material == legacyconnector.v1_7_10.net.minecraft.block.material.Material.lava) return false;
            }
        }
        return true;
    }

    /**
     * Whether the navigator can follow a path right now: the entity stands on the ground, or swims when it may,
     * or rides something (1.7.10's {@code canNavigate}).
     */
    public boolean canNavigate() {
        Mob m = mob();
        if (m == null) return false;
        return m.onGround() || m.isInWater() && nav() != null && nav().canFloat() || m.isPassenger();
    }

    /**
     * Whether the box of the given size at the position is free of blocking blocks, looking only at the columns ahead of
     * {@code start} along the direction {@code (dx, dz)} (1.7.10's {@code isPositionClear}).
     */
    public boolean isPositionClear(int x, int y, int z, int sizeX, int sizeY, int sizeZ, legacyconnector.v1_7_10.net.minecraft.util.Vec3 start, double dx, double dz) {
        World world = Entity.mcc$get$worldObj(theEntity);
        if (world == null) return false;
        for (int i = x; i < x + sizeX; i++) {
            for (int j = z; j < z + sizeZ; j++) {
                double px = i + 0.5D - start.xCoord;
                double pz = j + 0.5D - start.zCoord;
                if (px * dx + pz * dz < 0.0D) continue;
                for (int k = y; k < y + sizeY; k++) {
                    if (!world.getBlock(i, k, j).getBlocksMovement(world, i, k, j)) return false;
                }
            }
        }
        return true;
    }
}

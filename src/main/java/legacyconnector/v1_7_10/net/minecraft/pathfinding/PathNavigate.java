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
}

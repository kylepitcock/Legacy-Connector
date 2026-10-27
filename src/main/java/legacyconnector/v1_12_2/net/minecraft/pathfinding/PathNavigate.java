package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
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

    public PathNavigate(EntityLiving entity, World world) {
        this.entity = entity;
        this.world = world;
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
        var nav = mcc$nav();
        return nav == null || pos == null ? null : Path.mcc$of(nav.createPath(pos.mcc$modern(), 1));
    }

    public Path getPathToEntityLiving(Entity target) {
        var nav = mcc$nav();
        return nav == null || target == null || target.mcc$entity == null ? null : Path.mcc$of(nav.createPath(target.mcc$entity, 1));
    }

    public boolean tryMoveToXYZ(double x, double y, double z, double speed) {
        var nav = mcc$nav();
        this.speed = speed;
        return nav != null && nav.moveTo(x, y, z, speed);
    }

    public boolean tryMoveToEntityLiving(Entity target, double speed) {
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
        return null;
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

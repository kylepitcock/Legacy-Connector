package legacyconnector.v1_7_10.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.pathfinding.PathEntity;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;

/**
 * 1.7.10 EntityCreature: the old AI (find a target, path to it and attack it, otherwise wander to the best nearby
 * spot by getBlockPathWeight) on the host's navigation, and the home area (the host's restriction).
 */
public abstract class EntityCreature extends EntityLiving {
    private PathEntity pathToEntity;
    protected Entity entityToAttack;
    protected boolean hasAttacked;
    protected int fleeingTick;

    public EntityCreature(World world) {
        super(world);
    }

    protected EntityCreature(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    private PathfinderMob mcc$creature() {
        return mcc$entity instanceof PathfinderMob m ? m : null;
    }

    protected boolean isMovementCeased() {
        return false;
    }

    @Override
    protected void updateEntityActionState() {
        hasAttacked = isMovementCeased();
        if (entityToAttack == null) {
            entityToAttack = findPlayerToAttack();
            if (entityToAttack != null) pathToEntity = getNavigator().getPathToEntityLiving(entityToAttack);
        } else if (entityToAttack.isEntityAlive()) {
            float dist = entityToAttack.getDistanceToEntity(this);
            if (canEntityBeSeen(entityToAttack)) attackEntity(entityToAttack, dist);
        } else {
            entityToAttack = null;
        }
        if (fleeingTick > 0) --fleeingTick;
        if (hasAttacked) {
            getNavigator().clearPathEntity();
        } else if (entityToAttack != null && (getNavigator().noPath() || getRNG().nextInt(20) == 0)) {
            getNavigator().tryMoveToEntityLiving(entityToAttack, fleeingTick > 0 ? 1.4 : 1.0);
        } else if (entityToAttack == null && (getNavigator().noPath() && getRNG().nextInt(180) == 0 || getRNG().nextInt(120) == 0 || fleeingTick > 0)
                && entityAge < 100) {
            updateWanderPath();
        }
        if (entityToAttack != null) faceEntity(entityToAttack, 30.0F, 30.0F);
        if ((isInWater() || handleLavaMovement()) && getRNG().nextFloat() < 0.8F) setJumping(true);
    }

    /** Picks the best of ten random nearby spots (by getBlockPathWeight) and walks there. */
    protected void updateWanderPath() {
        boolean found = false;
        int bx = -1, by = -1, bz = -1;
        float best = -99999.0F;
        for (int i = 0; i < 10; i++) {
            int x = (int) Math.floor(mcc$x() + getRNG().nextInt(13) - 6);
            int y = (int) Math.floor(mcc$y() + getRNG().nextInt(7) - 3);
            int z = (int) Math.floor(mcc$z() + getRNG().nextInt(13) - 6);
            float w = getBlockPathWeight(x, y, z);
            if (w > best) {
                best = w;
                bx = x;
                by = y;
                bz = z;
                found = true;
            }
        }
        if (found) getNavigator().tryMoveToXYZ(bx, by, bz, fleeingTick > 0 ? 1.4 : 1.0);
    }

    protected void attackEntity(Entity target, float distance) {
    }

    public float getBlockPathWeight(int x, int y, int z) {
        return 0.0F;
    }

    protected Entity findPlayerToAttack() {
        return null;
    }

    @Override
    public boolean getCanSpawnHere() {
        return super.getCanSpawnHere() && getBlockPathWeight((int) Math.floor(mcc$x()), (int) Math.floor(mcc$y()), (int) Math.floor(mcc$z())) >= 0.0F;
    }

    public boolean hasPath() {
        return !getNavigator().noPath();
    }

    public void setPathToEntity(PathEntity path) {
        this.pathToEntity = path;
        getNavigator().setPath(path, 1.0);
    }

    public Entity getEntityToAttack() {
        return entityToAttack;
    }

    public void setTarget(Entity target) {
        this.entityToAttack = target;
    }

    public boolean isWithinHomeDistanceCurrentPosition() {
        PathfinderMob m = mcc$creature();
        return m == null || m.isWithinRestriction();
    }

    public boolean isWithinHomeDistance(int x, int y, int z) {
        PathfinderMob m = mcc$creature();
        return m == null || m.isWithinRestriction(new BlockPos(x, y, z));
    }

    public void setHomeArea(int x, int y, int z, int distance) {
        PathfinderMob m = mcc$creature();
        if (m != null) m.restrictTo(new BlockPos(x, y, z), distance);
    }

    public ChunkCoordinates getHomePosition() {
        PathfinderMob m = mcc$creature();
        BlockPos p = m == null ? BlockPos.ZERO : m.getRestrictCenter();
        return new ChunkCoordinates(p.getX(), p.getY(), p.getZ());
    }

    public float getMaximumHomeDistance() {
        PathfinderMob m = mcc$creature();
        return m == null ? -1.0F : m.getRestrictRadius();
    }

    public void detachHome() {
        PathfinderMob m = mcc$creature();
        if (m != null) m.clearRestriction();
    }

    public boolean hasHome() {
        PathfinderMob m = mcc$creature();
        return m != null && m.hasRestriction();
    }
}

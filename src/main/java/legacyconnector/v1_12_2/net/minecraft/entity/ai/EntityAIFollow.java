package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNavigate;
import legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNavigateFlying;
import legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNavigateGround;
import legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNodeType;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Follows a nearby visible mob of another class (parrots following mobs), keeping {@code stopDistance} away. */
public class EntityAIFollow extends EntityAIBase {
    private final EntityLiving entity;
    private final java.util.function.Predicate<EntityLiving> followPredicate;
    private EntityLiving followingEntity;
    private final double speedModifier;
    private final PathNavigate navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private float oldWaterCost;
    private final float areaSize;

    public EntityAIFollow(EntityLiving entity, double speed, float stopDistance, float areaSize) {
        this.entity = entity;
        this.followPredicate = e -> e != null && entity.getClass() != e.getClass();
        this.speedModifier = speed;
        this.navigation = entity.getNavigator();
        this.stopDistance = stopDistance;
        this.areaSize = areaSize;
        setMutexBits(3);
        if (!(navigation instanceof PathNavigateGround) && !(navigation instanceof PathNavigateFlying)) {
            throw new IllegalArgumentException("Unsupported mob type for FollowMobGoal");
        }
    }

    @Override
    public boolean shouldExecute() {
        var level = entity.mcc$entity == null ? null : entity.mcc$entity.level();
        if (level == null) return false;
        for (EntityLiving e : EntityQueries1122.within(level, entity.getEntityBoundingBox().grow(areaSize), EntityLiving.class, entity, followPredicate)) {
            if (!e.isInvisible()) {
                followingEntity = e;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return followingEntity != null && !navigation.noPath() && entity.getDistanceSq(followingEntity) > stopDistance * stopDistance;
    }

    @Override
    public void startExecuting() {
        timeToRecalcPath = 0;
        oldWaterCost = entity.getPathPriority(PathNodeType.WATER);
        entity.setPathPriority(PathNodeType.WATER, 0.0F);
    }

    @Override
    public void resetTask() {
        followingEntity = null;
        navigation.clearPath();
        entity.setPathPriority(PathNodeType.WATER, oldWaterCost);
    }

    @Override
    public void updateTask() {
        if (followingEntity == null || entity.getLeashed()) return;
        entity.getLookHelper().setLookPositionWithEntity(followingEntity, 10.0F, entity.getVerticalFaceSpeed());
        if (--timeToRecalcPath > 0) return;
        timeToRecalcPath = 10;
        double dx = entity.mcc$x() - followingEntity.mcc$x();
        double dy = entity.mcc$y() - followingEntity.mcc$y();
        double dz = entity.mcc$z() - followingEntity.mcc$z();
        double d = dx * dx + dy * dy + dz * dz;
        if (d > stopDistance * stopDistance) {
            navigation.tryMoveToEntityLiving(followingEntity, speedModifier);
        } else {
            navigation.clearPath();
            var look = followingEntity.getLookHelper();
            if (d <= stopDistance || look.getLookPosX() == entity.mcc$x() && look.getLookPosY() == entity.mcc$y() && look.getLookPosZ() == entity.mcc$z()) {
                double ax = followingEntity.mcc$x() - entity.mcc$x();
                double az = followingEntity.mcc$z() - entity.mcc$z();
                navigation.tryMoveToXYZ(entity.mcc$x() - ax, entity.mcc$y(), entity.mcc$z() - az, speedModifier);
            }
        }
    }
}

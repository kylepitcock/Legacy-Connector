package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Looks at the nearest entity of a class now and then (the attack target first). */
public class EntityAIWatchClosest extends EntityAIBase {
    protected EntityLiving entity;
    protected Entity closestEntity;
    protected float maxDistance;
    private int lookTime;
    private final float chance;
    protected Class<? extends Entity> watchedClass;

    public EntityAIWatchClosest(EntityLiving entity, Class<? extends Entity> watchTargetClass, float maxDistance) {
        this(entity, watchTargetClass, maxDistance, 0.02F);
    }

    public EntityAIWatchClosest(EntityLiving entity, Class<? extends Entity> watchTargetClass, float maxDistance, float chance) {
        this.entity = entity;
        this.watchedClass = watchTargetClass;
        this.maxDistance = maxDistance;
        this.chance = chance;
        setMutexBits(2);
    }

    @Override
    public boolean shouldExecute() {
        if (entity.getRNG().nextFloat() >= chance) return false;
        if (entity.getAttackTarget() != null) closestEntity = entity.getAttackTarget();
        var level = entity.mcc$entity == null ? null : entity.mcc$entity.level();
        if (level == null) return false;
        if (watchedClass == EntityPlayer.class) {
            closestEntity = EntityQueries1122.nearestPlayer(level, entity.mcc$x(), entity.mcc$y(), entity.mcc$z(), maxDistance, null);
        } else {
            closestEntity = EntityQueries1122.nearest(level, entity.getEntityBoundingBox().grow(maxDistance, 3.0D, maxDistance), watchedClass, entity, null);
        }
        return closestEntity != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (closestEntity == null || !closestEntity.isEntityAlive()) return false;
        if (entity.getDistanceSq(closestEntity) > maxDistance * maxDistance) return false;
        return lookTime > 0;
    }

    @Override
    public void startExecuting() {
        lookTime = 40 + entity.getRNG().nextInt(40);
    }

    @Override
    public void resetTask() {
        closestEntity = null;
    }

    @Override
    public void updateTask() {
        if (closestEntity == null) return;
        entity.getLookHelper().setLookPosition(closestEntity.mcc$x(), closestEntity.mcc$y() + closestEntity.getEyeHeight(), closestEntity.mcc$z(),
                entity.getHorizontalFaceSpeed(), entity.getVerticalFaceSpeed());
        --lookTime;
    }
}

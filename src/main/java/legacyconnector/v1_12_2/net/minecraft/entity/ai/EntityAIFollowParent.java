package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** A child follows the nearest adult of its class. */
public class EntityAIFollowParent extends EntityAIBase {
    EntityAnimal childAnimal;
    EntityAnimal parentAnimal;
    double moveSpeed;
    private int delayCounter;

    public EntityAIFollowParent(EntityAnimal animal, double speed) {
        this.childAnimal = animal;
        this.moveSpeed = speed;
    }

    @Override
    public boolean shouldExecute() {
        if (childAnimal.getGrowingAge() >= 0) return false;
        var level = childAnimal.mcc$entity == null ? null : childAnimal.mcc$entity.level();
        if (level == null) return false;
        EntityAnimal best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityAnimal a : EntityQueries1122.<EntityAnimal>within(level, childAnimal.getEntityBoundingBox().grow(8.0D, 4.0D, 8.0D), childAnimal.getClass(),
                childAnimal, null)) {
            if (a.getGrowingAge() < 0) continue;
            double d = childAnimal.getDistanceSq(a);
            if (d <= bestDist) {
                bestDist = d;
                best = a;
            }
        }
        if (best == null || bestDist < 9.0D) return false;
        parentAnimal = best;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (childAnimal.getGrowingAge() >= 0 || !parentAnimal.isEntityAlive()) return false;
        double d = childAnimal.getDistanceSq(parentAnimal);
        return d >= 9.0D && d <= 256.0D;
    }

    @Override
    public void startExecuting() {
        delayCounter = 0;
    }

    @Override
    public void resetTask() {
        parentAnimal = null;
    }

    @Override
    public void updateTask() {
        if (--delayCounter <= 0) {
            delayCounter = 10;
            childAnimal.getNavigator().tryMoveToEntityLiving(parentAnimal, moveSpeed);
        }
    }
}

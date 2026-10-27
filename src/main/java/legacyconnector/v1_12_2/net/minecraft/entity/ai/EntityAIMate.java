package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Finds a partner in love and breeds with it, spawning the child with createChild. */
public class EntityAIMate extends EntityAIBase {
    private final EntityAnimal animal;
    private final Class<? extends EntityAnimal> mateClass;
    legacyconnector.v1_12_2.net.minecraft.world.World world;
    private EntityAnimal targetMate;
    int spawnBabyDelay;
    double moveSpeed;

    public EntityAIMate(EntityAnimal animal, double speed) {
        this(animal, speed, animal.getClass());
    }

    public EntityAIMate(EntityAnimal animal, double speed, Class<? extends EntityAnimal> mateClass) {
        this.animal = animal;
        this.world = animal.mcc$world();
        this.mateClass = mateClass;
        this.moveSpeed = speed;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (!animal.isInLove()) return false;
        targetMate = getNearbyMate();
        return targetMate != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return targetMate.isEntityAlive() && targetMate.isInLove() && spawnBabyDelay < 60;
    }

    @Override
    public void resetTask() {
        targetMate = null;
        spawnBabyDelay = 0;
    }

    @Override
    public void updateTask() {
        animal.getLookHelper().setLookPositionWithEntity(targetMate, 10.0F, animal.getVerticalFaceSpeed());
        animal.getNavigator().tryMoveToEntityLiving(targetMate, moveSpeed);
        ++spawnBabyDelay;
        if (spawnBabyDelay >= 60 && animal.getDistanceSq(targetMate) < 9.0D) spawnBaby();
    }

    private EntityAnimal getNearbyMate() {
        var level = animal.mcc$entity == null ? null : animal.mcc$entity.level();
        if (level == null) return null;
        EntityAnimal best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityAnimal other : EntityQueries1122.<EntityAnimal>within(level, animal.getEntityBoundingBox().grow(8.0D), mateClass, animal, null)) {
            if (animal.canMateWith(other) && animal.getDistanceSq(other) < bestDist) {
                best = other;
                bestDist = animal.getDistanceSq(other);
            }
        }
        return best;
    }

    private void spawnBaby() {
        legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable child = animal.createChild(targetMate);
        if (child == null) return;
        var player = animal.getLoveCause();
        if (player == null && targetMate.getLoveCause() != null) player = targetMate.getLoveCause();
        animal.setGrowingAge(6000);
        targetMate.setGrowingAge(6000);
        animal.resetInLove();
        targetMate.resetInLove();
        child.setGrowingAge(-24000);
        child.setLocationAndAngles(animal.mcc$x(), animal.mcc$y(), animal.mcc$z(), 0.0F, 0.0F);
        org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.spawn(world, child);
        if (animal.mcc$entity != null && animal.mcc$entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.broadcastEntityEvent(animal.mcc$entity, (byte) 18);
            if (sl.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
                net.minecraft.world.entity.ExperienceOrb.award(sl, animal.mcc$entity.position(), animal.getRNG().nextInt(7) + 1);
            }
        }
    }
}

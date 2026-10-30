package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityIronGolem;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** A villager child walks up to an iron golem offering a poppy (by day) and takes it. */
public class EntityAIFollowGolem extends EntityAIBase {
    private final EntityVillager villager;
    private EntityIronGolem ironGolem;
    private int takeGolemRoseTick;
    private boolean tookGolemRose;

    public EntityAIFollowGolem(EntityVillager villager) {
        this.villager = villager;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (villager.getGrowingAge() >= 0) return false;
        var world = Entity.mcc$get$world(villager);
        if (world == null || !world.isDaytime()) return false;
        var level = villager.mcc$entity == null ? null : villager.mcc$entity.level();
        ironGolem = null;
        for (EntityIronGolem golem : EntityQueries1122.within(level, villager.getEntityBoundingBox().grow(6.0D, 2.0D, 6.0D), EntityIronGolem.class, villager, null)) {
            if (golem.getHoldRoseTick() > 0) {
                ironGolem = golem;
                break;
            }
        }
        return ironGolem != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return ironGolem != null && ironGolem.getHoldRoseTick() > 0;
    }

    @Override
    public void startExecuting() {
        takeGolemRoseTick = villager.getRNG().nextInt(320);
        tookGolemRose = false;
        ironGolem.getNavigator().clearPath();
    }

    @Override
    public void resetTask() {
        ironGolem = null;
        villager.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        villager.getLookHelper().setLookPositionWithEntity(ironGolem, 30.0F, 30.0F);
        if (ironGolem.getHoldRoseTick() == takeGolemRoseTick) {
            villager.getNavigator().tryMoveToEntityLiving(ironGolem, 0.5D);
            tookGolemRose = true;
        }
        if (tookGolemRose && villager.getDistanceSq(ironGolem) < 4.0D) {
            ironGolem.setHoldingRose(false);
            villager.getNavigator().clearPath();
        }
    }
}

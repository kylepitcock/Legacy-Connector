package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityIronGolem;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;

/** An iron golem now and then (by day, 1 in 8000 ticks) offers a flower to a villager within 6 blocks, looking at it for 20 seconds. */
public class EntityAILookAtVillager extends EntityAIBase {
    private final EntityIronGolem ironGolem;
    private EntityVillager villager;
    private int lookTime;

    public EntityAILookAtVillager(EntityIronGolem ironGolemIn) {
        this.ironGolem = ironGolemIn;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        var world = Entity.mcc$get$world(ironGolem);
        if (world == null || !world.isDaytime() || ironGolem.getRNG().nextInt(8000) != 0) return false;
        Entity found = world.findNearestEntityWithinAABB(EntityVillager.class, ironGolem.getEntityBoundingBox().grow(6.0D, 2.0D, 6.0D), ironGolem);
        villager = found instanceof EntityVillager v ? v : null;
        return villager != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return lookTime > 0;
    }

    @Override
    public void startExecuting() {
        lookTime = 400;
        ironGolem.setHoldingRose(true);
    }

    @Override
    public void resetTask() {
        ironGolem.setHoldingRose(false);
        villager = null;
    }

    @Override
    public void updateTask() {
        if (villager != null) ironGolem.getLookHelper().setLookPositionWithEntity(villager, 30.0F, 30.0F);
        --lookTime;
    }
}

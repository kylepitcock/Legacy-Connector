package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityTameable;

/** Tamed: stay put while ordered to sit. */
public class EntityAISit extends EntityAIBase {
    private final EntityTameable entity;
    private boolean isSitting;

    public EntityAISit(EntityTameable entity) {
        this.entity = entity;
        setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (!entity.isTamed() || entity.isInWater() || !entity.mcc$get$onGround(entity)) return false;
        return isSitting || entity.isSitting();
    }

    @Override
    public void startExecuting() {
        entity.getNavigator().clearPathEntity();
        entity.setSitting(true);
    }

    @Override
    public void resetTask() {
        entity.setSitting(false);
    }

    public void setSitting(boolean sitting) {
        this.isSitting = sitting;
    }
}

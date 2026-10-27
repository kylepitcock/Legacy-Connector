package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Keeps a tamed animal sitting while ordered to (stops its movement). */
public class EntityAISit extends EntityAIBase {
    private final EntityTameable tameable;
    private boolean isSitting;

    public EntityAISit(EntityTameable entity) {
        this.tameable = entity;
        setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (!tameable.isTamed()) return false;
        if (tameable.isInWater()) return false;
        if (!EntityTameable.mcc$get$onGround(tameable)) return false;
        EntityLivingBase owner = tameable.getOwner();
        if (owner == null) return true;
        return (!(tameable.getDistanceSq(owner) < 144.0D) || owner.getRevengeTarget() == null) && isSitting;
    }

    @Override
    public void startExecuting() {
        tameable.getNavigator().clearPath();
        tameable.setSitting(true);
    }

    @Override
    public void resetTask() {
        tameable.setSitting(false);
    }

    public void setSitting(boolean sitting) {
        this.isSitting = sitting;
    }
}

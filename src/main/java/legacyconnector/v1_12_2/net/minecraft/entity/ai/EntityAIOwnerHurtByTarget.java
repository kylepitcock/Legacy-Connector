package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Targets whatever hurt the owner. */
public class EntityAIOwnerHurtByTarget extends EntityAITarget {
    EntityTameable tameable;
    EntityLivingBase attacker;
    private int timestamp;

    public EntityAIOwnerHurtByTarget(EntityTameable tameable) {
        super(tameable, false);
        this.tameable = tameable;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!tameable.isTamed()) return false;
        EntityLivingBase owner = tameable.getOwner();
        if (owner == null) return false;
        attacker = owner.getRevengeTarget();
        int i = owner.getRevengeTimer();
        return i != timestamp && isSuitableTarget(attacker, false) && tameable.shouldAttackEntity(attacker, owner);
    }

    @Override
    public void startExecuting() {
        taskOwner.setAttackTarget(attacker);
        EntityLivingBase owner = tameable.getOwner();
        if (owner != null) timestamp = owner.getRevengeTimer();
        super.startExecuting();
    }
}

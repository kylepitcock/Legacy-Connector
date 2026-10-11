package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.entity.passive.EntityTameable;

/** Tamed: attack whatever attacked the owner. */
public class EntityAIOwnerHurtByTarget extends EntityAITarget {
    private final EntityTameable tameable;
    private EntityLivingBase attacker;
    private int timer;

    public EntityAIOwnerHurtByTarget(EntityTameable owner) {
        super(owner, false);
        this.tameable = owner;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!tameable.isTamed() || tameable.isSitting()) return false;
        EntityLivingBase owner = tameable.getOwner();
        if (owner == null) return false;
        attacker = owner.getAITarget();
        return owner.getRevengeTimer() != timer && isSuitableTarget(attacker, false) && tameable.func_142018_a(attacker, owner);
    }

    @Override
    public void startExecuting() {
        taskOwner.setAttackTarget(attacker);
        EntityLivingBase owner = tameable.getOwner();
        if (owner != null) timer = owner.getRevengeTimer();
        super.startExecuting();
    }
}

package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.entity.passive.EntityTameable;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;

/** Tamed: attack whatever the owner attacked. */
public class EntityAIOwnerHurtTarget extends EntityAITarget {
    private final EntityTameable tameable;
    private EntityLivingBase target;
    private int timer;

    public EntityAIOwnerHurtTarget(EntityTameable owner) {
        super(owner, false);
        this.tameable = owner;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!tameable.isTamed() || tameable.isSitting()) return false;
        EntityLivingBase owner = tameable.getOwner();
        if (owner == null || !(owner.mcc$entity instanceof LivingEntity o) || o.getLastHurtMob() == null) return false;
        target = (EntityLivingBase) EntityBridge.wrap(o.getLastHurtMob());
        return o.getLastHurtMobTimestamp() != timer && isSuitableTarget(target, false) && tameable.func_142018_a(target, owner);
    }

    @Override
    public void startExecuting() {
        taskOwner.setAttackTarget(target);
        EntityLivingBase owner = tameable.getOwner();
        if (owner != null && owner.mcc$entity instanceof LivingEntity o) timer = o.getLastHurtMobTimestamp();
        super.startExecuting();
    }
}

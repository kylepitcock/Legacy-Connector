package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.IEntityOwnable;
import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** Base of legacy targeting tasks: keeps the attack target while it stays valid, visible and in follow range. */
public abstract class EntityAITarget extends EntityAIBase {
    protected EntityCreature taskOwner;
    protected boolean shouldCheckSight;
    private final boolean nearbyOnly;
    private int unseenTicks;

    public EntityAITarget(EntityCreature owner, boolean checkSight) {
        this(owner, checkSight, false);
    }

    public EntityAITarget(EntityCreature owner, boolean checkSight, boolean nearbyOnly) {
        this.taskOwner = owner;
        this.shouldCheckSight = checkSight;
        this.nearbyOnly = nearbyOnly;
    }

    @Override
    public boolean continueExecuting() {
        EntityLivingBase target = taskOwner.getAttackTarget();
        if (target == null || !target.isEntityAlive()) return false;
        double range = getTargetDistance();
        if (taskOwner.getDistanceSqToEntity(target) > range * range) return false;
        if (shouldCheckSight) {
            if (taskOwner.getEntitySenses().canSee(target)) unseenTicks = 0;
            else if (++unseenTicks > 60) return false;
        }
        return !(target instanceof EntityPlayer p && legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$get$disableDamage(p.capabilities));
    }

    protected double getTargetDistance() {
        var a = taskOwner.getEntityAttribute(SharedMonsterAttributes.followRange);
        return a == null ? 16.0 : a.getAttributeValue();
    }

    @Override
    public void startExecuting() {
        unseenTicks = 0;
    }

    @Override
    public void resetTask() {
        taskOwner.setAttackTarget(null);
    }

    protected boolean isSuitableTarget(EntityLivingBase target, boolean ignoreDisabledDamage) {
        if (target == null || target == taskOwner || !target.isEntityAlive()) return false;
        if (!taskOwner.canAttackClass(target.getClass())) return false;
        if (taskOwner instanceof IEntityOwnable owned && owned.func_152113_b() != null && !owned.func_152113_b().isEmpty()) {
            if (target instanceof IEntityOwnable other && owned.func_152113_b().equals(other.func_152113_b())) return false;
            if (target == owned.getOwner()) return false;
        }
        if (target instanceof EntityPlayer p && !ignoreDisabledDamage && legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$get$disableDamage(p.capabilities)) return false;
        if (!taskOwner.isWithinHomeDistance((int) Math.floor(target.mcc$x()), (int) Math.floor(target.mcc$y()), (int) Math.floor(target.mcc$z()))) return false;
        return !shouldCheckSight || taskOwner.getEntitySenses().canSee(target);
    }
}

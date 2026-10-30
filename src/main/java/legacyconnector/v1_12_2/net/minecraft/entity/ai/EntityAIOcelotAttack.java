package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Chases the attack target (sprinting when close) and strikes once a second within reach. */
public class EntityAIOcelotAttack extends EntityAIBase {
    World world;
    EntityLiving entity;
    EntityLivingBase target;
    int attackCountdown;

    public EntityAIOcelotAttack(EntityLiving entity) {
        this.entity = entity;
        this.world = Entity.mcc$get$world(entity);
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase t = entity.getAttackTarget();
        if (t == null) return false;
        target = t;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (target == null || !target.isEntityAlive()) return false;
        if (entity.getDistanceSq(target) > 225.0D) return false;
        return !entity.getNavigator().noPath() || shouldExecute();
    }

    @Override
    public void resetTask() {
        target = null;
        entity.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        float width = Entity.mcc$get$width(entity);
        double reach = width * 2.0F * width * 2.0F;
        double dist = entity.getDistanceSq(target.mcc$x(), target.getEntityBoundingBox().minY, target.mcc$z());
        double speed = 0.8D;
        if (dist > reach && dist < 16.0D) speed = 1.33D;
        else if (dist < 225.0D) speed = 0.6D;
        entity.getNavigator().tryMoveToEntityLiving(target, speed);
        attackCountdown = Math.max(attackCountdown - 1, 0);
        if (dist <= reach && attackCountdown <= 0) {
            attackCountdown = 20;
            entity.attackEntityAsMob(target);
        }
    }
}

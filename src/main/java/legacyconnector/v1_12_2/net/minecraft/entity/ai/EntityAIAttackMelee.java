package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Chases the attack target along a path and hits it when in reach (1.12's melee task). */
public class EntityAIAttackMelee extends EntityAIBase {
    protected EntityCreature attacker;
    protected int attackTick;
    double speedTowardsTarget;
    boolean longMemory;
    legacyconnector.v1_12_2.net.minecraft.pathfinding.Path path;
    private int delayCounter;
    private double targetX;
    private double targetY;
    private double targetZ;
    protected final int attackInterval = 20;

    public EntityAIAttackMelee(EntityCreature creature, double speedIn, boolean useLongMemory) {
        this.attacker = creature;
        this.speedTowardsTarget = speedIn;
        this.longMemory = useLongMemory;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = attacker.getAttackTarget();
        if (target == null || !target.isEntityAlive()) return false;
        path = attacker.getNavigator().getPathToEntityLiving(target);
        if (path != null) return true;
        return getAttackReachSqr(target) >= attacker.getDistanceSq(target.mcc$x(), target.getEntityBoundingBox().minY, target.mcc$z());
    }

    @Override
    public boolean shouldContinueExecuting() {
        EntityLivingBase target = attacker.getAttackTarget();
        if (target == null || !target.isEntityAlive()) return false;
        if (!longMemory) return !attacker.getNavigator().noPath();
        if (!attacker.isWithinHomeDistanceFromPosition(new BlockPos(target.mcc$x(), target.mcc$y(), target.mcc$z()))) return false;
        return !(target instanceof EntityPlayer p && (p.isSpectator() || p.isCreative()));
    }

    @Override
    public void startExecuting() {
        attacker.getNavigator().setPath(path, speedTowardsTarget);
        delayCounter = 0;
    }

    @Override
    public void resetTask() {
        EntityLivingBase target = attacker.getAttackTarget();
        if (target instanceof EntityPlayer p && (p.isSpectator() || p.isCreative())) attacker.setAttackTarget(null);
        attacker.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        EntityLivingBase target = attacker.getAttackTarget();
        if (target == null) return;
        attacker.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        double d0 = attacker.getDistanceSq(target.mcc$x(), target.getEntityBoundingBox().minY, target.mcc$z());
        --delayCounter;
        if ((longMemory || attacker.getEntitySenses().canSee(target)) && delayCounter <= 0
                && (targetX == 0.0D && targetY == 0.0D && targetZ == 0.0D || target.getDistanceSq(targetX, targetY, targetZ) >= 1.0D
                        || attacker.getRNG().nextFloat() < 0.05F)) {
            targetX = target.mcc$x();
            targetY = target.getEntityBoundingBox().minY;
            targetZ = target.mcc$z();
            delayCounter = 4 + attacker.getRNG().nextInt(7);
            if (d0 > 1024.0D) delayCounter += 10;
            else if (d0 > 256.0D) delayCounter += 5;
            if (!attacker.getNavigator().tryMoveToEntityLiving(target, speedTowardsTarget)) delayCounter += 15;
        }
        attackTick = Math.max(attackTick - 1, 0);
        checkAndPerformAttack(target, d0);
    }

    protected void checkAndPerformAttack(EntityLivingBase enemy, double distToEnemySqr) {
        double d0 = getAttackReachSqr(enemy);
        if (distToEnemySqr <= d0 && attackTick <= 0) {
            attackTick = 20;
            attacker.swingArm(legacyconnector.v1_12_2.net.minecraft.util.EnumHand.MAIN_HAND);
            attacker.attackEntityAsMob(enemy);
        }
    }

    protected double getAttackReachSqr(EntityLivingBase attackTarget) {
        float w = EntityCreature.mcc$get$width(attacker);
        return w * 2.0F * w * 2.0F + EntityLivingBase.mcc$get$width(attackTarget);
    }
}

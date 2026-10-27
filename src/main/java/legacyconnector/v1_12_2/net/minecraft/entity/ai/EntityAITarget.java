package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Base of the 1.12 targeting tasks: keeps the attack target while it stays valid, visible and within follow range. */
public abstract class EntityAITarget extends EntityAIBase {
    protected final EntityCreature taskOwner;
    protected boolean shouldCheckSight;
    private final boolean nearbyOnly;
    private int targetSearchStatus;
    private int targetSearchDelay;
    private int targetUnseenTicks;
    protected EntityLivingBase target;
    protected int unseenMemoryTicks = 60;

    public EntityAITarget(EntityCreature creature, boolean checkSight) {
        this(creature, checkSight, false);
    }

    public EntityAITarget(EntityCreature creature, boolean checkSight, boolean onlyNearby) {
        this.taskOwner = creature;
        this.shouldCheckSight = checkSight;
        this.nearbyOnly = onlyNearby;
    }

    @Override
    public boolean shouldContinueExecuting() {
        EntityLivingBase t = taskOwner.getAttackTarget();
        if (t == null) t = target;
        if (t == null || !t.isEntityAlive()) return false;
        if (taskOwner.isOnSameTeam(t)) return false;
        double d = getTargetDistance();
        if (taskOwner.getDistanceSq(t) > d * d) return false;
        if (shouldCheckSight) {
            if (taskOwner.getEntitySenses().canSee(t)) targetUnseenTicks = 0;
            else if (++targetUnseenTicks > unseenMemoryTicks) return false;
        }
        if (t instanceof EntityPlayer p && legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities.mcc$get$disableDamage(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.mcc$get$capabilities(p))) return false;
        taskOwner.setAttackTarget(t);
        return true;
    }

    protected double getTargetDistance() {
        var a = taskOwner.getEntityAttribute(legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes.FOLLOW_RANGE);
        return a == null ? 16.0D : a.getAttributeValue();
    }

    @Override
    public void startExecuting() {
        targetSearchStatus = 0;
        targetSearchDelay = 0;
        targetUnseenTicks = 0;
    }

    @Override
    public void resetTask() {
        taskOwner.setAttackTarget(null);
        target = null;
    }

    public static boolean isSuitableTarget(EntityLiving attacker, EntityLivingBase target, boolean includeInvincibles, boolean checkSight) {
        if (target == null || target == attacker || !target.isEntityAlive()) return false;
        if (!attacker.canAttackClass(target.getClass())) return false;
        if (attacker.isOnSameTeam(target)) return false;
        if (attacker instanceof legacyconnector.v1_12_2.net.minecraft.entity.IEntityOwnable owned && owned.getOwnerId() != null) {
            if (target instanceof legacyconnector.v1_12_2.net.minecraft.entity.IEntityOwnable other && owned.getOwnerId().equals(other.getOwnerId())) return false;
            if (target == owned.getOwner()) return false;
        } else if (target instanceof EntityPlayer p && !includeInvincibles
                && legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities.mcc$get$disableDamage(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.mcc$get$capabilities(p))) {
            return false;
        }
        return !checkSight || attacker.getEntitySenses().canSee(target);
    }

    protected boolean isSuitableTarget(EntityLivingBase target, boolean includeInvincibles) {
        if (!isSuitableTarget(taskOwner, target, includeInvincibles, shouldCheckSight)) return false;
        if (!taskOwner.isWithinHomeDistanceFromPosition(new BlockPos(target.mcc$x(), target.mcc$y(), target.mcc$z()))) return false;
        if (nearbyOnly) {
            if (--targetSearchDelay <= 0) targetSearchStatus = 0;
            if (targetSearchStatus == 0) targetSearchStatus = canEasilyReach(target) ? 1 : 2;
            if (targetSearchStatus == 2) return false;
        }
        return true;
    }

    private boolean canEasilyReach(EntityLivingBase target) {
        targetSearchDelay = 10 + taskOwner.getRNG().nextInt(5);
        var path = taskOwner.getNavigator().getPathToEntityLiving(target);
        if (path == null) return false;
        var point = path.getFinalPathPoint();
        if (point == null) return false;
        int i = point.x - (int) Math.floor(target.mcc$x());
        int j = point.z - (int) Math.floor(target.mcc$z());
        return i * i + j * j <= 2.25D;
    }

    public EntityAITarget setUnseenMemoryTicks(int ticks) {
        this.unseenMemoryTicks = ticks;
        return this;
    }
}

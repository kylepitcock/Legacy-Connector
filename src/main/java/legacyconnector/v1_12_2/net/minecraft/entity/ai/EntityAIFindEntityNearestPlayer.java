package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Targets the nearest player in follow range (mobs without the creature AI, like slimes). */
public class EntityAIFindEntityNearestPlayer extends EntityAIBase {
    private final EntityLiving entityLiving;
    private EntityLivingBase entityTarget;

    public EntityAIFindEntityNearestPlayer(EntityLiving entity) {
        this.entityLiving = entity;
    }

    @Override
    public boolean shouldExecute() {
        var level = entityLiving.mcc$entity == null ? null : entityLiving.mcc$entity.level();
        if (level == null) return false;
        double d = maxTargetRange();
        entityTarget = EntityQueries1122.nearestPlayer(level, entityLiving.mcc$x(), entityLiving.mcc$y(), entityLiving.mcc$z(), d,
                p -> !p.isCreative() && !p.isSpectator() && !legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities.mcc$get$disableDamage(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.mcc$get$capabilities(p)));
        return entityTarget != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        EntityLivingBase t = entityLiving.getAttackTarget();
        if (t == null || !t.isEntityAlive()) return false;
        if (t instanceof EntityPlayer p && (p.isCreative() || p.isSpectator())) return false;
        double d = maxTargetRange();
        return entityLiving.getDistanceSq(t) <= d * d;
    }

    @Override
    public void startExecuting() {
        entityLiving.setAttackTarget(entityTarget);
    }

    @Override
    public void resetTask() {
        entityLiving.setAttackTarget(null);
    }

    protected double maxTargetRange() {
        var a = entityLiving.getEntityAttribute(legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes.FOLLOW_RANGE);
        return a == null ? 16.0D : a.getAttributeValue();
    }
}

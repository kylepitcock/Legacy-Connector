package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Targets the nearest suitable entity of a class in follow range (for mobs without the creature AI). */
public class EntityAIFindEntityNearest extends EntityAIBase {
    private final EntityLiving mob;
    private final Class<? extends EntityLivingBase> classToCheck;
    private final EntityAINearestAttackableTarget.Sorter sorter;
    private EntityLivingBase target;

    public EntityAIFindEntityNearest(EntityLiving mob, Class<? extends EntityLivingBase> classToCheck) {
        this.mob = mob;
        this.classToCheck = classToCheck;
        this.sorter = new EntityAINearestAttackableTarget.Sorter(mob);
    }

    protected double getFollowRange() {
        var a = mob.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        return a == null ? 16.0D : a.getAttributeValue();
    }

    private boolean suitable(EntityLivingBase e) {
        double range = getFollowRange();
        if (e.isSneaking()) range *= 0.8D;
        if (e.isInvisible()) range *= 0.7D;
        return e.getDistance(mob) <= range && EntityAITarget.isSuitableTarget(mob, e, false, true);
    }

    @Override
    public boolean shouldExecute() {
        var level = mob.mcc$entity == null ? null : mob.mcc$entity.level();
        if (level == null) return false;
        double range = getFollowRange();
        @SuppressWarnings("unchecked")
        Class<EntityLivingBase> cls = (Class<EntityLivingBase>) classToCheck;
        java.util.List<EntityLivingBase> list = EntityQueries1122.within(level, mob.getEntityBoundingBox().grow(range, 4.0D, range), cls, mob, this::suitable);
        if (list.isEmpty()) return false;
        list.sort(sorter);
        target = list.get(0);
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        EntityLivingBase t = mob.getAttackTarget();
        if (t == null || !t.isEntityAlive()) return false;
        double range = getFollowRange();
        if (mob.getDistanceSq(t) > range * range) return false;
        return !(t instanceof EntityPlayer p) || !p.isCreative() && !p.isSpectator();
    }

    @Override
    public void startExecuting() {
        mob.setAttackTarget(target);
        super.startExecuting();
    }

    @Override
    public void resetTask() {
        mob.setAttackTarget(null);
        super.resetTask();
    }
}

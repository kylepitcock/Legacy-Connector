package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Targets whatever last hurt the owner, optionally calling nearby mobs of its class for help. */
public class EntityAIHurtByTarget extends EntityAITarget {
    private final boolean entityCallsForHelp;
    private int revengeTimerOld;
    private final Class<?>[] excludedReinforcementTypes;

    public EntityAIHurtByTarget(EntityCreature creature, boolean entityCallsForHelp, Class<?>... excludedReinforcementTypes) {
        super(creature, true);
        this.entityCallsForHelp = entityCallsForHelp;
        this.excludedReinforcementTypes = excludedReinforcementTypes;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        int i = taskOwner.getRevengeTimer();
        EntityLivingBase revenge = taskOwner.getRevengeTarget();
        return i != revengeTimerOld && revenge != null && isSuitableTarget(revenge, false);
    }

    @Override
    public void startExecuting() {
        taskOwner.setAttackTarget(taskOwner.getRevengeTarget());
        target = taskOwner.getAttackTarget();
        revengeTimerOld = taskOwner.getRevengeTimer();
        unseenMemoryTicks = 300;
        if (entityCallsForHelp) alertOthers();
        super.startExecuting();
    }

    protected void alertOthers() {
        double d = getTargetDistance();
        var level = taskOwner.mcc$entity == null ? null : taskOwner.mcc$entity.level();
        EntityLivingBase revenge = taskOwner.getRevengeTarget();
        if (level == null || revenge == null) return;
        var box = new legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB(taskOwner.mcc$x(), taskOwner.mcc$y(), taskOwner.mcc$z(),
                taskOwner.mcc$x() + 1.0D, taskOwner.mcc$y() + 1.0D, taskOwner.mcc$z() + 1.0D).grow(d, 10.0D, d);
        for (EntityCreature c : EntityQueries1122.<EntityCreature>within(level, box, taskOwner.getClass(), taskOwner, null)) {
            if (c == taskOwner || c.getAttackTarget() != null || c.isOnSameTeam(revenge)) continue;
            if (taskOwner instanceof legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable t
                    && (!(c instanceof legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable ct) || t.getOwner() != ct.getOwner())) {
                continue;
            }
            boolean excluded = false;
            if (excludedReinforcementTypes != null) for (Class<?> cls : excludedReinforcementTypes) if (c.getClass() == cls) excluded = true;
            if (!excluded) setEntityAttackTarget(c, revenge);
        }
    }

    protected void setEntityAttackTarget(EntityCreature creature, EntityLivingBase target) {
        creature.setAttackTarget(target);
    }
}

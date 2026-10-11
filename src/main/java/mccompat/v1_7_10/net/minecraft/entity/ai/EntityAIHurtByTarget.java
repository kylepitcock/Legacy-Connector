package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityCreature;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;

/** Targets whatever last hurt the entity; optionally alerts nearby entities of the same class. */
public class EntityAIHurtByTarget extends EntityAITarget {
    private final boolean entityCallsForHelp;
    private int revengeTimer;

    public EntityAIHurtByTarget(EntityCreature owner, boolean callsForHelp) {
        super(owner, false);
        this.entityCallsForHelp = callsForHelp;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        return taskOwner.getRevengeTimer() != revengeTimer && isSuitableTarget(taskOwner.getAITarget(), false);
    }

    @Override
    public void startExecuting() {
        EntityLivingBase attacker = taskOwner.getAITarget();
        taskOwner.setAttackTarget(attacker);
        revengeTimer = taskOwner.getRevengeTimer();
        if (entityCallsForHelp && taskOwner.mcc$entity instanceof LivingEntity self && attacker != null) {
            double range = getTargetDistance();
            for (LivingEntity other : self.level().getEntitiesOfClass(LivingEntity.class, self.getBoundingBox().inflate(range, 10.0, range))) {
                var legacy = EntityBridge.wrap(other);
                if (other != self && legacy instanceof EntityCreature c && c.getClass() == taskOwner.getClass() && c.getAttackTarget() == null
                        && !c.isOnSameTeam(attacker)) {
                    c.setAttackTarget(attacker);
                }
            }
        }
        super.startExecuting();
    }
}

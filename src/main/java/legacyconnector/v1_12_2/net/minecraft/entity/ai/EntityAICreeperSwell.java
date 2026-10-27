package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityCreeper;

/** Swells a creeper near its target (verbatim 1.12: within 3 blocks starts, beyond 7 or out of sight stops). */
public class EntityAICreeperSwell extends EntityAIBase {
    private final EntityCreeper swellingCreeper;
    private EntityLivingBase creeperAttackTarget;

    public EntityAICreeperSwell(EntityCreeper creeper) {
        this.swellingCreeper = creeper;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = swellingCreeper.getAttackTarget();
        return swellingCreeper.getCreeperState() > 0 || target != null && swellingCreeper.getDistanceSq(target) < 9.0D;
    }

    @Override
    public void startExecuting() {
        swellingCreeper.getNavigator().clearPath();
        creeperAttackTarget = swellingCreeper.getAttackTarget();
    }

    @Override
    public void resetTask() {
        creeperAttackTarget = null;
    }

    @Override
    public void updateTask() {
        if (creeperAttackTarget == null || swellingCreeper.getDistanceSq(creeperAttackTarget) > 49.0D
                || !swellingCreeper.getEntitySenses().canSee(creeperAttackTarget)) {
            swellingCreeper.setCreeperState(-1);
        } else {
            swellingCreeper.setCreeperState(1);
        }
    }
}

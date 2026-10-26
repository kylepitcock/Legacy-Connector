package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityCreeper;

/** Lights the creeper's fuse while its target is within 3 blocks and visible, and puts it out once the target is 7 blocks away. */
public class EntityAICreeperSwell extends EntityAIBase {
    protected EntityCreeper swellingCreeper;
    protected EntityLivingBase creeperAttackTarget;

    public EntityAICreeperSwell(EntityCreeper creeper) {
        this.swellingCreeper = creeper;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = swellingCreeper.getAttackTarget();
        return swellingCreeper.getCreeperState() > 0 || target != null && swellingCreeper.getDistanceSqToEntity(target) < 9.0D;
    }

    @Override
    public void startExecuting() {
        swellingCreeper.getNavigator().clearPathEntity();
        creeperAttackTarget = swellingCreeper.getAttackTarget();
    }

    @Override
    public void resetTask() {
        creeperAttackTarget = null;
    }

    @Override
    public void updateTask() {
        if (creeperAttackTarget == null || swellingCreeper.getDistanceSqToEntity(creeperAttackTarget) > 49.0D
                || !swellingCreeper.getEntitySenses().canSee(creeperAttackTarget)) {
            swellingCreeper.setCreeperState(-1);
        } else {
            swellingCreeper.setCreeperState(1);
        }
    }
}

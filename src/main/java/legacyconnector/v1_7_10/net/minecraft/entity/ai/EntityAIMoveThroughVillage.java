package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;

/** Walk through a village's doors. The layer reports no villages, so this task never starts. */
public class EntityAIMoveThroughVillage extends EntityAIBase {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIMoveThroughVillage(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e, float speed, boolean n) {
        this(mcc$creature(e), (double) speed, n);
    }

    public EntityAIMoveThroughVillage(EntityCreature entity, double speed, boolean nightOnly) {
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

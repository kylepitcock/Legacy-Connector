package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;

/** Go home to a village door at night. The layer reports no villages, so this task never starts. */
public class EntityAIMoveIndoors extends EntityAIBase {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIMoveIndoors(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e) {
        this(mcc$creature(e));
    }

    public EntityAIMoveIndoors(EntityCreature entity) {
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

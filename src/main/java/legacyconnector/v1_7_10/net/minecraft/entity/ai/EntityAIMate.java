package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAnimal;

/** Breeding needs a vanilla animal host; legacy mod animals do not breed yet. */
public class EntityAIMate extends EntityAIBase {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIMate(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAnimal a, float speed) {
        this(a, (double) speed);
    }

    public EntityAIMate(EntityAnimal animal, double speed) {
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

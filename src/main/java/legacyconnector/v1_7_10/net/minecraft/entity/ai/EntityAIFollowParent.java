package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAnimal;

public class EntityAIFollowParent extends EntityAIBase {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIFollowParent(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAnimal a, float speed) {
        this(a, (double) speed);
    }

    public EntityAIFollowParent(EntityAnimal animal, double speed) {
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

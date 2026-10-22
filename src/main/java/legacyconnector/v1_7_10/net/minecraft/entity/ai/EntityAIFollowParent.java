package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAnimal;

public class EntityAIFollowParent extends EntityAIBase {
    public EntityAIFollowParent(EntityAnimal animal, double speed) {
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

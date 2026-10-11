package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.passive.EntityAnimal;

/** Breeding needs a vanilla animal host; legacy mod animals do not breed yet. */
public class EntityAIMate extends EntityAIBase {
    public EntityAIMate(EntityAnimal animal, double speed) {
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

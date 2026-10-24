package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;

/** Go home to a village door at night. The layer reports no villages, so this task never starts. */
public class EntityAIMoveIndoors extends EntityAIBase {
    public EntityAIMoveIndoors(EntityCreature entity) {
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

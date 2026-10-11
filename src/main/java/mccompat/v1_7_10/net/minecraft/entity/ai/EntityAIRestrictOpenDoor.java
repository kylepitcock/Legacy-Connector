package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityCreature;

/** Villagers: keep doors restricted at night. No modern counterpart for non-villagers; never runs. */
public class EntityAIRestrictOpenDoor extends EntityAIBase {
    public EntityAIRestrictOpenDoor(EntityCreature entity) {
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

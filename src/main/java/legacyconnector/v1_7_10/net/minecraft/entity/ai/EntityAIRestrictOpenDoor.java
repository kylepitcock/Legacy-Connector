package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;

/** Villagers: keep doors restricted at night. No modern counterpart for non-villagers; never runs. */
public class EntityAIRestrictOpenDoor extends EntityAIBase {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIRestrictOpenDoor(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e) {
        this(mcc$creature(e));
    }

    public EntityAIRestrictOpenDoor(EntityCreature entity) {
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

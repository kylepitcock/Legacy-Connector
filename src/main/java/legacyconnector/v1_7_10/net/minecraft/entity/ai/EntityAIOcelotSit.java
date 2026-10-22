package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot;

/** Vanilla ocelots sitting on chests/beds; kept so the ocelot task list has its vanilla shape. */
public class EntityAIOcelotSit extends EntityAIBase {
    public EntityAIOcelotSit(EntityOcelot ocelot, double speed) {
        setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

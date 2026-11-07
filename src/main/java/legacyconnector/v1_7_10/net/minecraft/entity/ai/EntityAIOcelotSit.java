package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot;

/** Vanilla ocelots sitting on chests/beds; kept so the ocelot task list has its vanilla shape. */
public class EntityAIOcelotSit extends EntityAIBase {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIOcelotSit(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot o, float speed) {
        this(o, (double) speed);
    }

    public EntityAIOcelotSit(EntityOcelot ocelot, double speed) {
        setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}

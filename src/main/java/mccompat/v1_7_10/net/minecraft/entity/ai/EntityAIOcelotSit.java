package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.passive.EntityOcelot;
import mccompat.v1_7_10.net.minecraft.world.World;

public class EntityAIOcelotSit extends EntityAIBase {
    protected final EntityOcelot field_151493_a;
    protected final double field_151491_b;

    public EntityAIOcelotSit(EntityOcelot ocelot, double speed) {
        this.field_151493_a = ocelot;
        this.field_151491_b = speed;
        setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }

    protected boolean func_151486_a(World world, int x, int y, int z) {
        return false;
    }
}

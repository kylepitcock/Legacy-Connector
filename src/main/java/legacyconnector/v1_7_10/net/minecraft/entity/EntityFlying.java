package legacyconnector.v1_7_10.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A mob that flies (ghasts, bats); the base class of flying mods' mobs. */
public abstract class EntityFlying extends EntityLiving {
    public EntityFlying(World world) {
        super(world);
    }
}

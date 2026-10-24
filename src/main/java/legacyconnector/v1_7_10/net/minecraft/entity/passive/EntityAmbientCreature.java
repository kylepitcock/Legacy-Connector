package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** An ambient creature (bats, butterflies): harmless and not leashable. */
public abstract class EntityAmbientCreature extends EntityLiving implements IAnimals {
    public EntityAmbientCreature(World world) {
        super(world);
    }
}

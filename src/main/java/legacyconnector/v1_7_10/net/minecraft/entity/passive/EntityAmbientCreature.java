package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** An ambient creature (bats, butterflies): harmless and not leashable. */
public abstract class EntityAmbientCreature extends EntityLiving implements IAnimals {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    protected EntityAmbientCreature(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityAmbientCreature(World world) {
        super(world);
    }
}

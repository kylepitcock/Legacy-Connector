package legacyconnector.vb1_7_3.net.minecraft.entity.mob;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta's pig zombie is a zombie subtype, matching the native zombified-piglin family. */
public class PigZombieEntity extends ZombieEntity {
    public PigZombieEntity(World world){super(world);texture="/mob/pigzombie.png";movementSpeed=.5F;attackDamage=5;fireImmune=true;}
}

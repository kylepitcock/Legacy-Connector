package legacyconnector.vb1_7_3.net.minecraft.entity.mob;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Typed Beta zombie family view; ordinary native zombies retain their native simulation. */
public class ZombieEntity extends MonsterEntity {
    public ZombieEntity(World world){super(world);texture="/mob/zombie.png";movementSpeed=.5F;attackDamage=5;}
}

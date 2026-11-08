package legacyconnector.vb1_7_3.net.minecraft.entity.mob;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Typed native creeper view; legacy callbacks may reset the live fuse timer. */
public class CreeperEntity extends MonsterEntity {
    public int fuseTime;
    public CreeperEntity(World world){super(world);texture="/mob/creeper.png";}
}

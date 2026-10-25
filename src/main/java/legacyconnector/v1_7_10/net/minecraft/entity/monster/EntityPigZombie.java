package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 zombie pigman: a zombie that is immune to fire. */
public class EntityPigZombie extends EntityZombie {
    public EntityPigZombie(World world) {
        super(world);
        this.isImmuneToFire = true;
    }
}

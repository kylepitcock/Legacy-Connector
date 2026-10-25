package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 magma cube: a slime that is immune to fire. */
public class EntityMagmaCube extends EntitySlime {
    public EntityMagmaCube(World world) {
        super(world);
        this.isImmuneToFire = true;
    }
}

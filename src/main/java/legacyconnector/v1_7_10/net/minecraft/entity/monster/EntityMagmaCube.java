package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 magma cube: a slime that is immune to fire. */
public class EntityMagmaCube extends EntitySlime {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityMagmaCube(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityMagmaCube(World world) {
        super(world);
        this.isImmuneToFire = true;
    }
}

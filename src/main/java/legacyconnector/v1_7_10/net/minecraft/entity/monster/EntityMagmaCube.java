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

    /** The body settles slower than a slime's (it keeps 90% of the squash each tick). */
    @Override
    protected void alterSquishAmount() {
        this.squishAmount *= 0.9F;
    }

    /** Magma cubes squish on every landing, whatever their size. */
    @Override
    protected boolean makesSoundOnLand() {
        return true;
    }

    /** Magma cubes hit two harder than a slime of their size. */
    @Override
    protected int getAttackStrength() {
        return super.getAttackStrength() + 2;
    }
}

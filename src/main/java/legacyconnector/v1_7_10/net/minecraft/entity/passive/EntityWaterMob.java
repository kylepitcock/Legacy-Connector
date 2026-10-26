package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A mob that lives in water (squid, mods' fish): breathes water and suffocates on land (2 damage a second once out of air). */
public abstract class EntityWaterMob extends EntityCreature implements IAnimals {
    public EntityWaterMob(World world) {
        super(world);
    }

    /** View of a modern water animal. */
    protected EntityWaterMob(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean getCanSpawnHere() {
        return mcc$world().checkNoEntityCollision(mcc$get$boundingBox(this));
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    @Override
    protected boolean canDespawn() {
        return true;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return 1 + mcc$world().rand.nextInt(3);
    }

    /** Out of water the mob loses air, then takes drowning damage. */
    @Override
    public void onEntityUpdate() {
        int air = getAir();
        super.onEntityUpdate();
        if (!mcc$hosted()) return;
        if (isEntityAlive() && !isInWater()) {
            setAir(--air);
            if (getAir() == -20) {
                setAir(0);
                attackEntityFrom(DamageSource.drown, 2.0F);
            }
        } else {
            setAir(300);
        }
    }
}

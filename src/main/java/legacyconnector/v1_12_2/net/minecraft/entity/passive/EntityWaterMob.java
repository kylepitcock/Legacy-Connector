package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 EntityWaterMob: breathes water and suffocates out of it. */
public abstract class EntityWaterMob extends EntityLiving implements IAnimals {
    public EntityWaterMob(World world) {
        super(world);
    }

    protected EntityWaterMob(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean getCanSpawnHere() {
        return true;
    }

    @Override
    public boolean isNotColliding() {
        if (mcc$entity == null) return true;
        return mcc$entity.level().isUnobstructed(mcc$entity);
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
    protected int getExperiencePoints(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return 1 + rand.nextInt(3);
    }

    @Override
    public void onEntityUpdate() {
        int air = getAir();
        super.onEntityUpdate();
        if (mcc$hosted() && isEntityAlive() && !isInWater()) {
            --air;
            setAir(air);
            if (getAir() == -20) {
                setAir(0);
                attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource.DROWN, 2.0F);
            }
        } else if (mcc$hosted()) {
            setAir(300);
        }
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }
}

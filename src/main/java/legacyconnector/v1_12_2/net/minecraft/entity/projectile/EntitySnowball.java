package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 snowball (hurts blazes; a mod subclass impacts as 1.12). */
public class EntitySnowball extends EntityThrowable {
    public EntitySnowball(World world) {
        super(world);
    }

    public EntitySnowball(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntitySnowball(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntitySnowball(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private boolean mcc$server() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (result.entityHit != null) {
            int damage = result.entityHit instanceof legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityBlaze ? 3 : 0;
            result.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), damage);
        }
        if (mcc$server()) {
            mcc$entity.level().broadcastEntityEvent(mcc$entity, (byte) 3);
            setDead();
        }
    }
}

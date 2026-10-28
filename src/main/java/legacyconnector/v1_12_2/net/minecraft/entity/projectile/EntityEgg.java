package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 thrown egg (one in eight hatches chickens). */
public class EntityEgg extends EntityThrowable {
    public EntityEgg(World world) {
        super(world);
    }

    public EntityEgg(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityEgg(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityEgg(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private boolean mcc$server() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (result.entityHit != null) result.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), 0.0F);
        if (mcc$server()) {
            if (rand.nextInt(8) == 0) {
                int n = rand.nextInt(32) == 0 ? 4 : 1;
                for (int i = 0; i < n; ++i) {
                    var chicken = net.minecraft.world.entity.EntityType.CHICKEN.create(mcc$entity.level());
                    if (chicken == null) continue;
                    chicken.setAge(-24000);
                    chicken.moveTo(mcc$x(), mcc$y(), mcc$z(), mcc$get$rotationYaw(this), 0.0F);
                    mcc$entity.level().addFreshEntity(chicken);
                }
            }
            mcc$entity.level().broadcastEntityEvent(mcc$entity, (byte) 3);
            setDead();
        }
    }
}

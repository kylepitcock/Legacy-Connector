package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 ender pearl (teleports its thrower, sometimes leaving an endermite). */
public class EntityEnderPearl extends EntityThrowable {
    public EntityEnderPearl(World world) {
        super(world);
    }

    public EntityEnderPearl(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityEnderPearl(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityEnderPearl(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private boolean mcc$server() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        EntityLivingBase thrower = getThrower();
        if (result.entityHit != null && result.entityHit == thrower) return;
        if (result.entityHit != null) result.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, thrower), 0.0F);
        if (mcc$server()) {
            if (thrower != null && thrower.mcc$entity != null) {
                var t = thrower.mcc$entity;
                if (t instanceof net.minecraft.server.level.ServerPlayer sp) {
                    if (sp.connection != null && sp.level() == mcc$entity.level() && !sp.isSleeping()) {
                        if (rand.nextFloat() < 0.05F && mcc$entity.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
                            var mite = net.minecraft.world.entity.EntityType.ENDERMITE.create(mcc$entity.level());
                            if (mite != null) {
                                mite.moveTo(t.getX(), t.getY(), t.getZ(), t.getYRot(), t.getXRot());
                                mcc$entity.level().addFreshEntity(mite);
                            }
                        }
                        if (t.isPassenger()) t.stopRiding();
                        sp.teleportTo(mcc$x(), mcc$y(), mcc$z());
                        t.fallDistance = 0.0F;
                        t.hurt(t.damageSources().fall(), 5.0F);
                    }
                } else {
                    t.teleportTo(mcc$x(), mcc$y(), mcc$z());
                    t.fallDistance = 0.0F;
                }
            }
            setDead();
        }
    }
}

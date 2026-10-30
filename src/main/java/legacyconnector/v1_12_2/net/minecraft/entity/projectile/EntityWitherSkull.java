package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 wither skull: withers and blows up what it hits. */
public class EntityWitherSkull extends EntityFireball {
    public EntityWitherSkull(World world) {
        super(world);
        setSize(0.3125F, 0.3125F);
    }

    public EntityWitherSkull(World world, EntityLivingBase shooter, double accelX, double accelY, double accelZ) {
        super(world, shooter, accelX, accelY, accelZ);
        setSize(0.3125F, 0.3125F);
    }

    public EntityWitherSkull(World world, double x, double y, double z, double accelX, double accelY, double accelZ) {
        super(world, x, y, z, accelX, accelY, accelZ);
        setSize(0.3125F, 0.3125F);
    }

    protected EntityWitherSkull(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private boolean mcc$local_invulnerable;

    /** A blue (dangerous) skull: breaks blast-resistant blocks and flies slower (the modern dangerous flag). */
    public boolean isInvulnerable() {
        return mcc$entity instanceof net.minecraft.world.entity.projectile.WitherSkull s ? s.isDangerous() : mcc$local_invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        if (mcc$entity instanceof net.minecraft.world.entity.projectile.WitherSkull s) s.setDangerous(invulnerable);
        else mcc$local_invulnerable = invulnerable;
    }

    private boolean mcc$server() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (!mcc$server()) return;
        EntityLivingBase shooter = mcc$get$shootingEntity(this);
        if (result.entityHit != null) {
            if (shooter != null) {
                if (result.entityHit.attackEntityFrom(DamageSource.causeMobDamage(shooter), 8.0F)) {
                    if (result.entityHit.isEntityAlive()) applyEnchantments(shooter, result.entityHit);
                    else shooter.heal(5.0F);
                }
            } else {
                result.entityHit.attackEntityFrom(DamageSource.MAGIC, 5.0F);
            }
            if (result.entityHit.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
                int seconds = switch (l.level().getDifficulty()) {
                    case NORMAL -> 10;
                    case HARD -> 40;
                    default -> 0;
                };
                if (seconds > 0) l.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 20 * seconds, 1), mcc$entity);
            }
        }
        mcc$entity.level().explode(mcc$entity, mcc$x(), mcc$y(), mcc$z(), 1.0F, false, net.minecraft.world.level.Level.ExplosionInteraction.MOB);
        setDead();
    }
}

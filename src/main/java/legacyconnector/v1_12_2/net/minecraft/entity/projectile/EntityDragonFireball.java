package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 dragon fireball: leaves a cloud of dragon's breath where it lands. */
public class EntityDragonFireball extends EntityFireball {
    public EntityDragonFireball(World world) {
        super(world);
        setSize(1.0F, 1.0F);
    }

    public EntityDragonFireball(World world, EntityLivingBase shooter, double accelX, double accelY, double accelZ) {
        super(world, shooter, accelX, accelY, accelZ);
        setSize(1.0F, 1.0F);
    }

    public EntityDragonFireball(World world, double x, double y, double z, double accelX, double accelY, double accelZ) {
        super(world, x, y, z, accelX, accelY, accelZ);
        setSize(1.0F, 1.0F);
    }

    protected EntityDragonFireball(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        var m = mcc$entity;
        if (m == null || m.level().isClientSide()) return;
        EntityLivingBase shooter = mcc$get$shootingEntity(this);
        if (result.entityHit != null && shooter != null && result.entityHit == shooter) return;
        var cloud = new net.minecraft.world.entity.AreaEffectCloud(m.level(), mcc$x(), mcc$y(), mcc$z());
        if (shooter != null && shooter.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) cloud.setOwner(l);
        cloud.setParticle(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH);
        cloud.setRadius(3.0F);
        cloud.setDuration(600);
        cloud.setRadiusPerTick((7.0F - cloud.getRadius()) / cloud.getDuration());
        cloud.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HARM, 1, 1));
        m.level().levelEvent(2006, m.blockPosition(), m.isSilent() ? -1 : 1);
        m.level().addFreshEntity(cloud);
        setDead();
    }

    @Override
    protected boolean isFireballFiery() {
        return false;
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes getParticleType() {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes.DRAGON_BREATH;
    }
}

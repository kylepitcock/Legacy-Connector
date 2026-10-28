package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 ghast fireball: explodes with its explosion power. */
public class EntityLargeFireball extends EntityFireball {
    public int explosionPower = 1;

    public EntityLargeFireball(World world) {
        super(world);
        setSize(1.0F, 1.0F);
    }

    public EntityLargeFireball(World world, EntityLivingBase shooter, double accelX, double accelY, double accelZ) {
        super(world, shooter, accelX, accelY, accelZ);
        setSize(1.0F, 1.0F);
    }

    public EntityLargeFireball(World world, double x, double y, double z, double accelX, double accelY, double accelZ) {
        super(world, x, y, z, accelX, accelY, accelZ);
        setSize(1.0F, 1.0F);
    }

    protected EntityLargeFireball(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private boolean mcc$server() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (!mcc$server()) return;
        if (result.entityHit != null) {
            result.entityHit.attackEntityFrom(DamageSource.causeFireballDamage(this, mcc$get$shootingEntity(this)), 6.0F);
            applyEnchantments(mcc$get$shootingEntity(this), result.entityHit);
        }
        var level = mcc$entity.level();
        boolean grief = level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        level.explode(mcc$entity, mcc$x(), mcc$y(), mcc$z(), explosionPower, grief, grief ? net.minecraft.world.level.Level.ExplosionInteraction.MOB
                : net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        setDead();
    }
}

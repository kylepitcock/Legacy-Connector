package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 small fireball (blazes, dispensers): sets what it hits on fire. */
public class EntitySmallFireball extends EntityFireball {
    public EntitySmallFireball(World world) {
        super(world);
        setSize(0.3125F, 0.3125F);
    }

    public EntitySmallFireball(World world, EntityLivingBase shooter, double accelX, double accelY, double accelZ) {
        super(world, shooter, accelX, accelY, accelZ);
        setSize(0.3125F, 0.3125F);
    }

    public EntitySmallFireball(World world, double x, double y, double z, double accelX, double accelY, double accelZ) {
        super(world, x, y, z, accelX, accelY, accelZ);
        setSize(0.3125F, 0.3125F);
    }

    protected EntitySmallFireball(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private boolean mcc$server() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (!mcc$server()) return;
        if (result.entityHit != null) {
            if (!result.entityHit.isImmuneToFire()
                    && result.entityHit.attackEntityFrom(DamageSource.causeFireballDamage(this, mcc$get$shootingEntity(this)), 5.0F)) {
                applyEnchantments(mcc$get$shootingEntity(this), result.entityHit);
                result.entityHit.setFire(5);
            }
        } else if (result.getBlockPos() != null && result.sideHit != null) {
            var level = mcc$entity.level();
            var pos = result.getBlockPos().offset(result.sideHit).mcc$modern();
            if (level.isEmptyBlock(pos) && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) {
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.BaseFireBlock.getState(level, pos));
            }
        }
        setDead();
    }
}

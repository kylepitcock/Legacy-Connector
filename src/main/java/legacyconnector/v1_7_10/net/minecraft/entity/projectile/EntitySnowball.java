package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A thrown snowball: knocks back what it hits (3 damage to blazes). A plain one is the modern snowball; mods' subclasses fly here. */
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

    @Override
    protected void onImpact(MovingObjectPosition hit) {
        if (hit.entityHit != null) {
            float damage = hit.entityHit instanceof legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityBlaze ? 3.0F : 0.0F;
            hit.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), damage);
        }
        for (int i = 0; i < 8; i++) {
            mcc$world().spawnParticle("snowballpoof", mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), 0.0D, 0.0D, 0.0D);
        }
        if (!mcc$world().isRemote) setDead();
    }
}

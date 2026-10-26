package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityChicken;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A thrown egg: one in eight hatches a chick (one in 32 of those four). A plain one is the modern egg; mods' subclasses fly here. */
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

    @Override
    protected void onImpact(MovingObjectPosition hit) {
        if (hit.entityHit != null) hit.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), 0.0F);
        World world = mcc$world();
        if (!world.isRemote && rand.nextInt(8) == 0) {
            int chicks = rand.nextInt(32) == 0 ? 4 : 1;
            for (int i = 0; i < chicks; i++) {
                EntityChicken chicken = new EntityChicken(world);
                chicken.setGrowingAge(-24000);
                chicken.setLocationAndAngles(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), mcc$get$rotationYaw(this), 0.0F);
                world.spawnEntityInWorld(chicken);
            }
        }
        for (int i = 0; i < 8; i++) {
            world.spawnParticle("snowballpoof", mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), 0.0D, 0.0D, 0.0D);
        }
        if (!world.isRemote) setDead();
    }
}

package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityThrowable;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A thrown bottle o' enchanting: shatters into 3-11 experience. A plain one is the modern bottle; mods' subclasses fly here. */
public class EntityExpBottle extends EntityThrowable {
    public EntityExpBottle(World world) {
        super(world);
    }

    public EntityExpBottle(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityExpBottle(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    protected float getGravityVelocity() {
        return 0.07F;
    }

    @Override
    protected float func_70182_d() {
        return 0.7F;
    }

    @Override
    protected float func_70183_g() {
        return -20.0F;
    }

    @Override
    protected void onImpact(MovingObjectPosition hit) {
        World world = mcc$world();
        if (world.isRemote) return;
        world.playAuxSFX(2002, (int) Math.round(mcc$get$posX(this)), (int) Math.round(mcc$get$posY(this)), (int) Math.round(mcc$get$posZ(this)), 0);
        int xp = 3 + rand.nextInt(5) + rand.nextInt(5);
        while (xp > 0) {
            int split = EntityXPOrb.getXPSplit(xp);
            xp -= split;
            world.spawnEntityInWorld(new EntityXPOrb(world, mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), split));
        }
        setDead();
    }
}

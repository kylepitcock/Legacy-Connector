package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The ghast fireball: explodes where it lands with {@link #field_92057_e} strength. */
public class EntityLargeFireball extends EntityFireball {
    public int field_92057_e = 1;

    public EntityLargeFireball(World world) {
        super(world);
    }

    public EntityLargeFireball(World world, EntityLivingBase shooter, double ax, double ay, double az) {
        super(world, shooter, ax, ay, az);
    }

    public EntityLargeFireball(World world, double x, double y, double z, double ax, double ay, double az) {
        super(world, x, y, z, ax, ay, az);
    }

    @Override
    protected void onImpact(MovingObjectPosition position) {
        if (!mcc$world().isRemote) {
            mcc$world().createExplosion(this, mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), field_92057_e, true);
            setDead();
        }
    }
}

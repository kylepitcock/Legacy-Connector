package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The wither's skull: a fireball that withers what it hits. */
public class EntityWitherSkull extends EntityFireball {
    public EntityWitherSkull(World world) {
        super(world);
        setSize(0.3125F, 0.3125F);
    }

    public EntityWitherSkull(World world, EntityLivingBase shooter, double ax, double ay, double az) {
        super(world, shooter, ax, ay, az);
        setSize(0.3125F, 0.3125F);
    }

    public boolean isInvulnerable() {
        return false;
    }

    public void setInvulnerable(boolean invulnerable) {
    }

    @Override
    protected void onImpact(MovingObjectPosition position) {
        setDead();
    }
}

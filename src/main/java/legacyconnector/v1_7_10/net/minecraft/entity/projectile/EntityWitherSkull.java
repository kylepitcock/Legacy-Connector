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

    public EntityWitherSkull(World world, double x, double y, double z, double ax, double ay, double az) {
        super(world, x, y, z, ax, ay, az);
        setSize(0.3125F, 0.3125F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(10, (byte) 0);
    }

    /** Whether this is the blue skull (the wither's charged shot, which breaks any block and is stronger). */
    public boolean isInvulnerable() {
        return dataWatcher.getWatchableObjectByte(10) == 1;
    }

    public void setInvulnerable(boolean invulnerable) {
        dataWatcher.updateObject(10, (byte) (invulnerable ? 1 : 0));
    }

    @Override
    protected void onImpact(MovingObjectPosition position) {
        setDead();
    }
}

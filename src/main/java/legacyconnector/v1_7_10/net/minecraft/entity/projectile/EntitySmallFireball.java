package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The small fireball of blazes and fire charges: sets fire to where it lands. */
public class EntitySmallFireball extends EntityFireball {
    public EntitySmallFireball(World world) {
        super(world);
        setSize(0.3125F, 0.3125F);
    }

    public EntitySmallFireball(World world, EntityLivingBase shooter, double ax, double ay, double az) {
        super(world, shooter, ax, ay, az);
        setSize(0.3125F, 0.3125F);
    }

    public EntitySmallFireball(World world, double x, double y, double z, double ax, double ay, double az) {
        super(world, x, y, z, ax, ay, az);
        setSize(0.3125F, 0.3125F);
    }

    @Override
    protected void onImpact(MovingObjectPosition position) {
        if (position != null && position.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int x = position.blockX;
            int y = position.blockY + 1;
            int z = position.blockZ;
            if (mcc$world().isAirBlock(x, y, z)) mcc$world().setBlock(x, y, z, legacyconnector.v1_7_10.net.minecraft.init.Blocks.fire);
        }
        setDead();
    }
}

package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityThrowable;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A thrown ender pearl: teleports the player who threw it to where it lands (5 fall damage, cancellable by EnderTeleportEvent).
 * A plain one is the modern pearl; mods' subclasses fly here.
 */
public class EntityEnderPearl extends EntityThrowable {
    public EntityEnderPearl(World world) {
        super(world);
    }

    public EntityEnderPearl(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityEnderPearl(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    protected void onImpact(MovingObjectPosition hit) {
        EntityLivingBase thrower = getThrower();
        if (hit.entityHit != null) hit.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, thrower), 0.0F);
        World world = mcc$world();
        for (int i = 0; i < 32; i++) {
            world.spawnParticle("portal", mcc$get$posX(this), mcc$get$posY(this) + rand.nextDouble() * 2.0D, mcc$get$posZ(this), rand.nextGaussian(), 0.0D, rand.nextGaussian());
        }
        if (world.isRemote) return;
        if (thrower instanceof EntityPlayerMP player && player.mcc$entity != null && player.mcc$entity.level() == mcc$entity.level()) {
            var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.EnderTeleportEvent(player, mcc$get$posX(this), mcc$get$posY(this),
                    mcc$get$posZ(this), 5.0F);
            if (!legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) {
                if (player.isRiding()) player.mountEntity(null);
                player.setPositionAndUpdate(event.targetX, event.targetY, event.targetZ);
                mcc$set$fallDistance(player, 0.0F);
                player.attackEntityFrom(DamageSource.fall, event.attackDamage);
            }
        }
        setDead();
    }
}

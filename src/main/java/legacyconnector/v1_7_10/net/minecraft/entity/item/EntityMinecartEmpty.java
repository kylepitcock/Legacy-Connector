package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The rideable minecart; constructing this exact class makes a vanilla cart, a mod's subclass is a legacy cart. */
public class EntityMinecartEmpty extends EntityMinecart {
    public EntityMinecartEmpty(World world) {
        super(world);
    }

    public EntityMinecartEmpty(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    /** A player gets in (unless another player sits in it, or a MinecartInteractEvent handler took the click). */
    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartInteractEvent(this, player))) {
            return true;
        }
        Entity rider = mcc$get$riddenByEntity(this);
        if (rider instanceof EntityPlayer && rider != player) return true;
        if (rider != null && rider != player) return false;
        if (!mcc$world().isRemote) player.mountEntity(this);
        return true;
    }

    @Override
    public int getMinecartType() {
        return 0;
    }
}

package legacyconnector.v1_7_10.net.minecraftforge.common;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;

/** Forge's global replacement for minecart collisions (Railcraft's linking and cart pushing), set with EntityMinecart.setCollisionHandler. */
public interface IMinecartCollisionHandler {
    void onEntityCollision(EntityMinecart cart, Entity other);

    AxisAlignedBB getCollisionBox(EntityMinecart cart, Entity other);

    AxisAlignedBB getMinecartCollisionBox(EntityMinecart cart);

    AxisAlignedBB getBoundingBox(EntityMinecart cart);
}

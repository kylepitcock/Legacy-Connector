package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraftforge.common.IMinecartCollisionHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A legacy minecart collision handler installed as NeoForge's (which kept Forge's hook): every cart, vanilla or legacy, is handed to it
 * as a legacy view. A handler that throws is uninstalled, so carts fall back to vanilla pushing instead of failing every tick.
 */
public final class MinecartCollisionBridge implements net.neoforged.neoforge.common.IMinecartCollisionHandler {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Entities");
    private final IMinecartCollisionHandler legacy;

    private MinecartCollisionBridge(IMinecartCollisionHandler legacy) {
        this.legacy = legacy;
    }

    public static void install(IMinecartCollisionHandler legacy) {
        AbstractMinecart.registerCollisionHandler(legacy == null ? null : new MinecartCollisionBridge(legacy));
    }

    @Override
    public void onEntityCollision(AbstractMinecart cart, Entity other) {
        EntityMinecart view = cart(cart);
        if (view == null) return;
        try {
            legacy.onEntityCollision(view, EntityBridge.wrap(other));
        } catch (RuntimeException | LinkageError e) {
            failed(cart, e);
        }
    }

    @Override
    public AABB getCollisionBox(AbstractMinecart cart, Entity other) {
        EntityMinecart view = cart(cart);
        try {
            var box = view == null ? null : legacy.getCollisionBox(view, EntityBridge.wrap(other));
            return box == null ? null : box.mcc$modern();
        } catch (RuntimeException | LinkageError e) {
            failed(cart, e);
            return null;
        }
    }

    @Override
    public AABB getMinecartCollisionBox(AbstractMinecart cart) {
        EntityMinecart view = cart(cart);
        try {
            var box = view == null ? null : legacy.getMinecartCollisionBox(view);
            if (box != null) return box.mcc$modern();
        } catch (RuntimeException | LinkageError e) {
            failed(cart, e);
        }
        return cart.getBoundingBox().inflate(0.2F, 0.0D, 0.2F); // vanilla's box (this one must not be null)
    }

    @Override
    public AABB getBoundingBox(AbstractMinecart cart) {
        EntityMinecart view = cart(cart);
        try {
            var box = view == null ? null : legacy.getBoundingBox(view);
            return box == null ? null : box.mcc$modern();
        } catch (RuntimeException | LinkageError e) {
            failed(cart, e);
            return null;
        }
    }

    private static EntityMinecart cart(AbstractMinecart cart) {
        return EntityBridge.wrap(cart) instanceof EntityMinecart m ? m : null;
    }

    private void failed(AbstractMinecart cart, Throwable e) {
        if (cart.getCollisionHandler() != this) return; // already replaced
        LOG.warn("Legacy minecart collision handler {} failed; using vanilla minecart collisions", legacy.getClass().getName(), e);
        AbstractMinecart.registerCollisionHandler(null);
    }
}

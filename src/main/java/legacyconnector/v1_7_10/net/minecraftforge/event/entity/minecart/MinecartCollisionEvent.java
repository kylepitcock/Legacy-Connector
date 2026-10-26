package legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;

/** A cart collides with another entity (posted before the collision handler or the vanilla push runs). */
public class MinecartCollisionEvent extends MinecartEvent {
    public final Entity collider;

    public MinecartCollisionEvent(EntityMinecart minecart, Entity collider) {
        super(minecart);
        this.collider = collider;
    }
}

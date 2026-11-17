package legacyconnector.v1_12_2.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart;

/** A minecart collides with an entity (posted by the cart's collision handling, not cancelable). */
public class MinecartCollisionEvent extends MinecartEvent {
    private final Entity collider;

    public MinecartCollisionEvent(EntityMinecart minecart, Entity collider) {
        super(minecart);
        this.collider = collider;
    }

    public Entity getCollider() {
        return collider;
    }
}

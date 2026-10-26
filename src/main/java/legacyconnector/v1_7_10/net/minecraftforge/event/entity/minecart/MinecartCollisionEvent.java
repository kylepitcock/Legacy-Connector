package legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;

/** A minecart bumped into an entity. */
public class MinecartCollisionEvent extends MinecartEvent {
    public final Entity collider;

    public MinecartCollisionEvent(EntityMinecart minecart, Entity collider) {
        super(minecart);
        this.collider = collider;
    }
}

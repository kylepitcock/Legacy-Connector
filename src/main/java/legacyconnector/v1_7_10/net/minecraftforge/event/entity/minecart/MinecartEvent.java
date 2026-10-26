package legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;

/** Base of the events about a minecart. */
public class MinecartEvent extends legacyconnector.v1_7_10.net.minecraftforge.event.entity.EntityEvent {
    public final EntityMinecart minecart;

    public MinecartEvent(EntityMinecart minecart) {
        super(minecart);
        this.minecart = minecart;
    }
}

package legacyconnector.v1_12_2.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityEvent;

/** Events about minecarts. */
public class MinecartEvent extends EntityEvent {
    private final EntityMinecart minecart;

    public MinecartEvent(EntityMinecart minecart) {
        super(minecart);
        this.minecart = minecart;
    }

    public EntityMinecart getMinecart() {
        return minecart;
    }
}

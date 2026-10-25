package legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;

/** Base of the events about a minecart. */
public class MinecartEvent extends Event {
    public final EntityMinecart minecart;

    public MinecartEvent(EntityMinecart minecart) {
        this.minecart = minecart;
    }
}

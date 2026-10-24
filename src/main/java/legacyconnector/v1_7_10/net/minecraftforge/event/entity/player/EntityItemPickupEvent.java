package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A player is about to pick an item up; cancelling, or a DENY result, leaves it on the ground. */
@Cancelable
@Event.HasResult
public class EntityItemPickupEvent extends PlayerEvent {
    public final EntityItem item;

    public EntityItemPickupEvent(EntityPlayer player, EntityItem item) {
        super(player);
        this.item = item;
    }
}
